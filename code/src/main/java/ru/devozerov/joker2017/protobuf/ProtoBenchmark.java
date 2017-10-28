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

import java.util.Arrays;

@State(Scope.Benchmark)
@Warmup(iterations = 10)
@Measurement(iterations = 10)
@Fork(value = 1)
public class ProtoBenchmark {
    /** Cached size-optimized instance. */
    private PersonSizeMessage.PersonSize size;

    /** Cache speed-optimized instance. */
    private PersonSpeedMessage.PersonSpeed speed;

    /** Cache serialized data. */
    private byte[] data;

    @Setup
    public void setup() {
        size = createSize();
        speed = createSpeed();

        data = size.toByteArray();

        // Make sure that both objects are serialized in the same way.
        if (!Arrays.equals(data, speed.toByteArray()))
            throw new RuntimeException("Size-optimized and speed-optimized arrays are not equal!");
    }

    /*
     * Test instance creation performance. Size-optimized message performs worse due to reflective calls on final
     * build stage. Set a breakpoint inside Person[Size|Speed]Message.Person[Size|Speed].Builder#build() on a line
     * with result.isInitialized() call to see the difference.
     */

    @Benchmark
    public Object buildSize() throws Exception {
        return createSize();
    }

    @Benchmark
    public Object buildSpeed() throws Exception {
        return createSpeed();
    }

    /*
     * Compare build+serialize scenario.
     */

    @Benchmark
    public Object buildAndSerializeSize() throws Exception {
        return createSize().toByteArray();
    }

    @Benchmark
    public Object buildAndSerializeSpeed() throws Exception {
        return createSpeed().toByteArray();
    }

    /*
     * Compare serialize scenario.
     */

    @Benchmark
    public Object serializeSize() throws Exception {
        return size.toByteArray();
    }

    @Benchmark
    public Object serializeSpeed() throws Exception {
        return speed.toByteArray();
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
        return PersonSizeMessage.PersonSize.newBuilder().setId(1234).setName("John Doe").build();
    }

    /**
     * @return New speed-optimized instance.
     */
    private PersonSpeedMessage.PersonSpeed createSpeed() {
        return PersonSpeedMessage.PersonSpeed.newBuilder().setId(1234).setName("John Doe").build();
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
