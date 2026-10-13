package dev.morphia.benchmarks.handwritten;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import com.mongodb.client.MongoClient;

import dev.morphia.benchmarks.Fixtures;
import dev.morphia.benchmarks.MapperVariant;
import dev.morphia.benchmarks.Models;
import dev.morphia.config.MorphiaConfig;
import dev.morphia.mapping.Mapper;

import org.bson.BsonBinaryReader;
import org.bson.BsonBinaryWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.io.BasicOutputBuffer;
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
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;

/**
 * Same shape as {@code CodecBenchmark} (same annotations, buffers, contexts and sample entities) so the numbers line up,
 * but the codec under test is selected by {@code impl}:
 * <ul>
 * <li>{@code morphia}: Morphia's own codec for the given {@code variant}, built exactly as {@code CodecBenchmark} builds it
 * (a same-run baseline);</li>
 * <li>{@code flat}: {@link FlatModelCodec}, the "flat immutable runtime model + one interpretive loop" design (O2/O3);</li>
 * <li>{@code handwritten}: the per-entity generated-codec shape (O1), e.g. {@link SimpleEntityCodec}.</li>
 * </ul>
 * Setup fails unless the codec's encoding is byte-for-byte identical to Morphia's.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(3)
@State(Scope.Benchmark)
public class HandwrittenCodecBenchmark {
    @Param({ "morphia", "flat", "handwritten" })
    public String impl;

    @Param({ "reflection" })
    public String variant;

    @Param({ "SIMPLE", "NESTED" })
    public Models model;

    private MongoClient client;
    private Codec<Object> codec;
    private Object entity;
    private byte[] encoded;
    private final EncoderContext encoderContext = EncoderContext.builder().isEncodingCollectibleDocument(true).build();
    private final DecoderContext decoderContext = DecoderContext.builder().build();

    @Setup(Level.Trial)
    @SuppressWarnings("unchecked")
    public void setup() {
        MapperVariant mapperVariant = MapperVariant.of(variant);
        MorphiaConfig config = Fixtures.config(mapperVariant);
        Mapper mapper = Fixtures.mappedMapper(mapperVariant, config);
        client = Fixtures.offlineClient();
        CodecRegistry registry = Fixtures.codecRegistry(client, config, mapper);
        Codec<Object> morphia = (Codec<Object>) registry.get(model.type());
        codec = switch (impl) {
            case "morphia" -> morphia;
            case "flat" -> Codecs.flat(model, registry, mapper);
            case "handwritten" -> Codecs.handwritten(model, registry, mapper);
            default -> throw new IllegalArgumentException(impl);
        };
        System.out.printf("[%s/%s] model tiers: %s%n", impl, variant, MapperVariant.describeTiers(mapper));
        entity = model.sample();
        byte[] expected = Verify.encode(morphia, entity, encoderContext);
        encoded = encode();
        if (!Arrays.equals(expected, encoded)) {
            throw new IllegalStateException("%s encodes %s differently from Morphia".formatted(impl, model));
        }
        Object decoded = decode();
        if (!model.type().isInstance(decoded) || !Arrays.equals(expected, Verify.encode(morphia, decoded, encoderContext))) {
            throw new IllegalStateException("Round trip of %s through %s produced %s".formatted(model, impl, decoded));
        }
    }

    @TearDown(Level.Trial)
    public void tearDown() {
        client.close();
    }

    @Benchmark
    public byte[] encode() {
        BasicOutputBuffer buffer = new BasicOutputBuffer(512);
        try (BsonBinaryWriter writer = new BsonBinaryWriter(buffer)) {
            codec.encode(writer, entity, encoderContext);
        }
        return buffer.toByteArray();
    }

    @Benchmark
    public Object decode() {
        try (BsonBinaryReader reader = new BsonBinaryReader(ByteBuffer.wrap(encoded))) {
            return codec.decode(reader, decoderContext);
        }
    }
}
