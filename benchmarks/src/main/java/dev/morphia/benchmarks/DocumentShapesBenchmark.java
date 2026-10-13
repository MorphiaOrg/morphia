package dev.morphia.benchmarks;

import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;

import com.mongodb.client.MongoClient;

import dev.morphia.benchmarks.models.SimpleEntity;
import dev.morphia.config.MorphiaConfig;
import dev.morphia.mapping.Mapper;

import org.bson.BsonBinaryReader;
import org.bson.BsonBinaryWriter;
import org.bson.BsonDocument;
import org.bson.BsonString;
import org.bson.codecs.BsonDocumentCodec;
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
 * Decoding {@link SimpleEntity} documents Morphia didn't write itself, which take paths the {@link CodecBenchmark}
 * documents never reach.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(2)
@State(Scope.Benchmark)
public class DocumentShapesBenchmark {
    /**
     * The document variants.
     */
    public enum Shape {
        /**
         * The document as Morphia writes it, for reference.
         */
        AS_WRITTEN,
        /**
         * No {@code _t}: the discriminator pre-scan reads the whole document before decoding it again.
         */
        NO_DISCRIMINATOR,
        /**
         * Numbers stored as strings: each such field fails in its codec, rewinds, and is converted.
         */
        SCHEMA_DRIFT
    }

    @Param({ "reflection", "critter", "critter-runtime" })
    public String variant;

    @Param({ "AS_WRITTEN", "NO_DISCRIMINATOR", "SCHEMA_DRIFT" })
    public Shape shape;

    private MongoClient client;
    private Codec<SimpleEntity> codec;
    private byte[] encoded;
    private final DecoderContext decoderContext = DecoderContext.builder().build();

    @Setup(Level.Trial)
    @SuppressWarnings("unchecked")
    public void setup() {
        MapperVariant mapperVariant = MapperVariant.of(variant);
        MorphiaConfig config = Fixtures.config(mapperVariant);
        Mapper mapper = Fixtures.mappedMapper(mapperVariant, config);
        client = Fixtures.offlineClient();
        codec = (Codec<SimpleEntity>) Fixtures.codecRegistry(client, config, mapper).get(SimpleEntity.class);

        BasicOutputBuffer buffer = new BasicOutputBuffer(512);
        try (BsonBinaryWriter writer = new BsonBinaryWriter(buffer)) {
            codec.encode(writer, SimpleEntity.sample(), EncoderContext.builder().isEncodingCollectibleDocument(true).build());
        }
        BsonDocument document;
        try (BsonBinaryReader reader = new BsonBinaryReader(ByteBuffer.wrap(buffer.toByteArray()))) {
            document = new BsonDocumentCodec().decode(reader, DecoderContext.builder().build());
        }
        switch (shape) {
            case AS_WRITTEN -> {
            }
            case NO_DISCRIMINATOR -> document.remove("_t");
            case SCHEMA_DRIFT -> {
                document.put("age", new BsonString(String.valueOf(document.getInt32("age").getValue())));
                document.put("score", new BsonString(String.valueOf(document.getDouble("score").getValue())));
            }
        }
        encoded = bytes(document);

        SimpleEntity decoded = decode();
        if (decoded.getAge() != SimpleEntity.sample().getAge()) {
            throw new IllegalStateException("%s decoded age %d".formatted(shape, decoded.getAge()));
        }
    }

    @TearDown(Level.Trial)
    public void tearDown() {
        client.close();
    }

    @Benchmark
    public SimpleEntity decode() {
        try (BsonBinaryReader reader = new BsonBinaryReader(ByteBuffer.wrap(encoded))) {
            return codec.decode(reader, decoderContext);
        }
    }

    private static byte[] bytes(BsonDocument document) {
        BasicOutputBuffer buffer = new BasicOutputBuffer(512);
        try (BsonBinaryWriter writer = new BsonBinaryWriter(buffer)) {
            new BsonDocumentCodec().encode(writer, document, EncoderContext.builder().build());
        }
        return buffer.toByteArray();
    }
}
