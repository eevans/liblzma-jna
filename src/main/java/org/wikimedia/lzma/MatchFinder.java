package org.wikimedia.lzma;

/**
 * Match finders
 * 
 * <p>
 * Match finder has major effect on both speed and compression ratio. Usually hash chains are faster
 * than binary trees.
 * </p>
 * 
 * <p>
 * If you will use LZMA_SYNC_FLUSH often, the hash chains may be a better choice, because binary
 * trees get much higher compression ratio penalty with LZMA_SYNC_FLUSH.
 * </p>
 * 
 * <p>
 * The memory usage formulas are only rough estimates, which are closest to reality when dict_size
 * is a power of two. The formulas are more complex in reality, and can also change a little between
 * liblzma versions. Use lzma_raw_encoder_memusage() to get more accurate estimate of memory usage.
 * </p>
 */
public enum MatchFinder {
    /**
     * Hash Chain with 2- and 3-byte hashing
     * 
     * <p>
     * Minimum nice_len: 3
     * </p>
     * 
     * <p>
     * Memory usage: - dict_size <= 16 MiB: dict_size * 7.5 - dict_size > 16 MiB: dict_size * 5.5 +
     * 64 MiB
     * </p>
     */
    MF_HC3(0x03),
    /**
     * Hash Chain with 2-, 3-, and 4-byte hashing
     * 
     * <p>
     * Minimum nice_len: 4
     * </p>
     * 
     * <p>
     * Memory usage: - dict_size <= 32 MiB: dict_size * 7.5 - dict_size > 32 MiB: dict_size * 6.5
     * </p>
     */
    MF_HC4(0x04),
    /**
     * Binary Tree with 2-byte hashing
     * 
     * <p>
     * Minimum nice_len: 2
     * </p>
     * 
     * <p>
     * Memory usage: dict_size * 9.5
     * </p>
     */
    MF_BT2(0x12),
    /**
     * Binary Tree with 2- and 3-byte hashing
     * 
     * <p>
     * Minimum nice_len: 3
     * </p>
     * 
     * <p>
     * Memory usage: - dict_size <= 16 MiB: dict_size * 11.5 - dict_size > 16 MiB: dict_size * 9.5 +
     * 64 MiB
     * </p>
     */
    MF_BT3(0x13),
    /**
     * Binary Tree with 2-, 3-, and 4-byte hashing
     * 
     * <p>
     * Minimum nice_len: 4
     * </p>
     * 
     * <p>
     * Memory usage: - dict_size <= 32 MiB: dict_size * 11.5 - dict_size > 32 MiB: dict_size * 10.5
     * </p>
     */
    MF_BT4(0x14);

    private final int value;

    private MatchFinder(int value) {
        this.value = value;
    }

    public int getCode() {
        return this.value;
    }
}
