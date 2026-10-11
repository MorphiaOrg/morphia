package dev.morphia.benchmarks;

import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;

import com.mongodb.client.MongoClient;

import dev.morphia.config.MorphiaConfig;
import dev.morphia.mapping.Mapper;

import org.bson.BsonBinaryReader;
import org.bson.BsonBinaryWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
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
 * Steady-state entity ↔ BSON cost through Morphia's codecs, entirely in memory. Encodes to and decodes from raw BSON
 * bytes, which is what the driver does on the wire, so no server variance leaks into the numbers.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(3)
@State(Scope.Benchmark)
public class CodecBenchmark {
    @Param({ "reflection", "critter", "critter-runtime" })
    public String variant;

    @Param({ "SIMPLE", "NESTED", "COLLECTIONS", "POLYMORPHIC", "LIFECYCLE" })
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
        System.out.printf("[%s] model tiers: %s%n", variant, MapperVariant.describeTiers(mapper));
        client = Fixtures.offlineClient();
        codec = (Codec<Object>) Fixtures.codecRegistry(client, config, mapper).get(model.type());
        entity = model.sample();
        encoded = encode();
        Object decoded = decode();
        if (!model.type().isInstance(decoded)) {
            throw new IllegalStateException("Round trip of %s produced %s".formatted(model, decoded));
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
