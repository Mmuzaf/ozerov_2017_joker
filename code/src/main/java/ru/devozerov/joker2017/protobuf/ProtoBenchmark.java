package ru.devozerov.joker2017.protobuf;

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
import ru.devozerov.joker2017.protobuf.generated.PersonSizeMessage;
import ru.devozerov.joker2017.protobuf.generated.PersonSpeedMessage;

import java.util.concurrent.ThreadLocalRandom;

@SuppressWarnings("unused")
@State(Scope.Benchmark)
@Warmup(iterations = 10)
@Measurement(iterations = 10)
@Fork(value = 1)
public class ProtoBenchmark {
    /** Cached serialized data. */
    private byte[] data;

    @Setup
    public void setup() {
        data = createSize().toByteArray();
    }

    /*
     * Compare serialize scenario.
     */

    @Benchmark
    public Object serializeSize() throws Exception {
        return createSize().toByteArray();
    }

    @Benchmark
    public Object serializeSpeed() throws Exception {
        return createSpeed().toByteArray();
    }

    /*
     * Compare deserialize scenario.
     */

    @Benchmark
    public Object deserializeSize() throws Exception {
        return PersonSizeMessage.PersonSize.parseFrom(data);
    }

    @Benchmark
    public Object deserializeSpeed() throws Exception {
        return PersonSpeedMessage.PersonSpeed.parseFrom(data);
    }

    /**
     * @return New size-optimized instance.
     */
    private PersonSizeMessage.PersonSize createSize() {
        int val = ThreadLocalRandom.current().nextInt(1000, 10000);

        return PersonSizeMessage.PersonSize.newBuilder().setId(val).setName(String.valueOf(val)).build();
    }

    /**
     * @return New speed-optimized instance.
     */
    private PersonSpeedMessage.PersonSpeed createSpeed() {
        int val = ThreadLocalRandom.current().nextInt(1000, 10000);

        return PersonSpeedMessage.PersonSpeed.newBuilder().setId(val).setName(String.valueOf(val)).build();
    }

    /**
     * Runner.
     */
    public static void main(String[] args) throws Exception {
        Options opt = new OptionsBuilder()
            .include(ProtoBenchmark.class.getSimpleName())
            .build();

        new Runner(opt).run();
    }
}
