package ru.devozerov.joker2017.array;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import sun.misc.Unsafe;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.lang.reflect.Field;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;

@SuppressWarnings("unused")
@State(Scope.Benchmark)
@Warmup(iterations = 10)
@Measurement(iterations = 10)
@Fork(value = 1)
public class ArraySerializationBenchmark {
    /** Unsafe instance. */
    private static final Unsafe UNSAFE = unsafe();

    /** */
    private static final long BYTE_ARR_OFF = UNSAFE.arrayBaseOffset(byte[].class);

    /** */
    private static final long INT_ARR_OFF = UNSAFE.arrayBaseOffset(int[].class);

    /** Source array. */
    private int[] data;

    @Setup
    public void setup() {
        data = new int[32];

        for (int i = 0; i < data.length; i++)
            data[i] = i;
    }

    /**
     * Naive approach: create ByteArrayOutputStream, wrap it with DataOutputStream and write elements
     * one by one in portable big-endian format (i.e. byte-by-byte).
     */
    @Benchmark
    public byte[] serializeNormal() throws Exception {
        ByteArrayOutputStream res = new ByteArrayOutputStream(data.length * 4);

        DataOutputStream out = new DataOutputStream(res);

        for (int i = 0; i < data.length; i++)
            out.writeInt(i);

        return res.toByteArray();
    }

    /**
     * Optimized approach: reverse bytes in all elements to mimic "portability" from naive approach, then copy
     * temporary array to output stream in one hop using Unsafe, then shift output stream position manually.
     * Even though we loose time on intermediate array, this approach is order of magnitude faster than "naive"
     * counterpart.
     */
    @Benchmark
    public byte[] serializeOptimized() throws Exception {
        int[] tmp = new int[data.length];

        for (int i = 0; i < data.length; i++)
            tmp[i] = Integer.reverseBytes(data[i]);

        HackedByteArrayOutputStream res = new HackedByteArrayOutputStream(tmp.length * 4);

        res.ensure(tmp.length * 4);

        byte[] buf = res.buffer();

        UNSAFE.copyMemory(tmp, INT_ARR_OFF, buf, BYTE_ARR_OFF, tmp.length * 4);

        res.shiftCount(tmp.length * 4);

        return res.toByteArray();
    }

    /**
     * Runner.
     */
    public static void main(String[] args) throws Exception {
        Options opt = new OptionsBuilder()
            .include(ArraySerializationBenchmark.class.getSimpleName())
            .build();

        new Runner(opt).run();
    }

    /**
     * @return Instance of Unsafe class.
     */
    private static Unsafe unsafe() {
        try {
            return Unsafe.getUnsafe();
        }
        catch (SecurityException ignored) {
            try {
                return AccessController.doPrivileged
                    (new PrivilegedExceptionAction<Unsafe>() {
                        @Override public Unsafe run() throws Exception {
                            Field f = Unsafe.class.getDeclaredField("theUnsafe");

                            f.setAccessible(true);

                            return (Unsafe)f.get(null);
                        }
                    });
            }
            catch (PrivilegedActionException e) {
                throw new RuntimeException("Could not initialize intrinsics.", e.getCause());
            }
        }
    }

    /**
     * BAOS with exposed buffer and ability to shit "count" manually.
     */
    private static class HackedByteArrayOutputStream extends ByteArrayOutputStream {
        /**
         * Constructor.
         *
         * @param size Initial size.
         */
        HackedByteArrayOutputStream(int size) {
            super(size);
        }

        void ensure(int expected) {
            int remaining = buf.length - count;

            if (remaining < expected) {
                int newCap = count + expected;

                byte[] newBuf = new byte[newCap];

                System.arraycopy(buf, 0, newBuf, 0, count);

                buf = newBuf;
            }
        }

        byte[] buffer() {
            return buf;
        }

        void shiftCount(int delta) {
            this.count += delta;
        }
    }
}
