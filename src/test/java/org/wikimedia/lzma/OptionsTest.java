package org.wikimedia.lzma;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import org.junit.Test;

public class OptionsTest {

    @Test
    public void test() {
        assertThat("preset 1 should have 1MB dict size", Options.fromPreset(1).dict_size, equalTo(1024 * 1024));
    }

}
