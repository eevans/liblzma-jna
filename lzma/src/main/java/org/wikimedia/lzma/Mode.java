package org.wikimedia.lzma;

/**
 * Selects the mode to use for analyzing data produced by the match finder.
 * 
 * @author Eric Evans
 *
 */
public enum Mode {
    /**
     * Fast mode is usually at its best when combined with a hash chain match finder.
     */
    FAST(1),
    /**
     * This is usually notably slower than fast mode. Use this together with binary tree match
     * finders to expose the full potential of the LZMA1 or LZMA2 encoder.
     */
    NORMAL(2);

    private final int value;

    private Mode(int value) {
        this.value = value;
    }

    public int getCode() {
        return this.value;
    }
}
