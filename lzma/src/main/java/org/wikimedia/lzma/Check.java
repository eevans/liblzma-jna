package org.wikimedia.lzma;

/** analog to lzma_check (see: /usr/include/lzma/check.h) */
public enum Check {
    NONE(0), CRC32(1), CRC64(4), SHA256(10);

    private final int code;

    private Check(int code) {
        this.code = code;
    }

    public int getCode() {
        return this.code;
    }
}