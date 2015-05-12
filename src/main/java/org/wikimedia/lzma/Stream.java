package org.wikimedia.lzma;

import java.util.Arrays;
import java.util.List;

import com.sun.jna.Memory;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;

/** analog to lzma_stream */
public class Stream extends Structure {
    public Pointer next_in;
    public NativeLong avail_in;
    public long total_in;
    public Pointer next_out;
    public NativeLong avail_out;
    public long total_out;
    public Pointer allocator;
    public Pointer internal;
    public Pointer reserved_ptr1;
    public Pointer reserved_ptr2;
    public Pointer reserved_ptr3;
    public Pointer reserved_ptr4;
    public long reserved_int1;
    public long reserved_int2;
    public NativeLong reserved_int3;
    public NativeLong reserved_int4;
    public int reserved_enum1;
    public int reserved_enum2;

    public Stream(int bufSize) {
        this.next_in = new Memory(bufSize);
        this.next_out = new Memory(bufSize);
        allocateMemory();
    }

    @Override
    protected List<String> getFieldOrder() {
        return Arrays.asList(new String[] {
                "next_in",
                "avail_in",
                "total_in",
                "next_out",
                "avail_out",
                "total_out",
                "allocator",
                "internal",
                "reserved_ptr1",
                "reserved_ptr2",
                "reserved_ptr3",
                "reserved_ptr4",
                "reserved_int1",
                "reserved_int2",
                "reserved_int3",
                "reserved_int4",
                "reserved_enum1",
                "reserved_enum2", });
    }

}