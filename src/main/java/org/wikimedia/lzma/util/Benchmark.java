package org.wikimedia.lzma.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Iterator;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.wikimedia.lzma.Encoder;

import com.google.common.base.Throwables;

public class Benchmark implements AutoCloseable {

    static class Config {
        @Option(name = "-w", aliases = { "--warm-up" }, metaVar = "RUNS", usage = "Warm-up runs to perform")
        int warmUp = 100;
        @Option(name = "-r", aliases = { "--runs" }, metaVar = "RUNS", usage = "Number of runs to perform")
        int runs = 100;
        @Option(name = "-h", aliases = { "--help" }, help = true, usage = "Print (this) usage synopsis")
        boolean help = false;

        public int getWarmUp() {
            return this.warmUp;
        }

        public int getRuns() {
            return this.runs;
        }

        public boolean needsHelp() {
            return this.help;
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

    static class EncodeIterator implements Iterable<Result>, Iterator<Result> {

        private final Benchmark bench;
        private final int runs;
        private int completed = 0;
        private long min, max, sum;
        private boolean isFirst = true;

        EncodeIterator(Benchmark bench, int runs) {
            this.runs = runs;
            this.bench = bench;
        }

        Summary summarize() {
            return new Summary(this.min, this.sum / this.completed, this.max);
        }

        @Override
        public boolean hasNext() {
            return completed < runs;
        }

        @Override
        public Result next() {
            try {
                Result res = this.bench.encode();
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

    private Encoder encoder = new Encoder(1);
    private byte[] input, buffer;

    Benchmark() throws IOException {
        this.input = readInput(System.in);
        this.buffer = new byte[this.input.length];
    }

    void warmUp(int runs) throws IOException {
        for (int i = 0; i < runs; i++) {
            encode();
        }
    }

    EncodeIterator encode(int runs) throws IOException {
        return new EncodeIterator(this, runs);
    }

    Result encode() throws IOException {
        try {
            long start = System.nanoTime();
            this.encoder.setInput(input);
            this.encoder.finish();
            int size = this.encoder.encode(this.buffer, 0, this.buffer.length);
            return new Result(System.nanoTime() - start, size);
        }
        finally {
            this.encoder.reset();
        }
    }

    int getInputSize() {
        return this.input.length;
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

        try (Benchmark bench = new Benchmark()) {
            printf("Uncompressed input size: %d bytes%n", bench.getInputSize());
            printf("Warming up (%d passes)...%n", config.getWarmUp());
            bench.warmUp(config.getWarmUp());
            printf("Compressing...%n");
            int seq = 0;
            EncodeIterator results = bench.encode(config.getRuns());
            for (Result r : results) {
                printf("%d bytes: seq=%d time=%dns%n", r.size, seq++, r.time);
            }
            System.out.println(results.summarize());
        }
        System.exit(0);
    }

}
