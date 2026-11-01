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
import dev.morphia.config.MorphiaConfig;
import dev.morphia.mapping.MapperType;
import dev.morphia.mapping.codec.DecodeSession;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Lifecycle callbacks get a {@code Document} only when one of them takes it; otherwise the entity is encoded and decoded
 * directly. An entity's own callbacks and the configured global listeners say so in their parameters, and a codec decides
 * when it's built. Listeners added with the deprecated {@code addInterceptor} keep their old behavior.
 */
public class TestLifecycleStreaming {
    private static final EncoderContext COLLECTIBLE = EncoderContext.builder().isEncodingCollectibleDocument(true).build();
    private static final DecoderContext DECODER = DecoderContext.builder().build();
    static final List<Object> CALLS = new ArrayList<>();

    @BeforeEach
    void reset() {
        CALLS.clear();
    }

    @ParameterizedTest
    @EnumSource(MapperType.class)
    void callbacksWithoutADocumentStream(MapperType mapperType) {
        try (MongoClient client = MongoClients.create("mongodb://localhost:1")) {
            MorphiaCodec<Streamed> codec = codec(datastore(client, config(mapperType)), Streamed.class);
            assertStreams(codec, true, true);
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
            MorphiaCodec<Documented> codec = codec(datastore(client, config(mapperType)), Documented.class);
            assertStreams(codec, false, false);
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
    void configuredListenersDecideByTheirParameters(MapperType mapperType) {
        try (MongoClient client = MongoClients.create("mongodb://localhost:1")) {
            MorphiaConfig config = config(mapperType).listeners(List.of(PersistsTheEntity.class, LoadsTheDocument.class));
            MorphiaCodec<Plain> codec = codec(datastore(client, config), Plain.class);
            // the persisting listener leaves the Document out, so encoding streams; the loading one takes it
            assertStreams(codec, true, false);

            Plain entity = new Plain();
            entity.name = "plain";
            decode(codec, encode(codec, entity));

            Assertions.assertEquals(List.of("PostPersist:plain", "PostLoad:plain:plain"), CALLS);
        }
    }

    @ParameterizedTest
    @EnumSource(MapperType.class)
    void configuredListenersApplyToTheEntitiesTheyAccept(MapperType mapperType) {
        try (MongoClient client = MongoClients.create("mongodb://localhost:1")) {
            // takes the Document, but only for a Plain
            MorphiaConfig config = config(mapperType).listeners(List.of(LoadsTheDocument.class));
            MorphiaDatastore datastore = datastore(client, config);

            Assertions.assertTrue(((LifecycleDecoder<?>) codec(datastore, Plain.class).getDecoder()).needsDocument());
            assertStreams(codec(datastore, Streamed.class), true, true);
            Assertions.assertFalse(codec(datastore, Keyed.class).getDecoder() instanceof LifecycleDecoder,
                    "no callback applies to a Keyed");
        }
    }

    @ParameterizedTest
    @EnumSource(MapperType.class)
    void aListenerForPersistingLeavesDecodingAlone(MapperType mapperType) {
        try (MongoClient client = MongoClients.create("mongodb://localhost:1")) {
            MorphiaConfig config = config(mapperType).listeners(List.of(PersistsTheEntity.class));
            MorphiaCodec<Plain> codec = codec(datastore(client, config), Plain.class);

            Assertions.assertInstanceOf(LifecycleEncoder.class, codec.getEncoder());
            Assertions.assertFalse(codec.getDecoder() instanceof LifecycleDecoder, "nothing handles a load event");
        }
    }

    @ParameterizedTest
    @EnumSource(MapperType.class)
    @SuppressWarnings({ "deprecation", "removal" })
    void interceptorsKeepTheirBehavior(MapperType mapperType) {
        try (MongoClient client = MongoClients.create("mongodb://localhost:1")) {
            MorphiaDatastore datastore = datastore(client, config(mapperType));
            Interceptor interceptor = new Interceptor();
            datastore.getMapper().addInterceptor(interceptor);
            MorphiaCodec<Plain> codec = codec(datastore, Plain.class);

            // as before: any interceptor puts both directions on the lifecycle codecs, it's called for every event, and
            // it always gets the document
            Assertions.assertInstanceOf(LifecycleEncoder.class, codec.getEncoder());
            Assertions.assertInstanceOf(LifecycleDecoder.class, codec.getDecoder());
            Plain entity = new Plain();
            entity.name = "intercepted";
            decode(codec, encode(codec, entity));

            Assertions.assertEquals(List.of("PrePersist:null", "PostPersist:intercepted", "PreLoad:intercepted",
                    "PostLoad:intercepted"), interceptor.calls);
        }
    }

    @ParameterizedTest
    @EnumSource(MapperType.class)
    @SuppressWarnings({ "deprecation", "removal" })
    void anInterceptorAddedLaterStillGetsTheDocument(MapperType mapperType) {
        try (MongoClient client = MongoClients.create("mongodb://localhost:1")) {
            MorphiaDatastore datastore = datastore(client, config(mapperType));
            MorphiaCodec<Streamed> codec = codec(datastore, Streamed.class);
            assertStreams(codec, true, true);
            Streamed entity = new Streamed();
            entity.name = "late";
            decode(codec, encode(codec, entity));

            Interceptor interceptor = new Interceptor();
            datastore.getMapper().addInterceptor(interceptor);
            decode(codec, encode(codec, entity));

            Assertions.assertEquals(List.of("PrePersist:null", "PostPersist:late", "PreLoad:late", "PostLoad:late"),
                    interceptor.calls, "the codec goes through a Document while there's an interceptor");
        }
    }

    @ParameterizedTest
    @EnumSource(MapperType.class)
    void constructedEntitiesAreInTheSessionDuringTheirCallbacks(MapperType mapperType) {
        try (MongoClient client = MongoClients.create("mongodb://localhost:1")) {
            MorphiaConfig config = config(mapperType).listeners(List.of(LooksUpConstructed.class));
            MorphiaCodec<Constructed> codec = codec(datastore(client, config), Constructed.class);
            Constructed entity = new Constructed(new ObjectId(), "built");

            Constructed decoded = decode(codec, encode(codec, entity));

            Assertions.assertEquals("built", decoded.name);
            Assertions.assertEquals(1, CALLS.size());
            Assertions.assertSame(decoded, CALLS.get(0));
        }
    }

    @ParameterizedTest
    @EnumSource(MapperType.class)
    void compositeIdsGetTheirLoadCallbacksOnce(MapperType mapperType) {
        try (MongoClient client = MongoClients.create("mongodb://localhost:1")) {
            MorphiaConfig config = config(mapperType).listeners(List.of(RecordsLoads.class));
            MorphiaCodec<Keyed> codec = codec(datastore(client, config), Keyed.class);
            Keyed entity = new Keyed();
            entity.id = new CompositeKey();
            entity.id.region = "eu";
            entity.id.number = 7;

            Keyed decoded = decode(codec, encode(codec, entity));

            Assertions.assertEquals("eu", decoded.id.region);
            // the id is decoded once, as a property, inside the entity's own callbacks
            Assertions.assertEquals(List.of("PreLoad:Keyed", "PreLoad:CompositeKey", "PostLoad:CompositeKey", "PostLoad:Keyed"),
                    CALLS);
        }
    }

    private static void assertStreams(MorphiaCodec<?> codec, boolean encoding, boolean decoding) {
        Assertions.assertEquals(!encoding, ((LifecycleEncoder<?>) codec.getEncoder()).needsDocument(), "encoding streams");
        Assertions.assertEquals(!decoding, ((LifecycleDecoder<?>) codec.getDecoder()).needsDocument(), "decoding streams");
    }

    private static MorphiaConfig config(MapperType mapperType) {
        return ManualMorphiaConfig.configure()
                .database("lifecycle_streaming")
                .mapper(mapperType)
                .packages(List.of())
                .applyIndexes(false)
                .applyCaps(false)
                .applyDocumentValidations(false);
    }

    private static MorphiaDatastore datastore(MongoClient client, MorphiaConfig config) {
        MorphiaDatastore datastore = new MorphiaDatastore(client, config);
        datastore.getMapper().map(Streamed.class, Documented.class, Plain.class, Constructed.class, Keyed.class, CompositeKey.class);
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

    @Entity("lifecycle_constructed")
    public static class Constructed {
        @Id
        final ObjectId id;
        final String name;

        public Constructed(ObjectId id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    @Entity("lifecycle_keyed")
    public static class Keyed {
        @Id
        CompositeKey id;
    }

    /**
     * An id stored as a document with a discriminator, like {@code CustomId}.
     */
    @Entity
    public static class CompositeKey {
        String region;
        int number;
    }

    @Entity("lifecycle_plain")
    public static class Plain {
        @Id
        ObjectId id;
        String name;
    }

    /**
     * A configured global listener for {@code @PostPersist} that leaves the Document out.
     */
    public static class PersistsTheEntity {
        @PostPersist
        void postPersist(Plain entity) {
            CALLS.add("PostPersist:" + entity.name);
        }
    }

    /**
     * A configured global listener for {@code @PostLoad} that takes the Document.
     */
    public static class LoadsTheDocument {
        @PostLoad
        void postLoad(Plain entity, Document document) {
            CALLS.add("PostLoad:" + entity.name + ":" + document.getString("name"));
        }
    }

    /**
     * Looks a constructed entity up in the session from its {@code @PostLoad}, as a reference back to it would.
     */
    public static class LooksUpConstructed {
        @PostLoad
        void postLoad(Constructed entity) {
            CALLS.add(DecodeSession.current().lookup("lifecycle_constructed", entity.id));
        }
    }

    /**
     * Records the load events of every entity.
     */
    public static class RecordsLoads {
        @PreLoad
        void preLoad(Object entity) {
            CALLS.add("PreLoad:" + entity.getClass().getSimpleName());
        }

        @PostLoad
        void postLoad(Object entity) {
            CALLS.add("PostLoad:" + entity.getClass().getSimpleName());
        }
    }

    /**
     * An interceptor, added with the deprecated {@code addInterceptor}, that claims only {@code @PrePersist} and records
     * every call with the document's {@code name}.
     */
    private static class Interceptor implements EntityListener<Object> {
        private final List<String> calls = new ArrayList<>();

        @Override
        public boolean hasAnnotation(Class<? extends Annotation> type) {
            return type == PrePersist.class;
        }

        private void record(String name, Document document) {
            calls.add(name + ":" + document.getString("name"));
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
