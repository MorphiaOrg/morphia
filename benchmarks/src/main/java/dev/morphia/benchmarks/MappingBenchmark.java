package dev.morphia.benchmarks;

import java.util.concurrent.TimeUnit;

import dev.morphia.benchmarks.models.SimpleEntity;
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
 * <li>{@link #scanModelsPackage()} and {@link #scanEverything()}: startup with entities found by package scanning, the
 * first for the models package only, the second for {@code .*}, the default for {@code morphia.packages}, which scans
 * the whole classpath.</li>
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
        return Fixtures.mappedMapper(mapperVariant, config, Models.MAPPING_TYPES);
    }

    @Benchmark
    @BenchmarkMode(Mode.SingleShotTime)
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    @Warmup(iterations = 0)
    @Measurement(iterations = 1)
    @Fork(5)
    public Mapper scanModelsPackage() {
        Mapper mapper = mapperVariant.newMapper(config);
        mapper.mapPackage(SimpleEntity.class.getPackageName());
        return mapper;
    }

    @Benchmark
    @BenchmarkMode(Mode.SingleShotTime)
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    @Warmup(iterations = 0)
    @Measurement(iterations = 1)
    @Fork(5)
    public Mapper scanEverything() {
        Mapper mapper = mapperVariant.newMapper(config);
        mapper.mapPackage(".*");
        return mapper;
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    @Warmup(iterations = 5, time = 2)
    @Measurement(iterations = 5, time = 2)
    @Fork(2)
    public Mapper warmMapping() {
        return Fixtures.mappedMapper(mapperVariant, config, Models.MAPPING_TYPES);
    }
}
