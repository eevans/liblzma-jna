package org.wikimedia.lzma;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.Assert.assertThat;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

import org.junit.Test;

import com.google.common.base.Throwables;

public class EncodeDecodeTest {
    @Test
    public void testReuse() throws IOException {
        Encoder compress = new Encoder();
        Decoder decompress = new Decoder();

        // Test several round-trip compression/decompression on the same encoder/decoder.
        assertRoundTrip(compress, decompress, "foobar.html");
        assertRoundTrip(compress, decompress, "san_antonio.html");
        assertRoundTrip(compress, decompress, "barack_obama.html");

    }

    @Test
    public void testSimple() throws Exception {
        assertRoundTrip(new Encoder(), new Decoder(), "barack_obama.html");
    }

    private void assertMagicBytes(byte[] data) {
        assertThat(data.length, greaterThan(6));
        // The xz magic header is 6 bytes: FD 37 7A 58 5A 00
        assertThat(data[0], equalTo((byte) 0xFD));
        assertThat(data[1], equalTo((byte) 0x37));
        assertThat(data[2], equalTo((byte) 0x7A));
        assertThat(data[3], equalTo((byte) 0x58));
        assertThat(data[4], equalTo((byte) 0x5A));
        assertThat(data[5], equalTo((byte) 0x00));
    }

    private void assertRoundTrip(Encoder compress, Decoder decompress, String resourceName) throws IOException {
        byte[] inData, resData, outBuf = new byte[1 * 1024 * 1024];
        int outSize;

        inData = getTestResourceBytes(resourceName);
        compress.setInput(inData);
        compress.finish();
        outSize = compress.encode(outBuf);
        assertMagicBytes(outBuf);
        decompress.setInput(outBuf, 0, outSize);
        // decompress.finish();
        resData = new byte[inData.length];
        outSize = decompress.decode(resData, 0, resData.length);
        assertThat("round-trip result does not match input", Arrays.copyOf(resData, outSize), equalTo(inData));

        compress.reset();
        decompress.reset();

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
