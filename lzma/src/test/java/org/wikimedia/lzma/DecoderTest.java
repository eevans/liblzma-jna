package org.wikimedia.lzma;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.wikimedia.lzma.Decoder.getFlags;

import java.util.EnumSet;

import org.junit.Test;
import org.wikimedia.lzma.Decoder.Flags;

public class DecoderTest {

    @Test
    public void testGetFlags() {
        assertThat(getFlags(EnumSet.of(Flags.TELL_NO_CHECK, Flags.CONCATENATED)), equalTo(1 | 8));
        assertThat(getFlags(EnumSet.of(Flags.NONE, Flags.TELL_NO_CHECK, Flags.CONCATENATED)), equalTo(1 | 8));
    }

}
