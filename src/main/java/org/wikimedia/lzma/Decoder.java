package org.wikimedia.lzma;

import java.nio.ByteBuffer;
import java.util.EnumSet;

import com.google.common.primitives.UnsignedLong;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;

/**
 * 
 * @see Encoder
 * @author Eric Evans
 *
 */
public class Decoder {

    public static enum Flags {
        NONE(0x00), TELL_NO_CHECK(0x01), TELL_UNSUPPORTED_CHECK(0x02), TELL_ANY_CHECK(0x04), CONCATENATED(0x08);

        private final int value;

        private Flags(int value) {
            this.value = value;
        }

        public int getCode() {
            return this.value;
        }
    }

    private final int bufSize;
    private final Stream stream;
    private final Pointer nextIn;
    private final Pointer nextOut;
    private final UnsignedLong memLimit;
    private final EnumSet<Flags> flags;

    private ByteBuffer input;

    public Decoder(UnsignedLong memLimit, EnumSet<Flags> flags) {
        this(memLimit, flags, Encoder.DEFAULT_BUFFER_SIZE);
    }

    public Decoder(UnsignedLong memLimit, EnumSet<Flags> flags, int bufSize) {
        this.memLimit = memLimit;
        this.flags = flags;
        this.bufSize = bufSize;
        this.stream = new Stream(this.bufSize);
        this.nextIn = this.stream.next_in;
        this.nextOut = this.stream.next_out;
        CLibrary.lzma_stream_decoder(stream, this.memLimit.longValue(), getFlags(this.flags));
    }

    public void setInput(byte[] src, int offset, int len) {
        this.input = this.nextIn.getByteBuffer(0, this.bufSize);
        input.put(src, offset, len);
    }

    public int decode(byte[] dst, int offset, int len) {
        this.input.flip();
        this.stream.avail_in = new NativeLong(this.input.remaining());
        this.stream.avail_out = new NativeLong(this.bufSize);

        // Decode
        int code = CLibrary.lzma_code(stream, Action.FINISH.getCode());
        Return ret = Return.fromCode(code);
        assert ret.equals(Return.STREAM_END); // FIXME: real error handling

        int writeSize = this.bufSize - this.stream.avail_out.intValue();
        ByteBuffer output = this.nextOut.getByteBuffer(0, writeSize);
        output.get(dst, offset, writeSize);

        return writeSize;
    }

    public void end() {
        CLibrary.lzma_end(this.stream);
    }

    public void reset() {
        synchronized (this.stream) {
            this.stream.next_in = this.nextIn;
            this.stream.next_out = this.nextOut;
            CLibrary.lzma_stream_decoder(stream, this.memLimit.longValue(), getFlags(this.flags));
        }
    }

    static int getFlags(EnumSet<Flags> flags) {
        int res = 0;
        for (Flags f : flags) {
            res |= f.getCode();
        }
        return res;
    }

}
