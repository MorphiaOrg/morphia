package dev.morphia.benchmarks;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.mongodb.client.MongoClient;

import dev.morphia.Datastore;
import dev.morphia.EntityListener;
import dev.morphia.PostLoadDocumentListener;
import dev.morphia.PostLoadListener;
import dev.morphia.PostPersistDocumentListener;
import dev.morphia.PostPersistListener;
import dev.morphia.PreLoadListener;
import dev.morphia.PrePersistListener;
import dev.morphia.config.MorphiaConfig;
import dev.morphia.mapping.Mapper;

import org.bson.BsonBinaryReader;
import org.bson.BsonBinaryWriter;
import org.bson.Document;
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
 * Entities without lifecycle methods, but with a configured global listener for every event that does nothing, so every
 * entity type is on the lifecycle codecs. With {@link Listener#DOCUMENT} the listener takes the {@code Document}, so each
 * entity is round-tripped through one; with {@link Listener#ENTITY} it doesn't, so the codecs stream. Compare with the
 * same models in {@link CodecBenchmark}.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(2)
@State(Scope.Benchmark)
public class ListenerBenchmark {
    @Param({ "reflection", "critter", "critter-runtime" })
    public String variant;

    @Param({ "SIMPLE", "NESTED" })
    public Models model;

    @Param({ "DOCUMENT", "ENTITY" })
    public Listener listener;

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
        MorphiaConfig config = Fixtures.config(mapperVariant).listeners(List.of(listener.listener));
        Mapper mapper = Fixtures.mappedMapper(mapperVariant, config);
        client = Fixtures.offlineClient();
        codec = (Codec<Object>) Fixtures.codecRegistry(client, config, mapper).get(model.type());
        entity = model.sample();
        encoded = encode();
        if (!model.type().isInstance(decode())) {
            throw new IllegalStateException("Round trip of " + model + " failed");
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

    public enum Listener {
        DOCUMENT(new TakesTheDocument()),
        ENTITY(new TakesTheEntity());

        private final EntityListener<?> listener;

        Listener(EntityListener<?> listener) {
            this.listener = listener;
        }
    }

    /**
     * Handles every event and takes the {@code Document} wherever it can.
     */
    public static class TakesTheDocument implements PrePersistListener<Object>, PostPersistDocumentListener<Object>,
            PreLoadListener<Object>, PostLoadDocumentListener<Object> {
        @Override
        public void prePersist(Object entity, Datastore datastore) {
        }

        @Override
        public void postPersist(Object entity, Document document, Datastore datastore) {
        }

        @Override
        public void preLoad(Object entity, Document document, Datastore datastore) {
        }

        @Override
        public void postLoad(Object entity, Document document, Datastore datastore) {
        }
    }

    /**
     * Handles every event but {@code @PreLoad}, whose listener always takes the {@code Document}, with the entity alone.
     */
    public static class TakesTheEntity implements PrePersistListener<Object>, PostPersistListener<Object>, PostLoadListener<Object> {
        @Override
        public void prePersist(Object entity, Datastore datastore) {
        }

        @Override
        public void postPersist(Object entity, Datastore datastore) {
        }

        @Override
        public void postLoad(Object entity, Datastore datastore) {
        }
    }
}
