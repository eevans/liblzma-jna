package org.wikimedia.lzma;

import java.util.EnumSet;
import java.util.Map;

import com.google.common.collect.Maps;

/** lzma_return analog (see: /usr/include/lzma/base.h) */
public enum Return {
    OK(0, "operation completed successfully"),
    STREAM_END(1, "end of stream was reached"),
    NO_CHECK(2, "input stream has no integrity check"),
    UNSUPPORTED_CHECK(3, "cannot calculate the integrity check"),
    GET_CHECK(4, "integrity check type is now available"),
    MEM_ERROR(5, "cannot allocate memory"),
    MEMLIMIT_ERROR(6, "memory usage limit was reached"),
    FORMAT_ERROR(7, "file format not recognized"),
    OPTIONS_ERROR(8, "invalid or unsupported options"),
    DATA_ERROR(9, "data is corrupt"),
    BUF_ERROR(10, "no progress is possible"),
    PROG_ERROR(11, "programming error");

    private static final Map<Integer, Return> index = Maps.newHashMap();

    static {
        for (Return e : EnumSet.allOf(Return.class)) {
            index.put(e.getCode(), e);
        }
    }

    private final int code;
    private final String msg;

    private Return(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public int getCode() {
        return this.code;
    }

    public String getMessage() {
        return this.msg;
    }

    public static Return fromCode(int code) {
        return index.get(code);
    }
}