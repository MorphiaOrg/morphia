package dev.morphia.mapping.codec.pojo;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;

import dev.morphia.EntityListener;
import dev.morphia.MorphiaDatastore;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.PostLoad;
import dev.morphia.annotations.PostPersist;
import dev.morphia.annotations.PreLoad;
import dev.morphia.annotations.PrePersist;
import dev.morphia.config.ManualMorphiaConfig;
import dev.morphia.mapping.MapperType;

import org.bson.BsonBinaryReader;
import org.bson.BsonBinaryWriter;
import org.bson.BsonDocument;
import org.bson.Document;
import org.bson.RawBsonDocument;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.io.BasicOutputBuffer;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Lifecycle callbacks get a {@code Document} only when one of them takes it; otherwise the entity is encoded and decoded
 * directly. A global listener takes part only in the events it claims.
 */
public class TestLifecycleStreaming {
    private static final EncoderContext COLLECTIBLE = EncoderContext.builder().isEncodingCollectibleDocument(true).build();
    private static final DecoderContext DECODER = DecoderContext.builder().build();

    @ParameterizedTest
    @EnumSource(MapperType.class)
    void callbacksWithoutADocumentStream(MapperType mapperType) {
        try (MongoClient client = MongoClients.create("mongodb://localhost:1")) {
            MorphiaCodec<Streamed> codec = codec(datastore(client, mapperType), Streamed.class);
            Streamed entity = new Streamed();
            entity.name = "streamed";

            BsonDocument encoded = encode(codec, entity);
            Assertions.assertEquals(List.of("prePersist", "postPersist"), entity.events);
            Assertions.assertEquals("streamed", encoded.getString("name").getValue());
            Assertions.assertEquals(1, encoded.getInt32("version").getValue(), "@PrePersist ran before encoding");

            Streamed decoded = decode(codec, encoded);
            // @PreLoad sees the instance before its properties are read, @PostLoad after
            Assertions.assertEquals(List.of("preLoad:null", "postLoad:streamed"), decoded.events);
        }
    }

    @ParameterizedTest
    @EnumSource(MapperType.class)
    void callbacksTakingADocumentGetOne(MapperType mapperType) {
        try (MongoClient client = MongoClients.create("mongodb://localhost:1")) {
            MorphiaCodec<Documented> codec = codec(datastore(client, mapperType), Documented.class);
            Documented entity = new Documented();
            entity.name = "documented";

            BsonDocument encoded = encode(codec, entity);
            Assertions.assertEquals("added", encoded.getString("extra").getValue(), "@PrePersist can add to the document");
            Assertions.assertEquals("documented", entity.persisted);

            Documented decoded = decode(codec, encoded);
            Assertions.assertEquals("added", decoded.loaded);
        }
    }

    @ParameterizedTest
    @EnumSource(MapperType.class)
    @SuppressWarnings({ "deprecation", "removal" })
    void globalListenersRunOnlyForTheEventsTheyClaim(MapperType mapperType) {
        try (MongoClient client = MongoClients.create("mongodb://localhost:1")) {
            MorphiaDatastore datastore = datastore(client, mapperType);
            Recording persisting = new Recording(PostPersist.class, false);
            Recording loading = new Recording(PostLoad.class, true);
            datastore.getMapper().addInterceptor(persisting);
            datastore.getMapper().addInterceptor(loading);
            MorphiaCodec<Plain> codec = codec(datastore, Plain.class);

            Assertions.assertInstanceOf(LifecycleEncoder.class, codec.getEncoder());
            Assertions.assertInstanceOf(LifecycleDecoder.class, codec.getDecoder());

            Plain entity = new Plain();
            entity.name = "plain";
            BsonDocument encoded = encode(codec, entity);
            decode(codec, encoded);

            // each runs for its own event only. the one that doesn't use the document gets none: through a Document, @PostPersist
            // would see the encoded name
            Assertions.assertEquals(List.of("PostPersist:null"), persisting.calls);
            Assertions.assertEquals(List.of("PostLoad:plain"), loading.calls);
        }
    }

    @ParameterizedTest
    @EnumSource(MapperType.class)
    @SuppressWarnings({ "deprecation", "removal" })
    void aListenerForPersistingLeavesDecodingAlone(MapperType mapperType) {
        try (MongoClient client = MongoClients.create("mongodb://localhost:1")) {
            MorphiaDatastore datastore = datastore(client, mapperType);
            datastore.getMapper().addInterceptor(new Recording(PrePersist.class, false));
            MorphiaCodec<Plain> codec = codec(datastore, Plain.class);

            Assertions.assertInstanceOf(LifecycleEncoder.class, codec.getEncoder());
            Assertions.assertFalse(codec.getDecoder() instanceof LifecycleDecoder, "nothing claims a load event");
        }
    }

    private static MorphiaDatastore datastore(MongoClient client, MapperType mapperType) {
        MorphiaDatastore datastore = new MorphiaDatastore(client, ManualMorphiaConfig.configure()
                .database("lifecycle_streaming")
                .mapper(mapperType)
                .packages(List.of())
                .applyIndexes(false)
                .applyCaps(false)
                .applyDocumentValidations(false));
        datastore.getMapper().map(Streamed.class, Documented.class, Plain.class);
        return datastore;
    }

    @SuppressWarnings("unchecked")
    private static <T> MorphiaCodec<T> codec(MorphiaDatastore datastore, Class<T> type) {
        return (MorphiaCodec<T>) datastore.getCodecRegistry().get(type);
    }

    private static <T> BsonDocument encode(MorphiaCodec<T> codec, T entity) {
        BasicOutputBuffer buffer = new BasicOutputBuffer();
        try (BsonBinaryWriter writer = new BsonBinaryWriter(buffer)) {
            codec.encode(writer, entity, COLLECTIBLE);
        }
        return new RawBsonDocument(buffer.toByteArray());
    }

    private static <T> T decode(MorphiaCodec<T> codec, BsonDocument document) {
        RawBsonDocument raw = document instanceof RawBsonDocument r ? r : RawBsonDocument.parse(document.toJson());
        try (BsonBinaryReader reader = new BsonBinaryReader(raw.getByteBuffer().asNIO())) {
            return codec.decode(reader, DECODER);
        }
    }

    @Entity("lifecycle_streamed")
    public static class Streamed {
        @Id
        ObjectId id;
        String name;
        int version;
        transient List<String> events = new ArrayList<>();

        @PrePersist
        void prePersist() {
            version++;
            events.add("prePersist");
        }

        @PostPersist
        void postPersist() {
            events.add("postPersist");
        }

        @PreLoad
        void preLoad() {
            events.add("preLoad:" + name);
        }

        @PostLoad
        void postLoad() {
            events.add("postLoad:" + name);
        }
    }

    @Entity("lifecycle_documented")
    public static class Documented {
        @Id
        ObjectId id;
        String name;
        transient String persisted;
        transient String loaded;

        @PrePersist
        void prePersist(Document document) {
            document.put("extra", "added");
        }

        @PostPersist
        void postPersist(Document document) {
            persisted = document.getString("name");
        }

        @PostLoad
        void postLoad(Document document) {
            loaded = document.getString("extra");
        }
    }

    @Entity("lifecycle_plain")
    public static class Plain {
        @Id
        ObjectId id;
        String name;
    }

    /**
     * Claims one event, and records each call with the document's {@code name} (or null without a document).
     */
    private static class Recording implements EntityListener<Object> {
        private final Class<? extends Annotation> event;
        private final boolean usesDocument;
        private final List<String> calls = new ArrayList<>();

        Recording(Class<? extends Annotation> event, boolean usesDocument) {
            this.event = event;
            this.usesDocument = usesDocument;
        }

        @Override
        public boolean hasAnnotation(Class<? extends Annotation> type) {
            return event.equals(type);
        }

        @Override
        public boolean usesDocument(Class<? extends Annotation> type) {
            return usesDocument;
        }

        private void record(String name, Document document) {
            calls.add(name + ":" + (document == null ? null : document.getString("name")));
        }

        @Override
        public void prePersist(Object entity, Document document, Object datastore) {
            record("PrePersist", document);
        }

        @Override
        public void postPersist(Object entity, Document document, Object datastore) {
            record("PostPersist", document);
        }

        @Override
        public void preLoad(Object entity, Document document, Object datastore) {
            record("PreLoad", document);
        }

        @Override
        public void postLoad(Object entity, Document document, Object datastore) {
            record("PostLoad", document);
        }
    }
}
