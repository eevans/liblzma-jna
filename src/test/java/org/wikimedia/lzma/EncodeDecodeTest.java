package org.wikimedia.lzma;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.EnumSet;

import org.junit.Test;
import org.wikimedia.lzma.Decoder.Flags;

import com.google.common.base.Throwables;
import com.google.common.primitives.UnsignedLong;

public class EncodeDecodeTest {
    @Test
    public void testMulti() {

    }

    @Test
    public void testSimple() throws Exception {

        byte[] data = getTestResourceBytes("foobar.html");

        int preset = 1;
        byte[] output = new byte[data.length];
        int outputSize;

        // Encode ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        Encoder compress = new Encoder(preset, Check.CRC64);
        compress.setInput(data);
        compress.finish();
        outputSize = compress.encode(output);

        // The xz magic header is 6 bytes: FD 37 7A 58 5A 00
        assertThat(output[0], equalTo((byte) 0xFD));
        assertThat(output[1], equalTo((byte) 0x37));
        assertThat(output[2], equalTo((byte) 0x7A));
        assertThat(output[3], equalTo((byte) 0x58));
        assertThat(output[4], equalTo((byte) 0x5A));
        assertThat(output[5], equalTo((byte) 0x00));

        // Decode ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        EnumSet<Flags> decoderFlags = EnumSet.of(Flags.CONCATENATED);
        UnsignedLong memLimit = UnsignedLong.fromLongBits(Long.MAX_VALUE);
        Decoder decompress = new Decoder(memLimit, decoderFlags);
        decompress.setInput(output, 0, outputSize);
        byte[] result = new byte[data.length];
        int resultSize = decompress.decode(result, 0, result.length);
        assertThat(Arrays.copyOf(result, resultSize), equalTo(data));

    }

    private byte[] getTestResourceBytes(String name) throws IOException {
        URL url = getClass().getResource(String.format("/%s", name));
        if (url == null) {
            throw new FileNotFoundException();
        }

        URI uri;
        try {
            uri = url.toURI();
        }
        catch (URISyntaxException e) {
            throw Throwables.propagate(e);
        }

        return Files.readAllBytes(Paths.get(uri));
    }
}
