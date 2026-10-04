package dev.morphia.benchmarks;

import java.util.concurrent.TimeUnit;

import dev.morphia.config.MorphiaConfig;
import dev.morphia.mapping.Mapper;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

/**
 * The cost of building a mapper and mapping the full benchmark model set.
 * <ul>
 * <li>{@link #coldStart()}: one invocation per fresh JVM, i.e. what an application pays at startup, including class
 * loading and (for critter-runtime) bytecode generation.</li>
 * <li>{@link #warmMapping()}: repeated in a warmed-up JVM, isolating the mapping work from one-time class loading.</li>
 * </ul>
 */
@State(Scope.Benchmark)
public class MappingBenchmark {
    @Param({ "reflection", "critter", "critter-runtime" })
    public String variant;

    private MapperVariant mapperVariant;
    private MorphiaConfig config;

    @Setup(Level.Trial)
    public void setup() {
        mapperVariant = MapperVariant.of(variant);
        config = Fixtures.config(mapperVariant);
    }

    @Benchmark
    @BenchmarkMode(Mode.SingleShotTime)
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    @Warmup(iterations = 0)
    @Measurement(iterations = 1)
    @Fork(10)
    public Mapper coldStart() {
        return Fixtures.mappedMapper(mapperVariant, config);
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    @Warmup(iterations = 5, time = 2)
    @Measurement(iterations = 5, time = 2)
    @Fork(2)
    public Mapper warmMapping() {
        return Fixtures.mappedMapper(mapperVariant, config);
    }
}
