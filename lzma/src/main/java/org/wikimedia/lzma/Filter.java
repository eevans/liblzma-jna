package org.wikimedia.lzma;

import java.util.Arrays;
import java.util.List;

import com.sun.jna.Pointer;
import com.sun.jna.Structure;

public class Filter extends Structure {
    public static class IDs {
        public static long FILTER_X86    = 0x0000000000000004L;
        public static long FILTER_LZMA2  = 0x0000000000000021L;
        public static long VLI_UNKNOWN   = 0xffffffffffffffffL;  // UINT64_MAX
    }

    public long id;
    public Pointer options;

    @Override
    protected List<String> getFieldOrder() {
        return Arrays.asList(new String[] { "id", "options" });
    }

}
