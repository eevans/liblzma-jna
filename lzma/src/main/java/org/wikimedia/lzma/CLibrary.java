package org.wikimedia.lzma;

import com.sun.jna.LastErrorException;
import com.sun.jna.Native;

public class CLibrary {

    static {
        Native.register("lzma");
        //Native.setProtected(true);
    }

    public static native int lzma_stream_decoder(Stream strm, long memlimit, int flags) throws LastErrorException;

    public static native void lzma_end(Stream strm) throws LastErrorException;

    public static native int lzma_code(Stream strm, int action) throws LastErrorException;

    public static native int lzma_easy_encoder(Stream strm, int preset, int check) throws LastErrorException;

    public static native int lzma_stream_encoder(Stream strm, Filter filters, int check) throws LastErrorException;

    public static native boolean lzma_lzma_preset(Options options, int preset) throws LastErrorException;

}
