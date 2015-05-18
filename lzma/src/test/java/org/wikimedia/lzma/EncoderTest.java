package org.wikimedia.lzma;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class EncoderTest {
    private Encoder encoder;

    @Before
    public void setUp() {
        this.encoder = new Encoder(1, Check.NONE);
    }

    @After
    public void tearDown() {
        this.encoder.end();
    }

    @Test(expected = IllegalStateException.class)
    public void testEnd() {
        this.encoder.end();
        setInput();
    }

    @Test
    public void testNeedsInput() {
        assertThat(encoder.needsInput(), is(true));
        setInput();
        assertThat(encoder.needsInput(), is(false));
    }

    private void setInput() {
        this.encoder.setInput(new byte[] { 0 });
    }
}
