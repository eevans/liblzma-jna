package org.wikimedia.lzma.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Iterator;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.wikimedia.lzma.Decoder;
import org.wikimedia.lzma.Encoder;

import com.google.common.base.Throwables;

public class Benchmark implements AutoCloseable {

    static class Config {
        @Option(name = "-e", aliases = { "--encode" }, usage = "Get encode timings")
        boolean encode = false;
        @Option(name = "-d", aliases = { "--decode" }, usage = "Get decode timings")
        boolean decode = false;
        @Option(name = "-w", aliases = { "--warm-up" }, metaVar = "RUNS", usage = "Warm-up runs to perform")
        int warmUp = 100;
        @Option(name = "-r", aliases = { "--runs" }, metaVar = "RUNS", usage = "Number of runs to perform")
        int runs = 100;
        @Option(name = "-h", aliases = { "--help" }, help = true, usage = "Print (this) usage synopsis")
        boolean help = false;

        int getWarmUp() {
            return this.warmUp;
        }

        int getRuns() {
            return this.runs;
        }

        boolean needsHelp() {
            return this.help;
        }

        boolean doEncode() {
            return this.encode;
        }

        boolean doDecode() {
            return this.decode;
        }
    }

    static class Result {
        private long time;
        private int size;

        Result(long time, int size) {
            this.time = time;
            this.size = size;
        }
    }

    static class Summary {
        private long min;
        private long avg;
        private long max;

        Summary(long min, long avg, long max) {
            this.min = min;
            this.avg = avg;
            this.max = max;
        }

        private double minMs() {
            return millis(this.min);
        }

        private double avgMs() {
            return millis(this.avg);
        }

        private double maxMs() {
            return millis(this.max);
        }

        public String toString() {
            return String.format("min/avg/max = %.4fms/%.4fms/%.4fms", minMs(), avgMs(), maxMs());
        }

        private static double millis(long nanos) {
            return nanos / 1e6;
        }

    }

    static abstract class AbstractResultIterator implements Iterable<Result>, Iterator<Result> {

        protected final Benchmark bench;
        private final int runs;
        private int completed = 0;
        private long min, max, sum;
        private boolean isFirst = true;

        AbstractResultIterator(Benchmark bench, int runs) {
            this.runs = runs;
            this.bench = bench;
        }

        Summary summarize() {
            return new Summary(this.min, this.sum / this.completed, this.max);
        }

        abstract Result getResult() throws IOException;

        @Override
        public boolean hasNext() {
            return completed < runs;
        }

        @Override
        public Result next() {
            try {
                Result res = getResult();
                if (this.isFirst) {
                    this.min = this.max = this.sum = res.time;
                    this.isFirst = false;
                }
                else {
                    if (res.time < this.min) {
                        this.min = res.time;
                    }
                    if (res.time > this.max) {
                        this.max = res.time;
                    }
                    this.sum += res.time;
                }
                completed++;
                return res;
            }
            catch (IOException e) {
                throw Throwables.propagate(e);
            }
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Iterator<Result> iterator() {
            return this;
        }

    }

    static class EncodeIterator extends AbstractResultIterator {
        EncodeIterator(Benchmark bench, int runs) {
            super(bench, runs);
        }

        @Override
        Result getResult() throws IOException {
            return this.bench.encode();
        }

    }

    static class DecodeIterator extends AbstractResultIterator {
        DecodeIterator(Benchmark bench, int runs) {
            super(bench, runs);
        }

        @Override
        Result getResult() throws IOException {
            return this.bench.decode();
        }

    }

    private Encoder encoder = new Encoder(1);
    private Decoder decoder = new Decoder();
    private byte[] inputBuffer, outputBuffer, compressedBuffer;
    private int compressedSize;

    Benchmark() throws IOException {
        this.inputBuffer = readInput(System.in);
        this.outputBuffer = new byte[this.inputBuffer.length];
        this.compressedBuffer = new byte[this.inputBuffer.length];
    }

    void warmUpDecoder(int runs) throws IOException {
        for (int i = 0; i < runs; i++) {
            encode();
        }
    }

    void warmUpEncoder(int runs) throws IOException {
        for (int i = 0; i < runs; i++) {
            encode();
        }
    }

    EncodeIterator encode(int runs) throws IOException {
        return new EncodeIterator(this, runs);
    }

    DecodeIterator decode(int runs) throws IOException {
        return new DecodeIterator(this, runs);
    }

    Result encode() throws IOException {
        try {
            long start = System.nanoTime();
            this.encoder.setInput(inputBuffer);
            this.encoder.finish();
            this.compressedSize = this.encoder.encode(this.compressedBuffer, 0, this.compressedBuffer.length);
            return new Result(System.nanoTime() - start, this.compressedSize);
        }
        finally {
            this.encoder.reset();
        }
    }

    Result decode() {
        try {
            long start = System.nanoTime();
            this.decoder.setInput(this.compressedBuffer, 0, this.compressedSize);
            int size = this.decoder.decode(this.outputBuffer, 0, this.outputBuffer.length);
            return new Result(System.nanoTime() - start, size);
        }
        finally {
            this.decoder.reset();
        }
    }

    int getInputSize() {
        return this.inputBuffer.length;
    }

    public void close() {
        this.encoder.end();
    }

    static byte[] readInput(InputStream input) throws IOException {
        byte[] b = new byte[1 * 1024 * 1024];
        int size, total = 0;
        while ((size = System.in.read(b, 0, b.length)) != -1) {
            b = Arrays.copyOf(b, (int) (b.length * 1.75));
            total += size;
        }
        return Arrays.copyOf(b, total);
    }

    static void printf(String format, Object... args) {
        System.out.printf(format, args);
    }

    static void printUsage(OutputStream out, CmdLineParser parser) {
        System.err.printf("java Benchmark [options...]%n%n");
        parser.printUsage(System.err);
        System.err.println();
    }

    public static void main(String... args) throws IOException {
        Config config = new Config();
        CmdLineParser parser = new CmdLineParser(config);

        try {
            parser.parseArgument(args);
        }
        catch (CmdLineException e) {
            System.err.println(e.getMessage());
            printUsage(System.err, parser);
            System.exit(1);
        }

        if (config.needsHelp()) {
            printUsage(System.out, parser);
            System.exit(0);
        }

        if (!(config.doDecode() || config.doEncode())) {
            System.err.println("You must supply at least one of -e/--encode or -d/--decode");
            System.exit(1);
        }

        try (Benchmark bench = new Benchmark()) {
            printf("Uncompressed input size: %d bytes%n", bench.getInputSize());
            if (config.doEncode()) {
                printf("Warming up encoder (%d passes)...%n", config.getWarmUp());
                bench.warmUpEncoder(config.getWarmUp());
                printf("Compressing...%n");
                int seq = 0;
                EncodeIterator results = bench.encode(config.getRuns());
                for (Result r : results) {
                    printf("%d bytes: seq=%d time=%dns%n", r.size, seq++, r.time);
                }
                System.out.println(results.summarize());
            }
            if (config.doDecode()) {
                printf("Warming up decoder (%d passes)...%n", config.getWarmUp());
                bench.warmUpDecoder(config.getWarmUp());
                printf("Decompressing...%n");
                int seq = 0;
                DecodeIterator results = bench.decode(config.getRuns());
                for (Result r : results) {
                    printf("%d bytes: seq=%d time=%dns%n", r.size, seq++, r.time);
                }
                System.out.println(results.summarize());
            }
        }
        System.exit(0);
    }

}
