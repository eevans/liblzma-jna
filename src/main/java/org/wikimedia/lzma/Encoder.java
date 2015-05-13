package org.wikimedia.lzma;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;

import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;

/**
 * Usage:
 * 
 * <blockquote>
 * 
 * <pre>
 * byte[] input = &quot;hello world&quot;.getBytes(&quot;UTF-8&quot;);
 * byte[] output = new byte[512];
 * 
 * // Compress
 * try (Encoder encoder = new Encoder()) {
 *     encoder.setInput(input);
 *     encoder.finish();
 *     int encodedLength = encoder.encode(output);
 * }
 * 
 * // Decompress
 * try (Decoder decoder = new Decoder()) {
 *     decoder.setInput(output, 0, encodedLength);
 *     byte[] result = new byte[512];
 *     int resultLength = decoder.decode(result);
 * }
 * </pre>
 * 
 * </blockquote>
 * 
 * @see Decoder
 * @author Eric Evans
 *
 */
public class Encoder {
    public static enum Flush {
        NONE, SYNC, FULL;

        Action toAction() {
            switch (this) {
            case NONE:
                return Action.RUN;
            case SYNC:
                return Action.SYNC_FLUSH;
            case FULL:
                return Action.FULL_FLUSH;
            default:
                throw new RuntimeException("a bug!");
            }
        }
    }

    /** Default internal buffer size */
    public static final int DEFAULT_BUFFER_SIZE = 5 * 1024 * 1024;

    private final int bufSize;
    private final Stream stream;
    private final Options options;
    private final Check check;

    private final Pointer nextIn;
    private final Pointer nextOut;
    private ByteBuffer input;
    private long bytesRead;
    private long bytesWritten;
    private boolean finish = false;
    private boolean finished = false;
    private boolean initialized = true;

    public Encoder() {
        this(6);
    }

    public Encoder(int preset) {
        this(preset, Check.NONE);
    }

    public Encoder(int preset, Check check) {
        this(Options.fromPreset(preset), check);
    }

    public Encoder(Options options, Check check) {
        this(options, check, DEFAULT_BUFFER_SIZE);
    }

    public Encoder(Options options, Check check, int internalBufferSize) {
        this.bufSize = internalBufferSize;
        this.options = options;
        this.check = check;
        this.stream = new Stream(this.bufSize);
        this.nextIn = this.stream.next_in;
        this.nextOut = this.stream.next_out;
        this.input = null;
        CLibrary.lzma_stream_encoder(this.stream, filterChain(this.options), this.check.getCode());
    }

    public void setInput(byte[] src) {
        setInput(src, 0, src.length);
    }

    public void setInput(byte[] src, int offset, int len) {
        checkNotNull(src, "src argument");
        if (offset < 0 || len < 0 || offset > (src.length - len)) {
            throw new ArrayIndexOutOfBoundsException();
        }
        if (len > this.bufSize) {
            throw new BufferOverflowException();
        }
        synchronized (this.stream) {
            ensureReady();
            this.input = this.nextIn.getByteBuffer(0, this.bufSize);
            this.input.put(src, offset, len);
            this.input.flip();
        }
    }

    /**
     * 
     * @return true if the input buffer is empty, and {@link Encoder#setInput(byte[], int, int)}
     *         should be called to add more input.
     */
    public boolean needsInput() {
        synchronized (this.stream) {
            return this.input == null || this.input.remaining() <= 0;
        }
    }

    public void finish() {
        synchronized (this.stream) {
            this.finish = true;
        }
    }

    public int encode(byte[] dst) throws IOException {
        return encode(dst, 0, dst.length);
    }

    public int encode(byte[] dst, int offset, int len) throws IOException {
        return encode(dst, offset, len, Flush.NONE);
    }

    /**
     * Compresses the input data, filling the supplied buffer with the results. Returns the actual
     * number of bytes written to the buffer.
     *
     * <p>
     * Note: This method is incomplete, and as a result:
     * <ol>
     * <li>The flush mode is currently ignored</li>
     * <li>{@link Encoder#finish()} must be invoked before calling {@link Encoder#encode(...)}</li>
     * <li>All of the data corresponding to the input will be flushed to {@code dst} at once.</li>
     * <li>The encoder must be reset in between invocations</li>
     * </ol>
     * </p>
     *
     * @param dst
     *            the buffer to write compressed data to
     * @param offset
     *            start offset of the data
     * @param len
     *            the maximum number of compressed bytes to write to {@code dst}
     * @param flush
     *            the compression flush mode
     * @return the number of compressed bytes written to {@code dst}
     * @throws IOException
     */
    public int encode(byte[] dst, int offset, int len, Flush flush) throws IOException {
        checkNotNull(dst, "dst argument");
        if (offset < 0 || len < 0 || offset > (dst.length - len)) {
            throw new ArrayIndexOutOfBoundsException();
        }
        if (len > this.bufSize) {
            throw new BufferOverflowException();
        }

        Action action = this.finish ? Action.FINISH : flush.toAction();

        // TODO: implement sync modes
        if (!action.equals(Action.FINISH)) {
            throw new UnsupportedOperationException("you must invoke finish() prior to calling encode()");
        }

        synchronized (this.stream) {
            ensureReady();
            int availIn = this.input.remaining();
            this.stream.avail_in = new NativeLong(availIn);
            this.stream.avail_out = new NativeLong(this.bufSize);

            // Encode
            Return ret = Return.fromCode(CLibrary.lzma_code(stream, action.getCode()));
            int writeSize = this.bufSize - this.stream.avail_out.intValue();

            // TODO: implement sync modes
            if (len < writeSize) {
                throw new ArrayIndexOutOfBoundsException(String.format(
                        "insufficient space to write compressed data (given %d bytes, needed %d)",
                        len,
                        writeSize));
            }

            // XXX: Since (for the time being) we're expecting to compress all of our input in one
            // shot, there must have been sufficient room in the buffer to accommodate all output.
            if (stream.avail_out.intValue() == 0) {
                throw new BufferOverflowException();
            }

            this.nextOut.getByteBuffer(0, this.bufSize).get(dst, offset, writeSize);

            switch (ret) {
            case OK:
                throw new IllegalStateException("failed to reach end of stream");
            case STREAM_END:
                this.finished = true;
                break;
            default:
                throw new IOException(ret.getMessage());
            }

            this.bytesRead += availIn - this.stream.avail_in.intValue();
            this.bytesWritten += writeSize;

            return writeSize;
        }
    }

    /** Readies the encoder for a new set of input data. */
    public void reset() {
        synchronized (this.stream) {
            this.finished = false;
            this.stream.next_in = this.nextIn;
            this.stream.next_out = this.nextOut;
            CLibrary.lzma_stream_encoder(this.stream, filterChain(this.options), this.check.getCode());
            this.input = null;
            this.stream.avail_in = new NativeLong(0);
            this.stream.avail_out = new NativeLong(this.bufSize);
            this.bytesRead = 0;
            this.bytesWritten = 0;
        }
    }

    public void end() {
        synchronized (this.stream) {
            this.initialized = false;
            CLibrary.lzma_end(this.stream);
        }
    }

    public boolean finished() {
        synchronized (this.stream) {
            return this.finished;
        }
    }

    public long getBytesRead() {
        synchronized (this.stream) {
            return this.bytesRead;
        }
    }

    public long getBytesWritten() {
        synchronized (this.stream) {
            return this.bytesWritten;
        }
    }

    protected void finalize() {
        end();
    }

    private void ensureReady() {
        synchronized (this.stream) {
            if (!this.initialized) {
                throw new IllegalStateException("encoder has been deinitialized (instantiate a new one)");
            }
        }
    }

    private static Filter filterChain(Options options) {
        Filter filter = new Filter();
        filter.id = Filter.IDs.FILTER_X86;
        filter.options = null;
        Filter[] filters = (Filter[]) filter.toArray(3);
        filters[1].id = Filter.IDs.FILTER_LZMA2;
        filters[1].options = options.getPointer();
        filters[2].id = Filter.IDs.VLI_UNKNOWN;
        filters[2].options = null;
        return filter;
    }
}
