package dev.morphia.benchmarks;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.mongodb.client.MongoClient;

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
import org.openjdk.jmh.infra.Blackhole;

/**
 * Every {@link CodecBenchmark} model in one benchmark, so the encoder and decoder call sites shared across entity types
 * see all of them, as in an application. {@code CodecBenchmark} runs each model in its own JVM, where those call sites
 * see only one type and the JIT can specialize them. Compare one operation here with the sum of the per-model results.
 * {@link Models#ARRAYS} is left out: its embedding vector costs more than all the others together and would hide the
 * effect being measured.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(2)
@State(Scope.Benchmark)
public class MixedCodecBenchmark {
    @Param({ "reflection", "critter", "critter-runtime" })
    public String variant;

    private MongoClient client;
    private List<Codec<Object>> codecs;
    private List<Object> entities;
    private List<byte[]> encoded;
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
        List<Models> models = java.util.Arrays.stream(Models.values()).filter(model -> model != Models.ARRAYS).toList();
        codecs = models.stream().map(model -> (Codec<Object>) registry.get(model.type())).toList();
        entities = models.stream().map(Models::sample).toList();
        encoded = new java.util.ArrayList<>();
        for (int i = 0; i < codecs.size(); i++) {
            encoded.add(encode(codecs.get(i), entities.get(i)));
        }
    }

    @TearDown(Level.Trial)
    public void tearDown() {
        client.close();
    }

    /**
     * Encodes one of each model.
     */
    @Benchmark
    public void encode(Blackhole blackhole) {
        for (int i = 0; i < codecs.size(); i++) {
            blackhole.consume(encode(codecs.get(i), entities.get(i)));
        }
    }

    /**
     * Decodes one of each model.
     */
    @Benchmark
    public void decode(Blackhole blackhole) {
        for (int i = 0; i < codecs.size(); i++) {
            try (BsonBinaryReader reader = new BsonBinaryReader(ByteBuffer.wrap(encoded.get(i)))) {
                blackhole.consume(codecs.get(i).decode(reader, decoderContext));
            }
        }
    }

    private byte[] encode(Codec<Object> codec, Object entity) {
        BasicOutputBuffer buffer = new BasicOutputBuffer(512);
        try (BsonBinaryWriter writer = new BsonBinaryWriter(buffer)) {
            codec.encode(writer, entity, encoderContext);
        }
        return buffer.toByteArray();
    }
}
