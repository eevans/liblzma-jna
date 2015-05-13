package org.wikimedia.lzma;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.Arrays;
import java.util.List;

import com.sun.jna.Pointer;
import com.sun.jna.Structure;

public class Options extends Structure {
    public int dict_size;
    public Pointer preset_dict;
    public int preset_dict_size;
    public int lc;
    public int lp;
    public int pb;
    public int mode; // enum
    public int nice_len;
    public int mf; // enum
    public int depth;
    public int reserved_int1;
    public int reserved_int2;
    public int reserved_int3;
    public int reserved_int4;
    public int reserved_int5;
    public int reserved_int6;
    public int reserved_int7;
    public int reserved_int8;
    public int reserved_enum1;
    public int reserved_enum2;
    public int reserved_enum3;
    public int reserved_enum4;
    public Pointer reserved_ptr1;
    public Pointer reserved_ptr2;

    @Override
    protected List<String> getFieldOrder() {
        return Arrays.asList(new String[] {
                "dict_size",
                "preset_dict",
                "preset_dict_size",
                "lc",
                "lp",
                "pb",
                "mode",
                "nice_len",
                "mf",
                "depth",
                "reserved_int1",
                "reserved_int2",
                "reserved_int3",
                "reserved_int4",
                "reserved_int5",
                "reserved_int6",
                "reserved_int7",
                "reserved_int8",
                "reserved_enum1",
                "reserved_enum2",
                "reserved_enum3",
                "reserved_enum4",
                "reserved_ptr1",
                "reserved_ptr2" });
    }

    public static Options fromPreset(int preset) {
        checkArgument(preset >=0 && preset <= 9, "preset must be between 0-9");
        Options options = new Options();
        CLibrary.lzma_lzma_preset(options, preset);
        return options;
    }

}
