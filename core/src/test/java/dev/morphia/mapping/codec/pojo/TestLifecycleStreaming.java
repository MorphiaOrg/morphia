package dev.morphia.mapping.codec.pojo;

import java.util.ArrayList;
import java.util.List;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;

import dev.morphia.Datastore;
import dev.morphia.MorphiaDatastore;
import dev.morphia.PostLoadDocumentListener;
import dev.morphia.PostLoadListener;
import dev.morphia.PostPersistDocumentListener;
import dev.morphia.PostPersistListener;
import dev.morphia.PreLoadListener;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.PostLoad;
import dev.morphia.annotations.PostPersist;
import dev.morphia.annotations.PreLoad;
import dev.morphia.annotations.PrePersist;
import dev.morphia.config.ManualMorphiaConfig;
import dev.morphia.config.MorphiaConfig;
import dev.morphia.mapping.MapperType;
import dev.morphia.mapping.MappingException;
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
 * Lifecycle listeners get a {@code Document} only when one of them takes it; otherwise the entity is encoded and decoded
 * directly. An entity's own callbacks say so in their parameters, and the per-event listener interfaces in which of them
 * they implement. A codec decides when it's built.
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
    void configuredListenersDecideByTheirInterfaces(MapperType mapperType) {
        try (MongoClient client = MongoClients.create("mongodb://localhost:1")) {
            MorphiaConfig config = config(mapperType).listeners(List.of(new PersistsTheEntity(), new LoadsTheDocument()));
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
            MorphiaDatastore datastore = datastore(client, config(mapperType).listeners(List.of(new LoadsTheDocument())));

            Assertions.assertTrue(((LifecycleDecoder<?>) codec(datastore, Plain.class).getDecoder()).needsDocument());
            assertStreams(codec(datastore, Streamed.class), true, true);
            Assertions.assertFalse(codec(datastore, Keyed.class).getDecoder() instanceof LifecycleDecoder,
                    "no listener applies to a Keyed");
        }
    }

    @ParameterizedTest
    @EnumSource(MapperType.class)
    void aListenerForPersistingLeavesDecodingAlone(MapperType mapperType) {
        try (MongoClient client = MongoClients.create("mongodb://localhost:1")) {
            MorphiaConfig config = config(mapperType).listeners(List.of(new PersistsTheEntity()));
            MorphiaCodec<Plain> codec = codec(datastore(client, config), Plain.class);

            Assertions.assertInstanceOf(LifecycleEncoder.class, codec.getEncoder());
            Assertions.assertFalse(codec.getDecoder() instanceof LifecycleDecoder, "nothing handles a load event");
        }
    }

    @ParameterizedTest
    @EnumSource(MapperType.class)
    void aPostPersistDocumentListenerChangesWhatIsWritten(MapperType mapperType) {
        try (MongoClient client = MongoClients.create("mongodb://localhost:1")) {
            MorphiaConfig config = config(mapperType).listeners(List.of(new Signs()));
            MorphiaCodec<Plain> codec = codec(datastore(client, config), Plain.class);
            Assertions.assertTrue(((LifecycleEncoder<?>) codec.getEncoder()).needsDocument());
            Plain entity = new Plain();
            entity.name = "signed";

            BsonDocument encoded = encode(codec, entity);

            // the listener sees the encoded entity, and what it adds is written
            Assertions.assertEquals("signed:signed", encoded.getString("signature").getValue());
        }
    }

    @ParameterizedTest
    @EnumSource(MapperType.class)
    void aPreLoadListenerGetsTheDocument(MapperType mapperType) {
        try (MongoClient client = MongoClients.create("mongodb://localhost:1")) {
            MorphiaConfig config = config(mapperType).listeners(List.of(new PreLoadsTheDocument()));
            MorphiaCodec<Plain> codec = codec(datastore(client, config), Plain.class);
            Assertions.assertTrue(((LifecycleDecoder<?>) codec.getDecoder()).needsDocument());
            Plain entity = new Plain();
            entity.name = "preloaded";

            decode(codec, encode(codec, entity));

            Assertions.assertEquals(List.of("PreLoad:null:preloaded"), CALLS);
        }
    }

    @ParameterizedTest
    @EnumSource(MapperType.class)
    void entitiesCanTakeTheDocumentThroughTheInterfaces(MapperType mapperType) {
        try (MongoClient client = MongoClients.create("mongodb://localhost:1")) {
            MorphiaCodec<SelfDocumenting> codec = codec(datastore(client, config(mapperType)), SelfDocumenting.class);
            assertStreams(codec, false, false);
            SelfDocumenting entity = new SelfDocumenting();
            entity.name = "self";

            BsonDocument encoded = encode(codec, entity);
            Assertions.assertEquals("self", encoded.getString("copy").getValue());

            Assertions.assertEquals("self", decode(codec, encoded).copied);
        }
    }

    @ParameterizedTest
    @EnumSource(MapperType.class)
    void bothInterfacesForAnEventAreRejected(MapperType mapperType) {
        try (MongoClient client = MongoClients.create("mongodb://localhost:1")) {
            MorphiaConfig config = config(mapperType).listeners(List.of(new LoadsTwice()));
            MappingException configured = Assertions.assertThrows(MappingException.class, () -> new MorphiaDatastore(client, config));
            Assertions.assertTrue(configured.getMessage().contains("PostLoadListener and PostLoadDocumentListener"),
                    configured.getMessage());

            MorphiaDatastore datastore = new MorphiaDatastore(client, config(mapperType));
            Assertions.assertThrows(MappingException.class, () -> datastore.getMapper().map(PersistsTwice.class));
        }
    }

    @ParameterizedTest
    @EnumSource(MapperType.class)
    void constructedEntitiesAreInTheSessionDuringTheirCallbacks(MapperType mapperType) {
        try (MongoClient client = MongoClients.create("mongodb://localhost:1")) {
            MorphiaConfig config = config(mapperType).listeners(List.of(new LooksUpConstructed()));
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
            MorphiaConfig config = config(mapperType).listeners(List.of(new RecordsLoads()));
            MorphiaCodec<Keyed> codec = codec(datastore(client, config), Keyed.class);
            Assertions.assertFalse(((LifecycleDecoder<?>) codec.getDecoder()).needsDocument());
            Keyed entity = new Keyed();
            entity.id = new CompositeKey();
            entity.id.region = "eu";
            entity.id.number = 7;

            Keyed decoded = decode(codec, encode(codec, entity));

            Assertions.assertEquals("eu", decoded.id.region);
            // the id is decoded once, as a property, before the entity's own callback
            Assertions.assertEquals(List.of("PostLoad:CompositeKey", "PostLoad:Keyed"), CALLS);
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
        datastore.getMapper().map(Streamed.class, Documented.class, Plain.class, Constructed.class, Keyed.class, CompositeKey.class,
                SelfDocumenting.class);
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

    @Entity("lifecycle_self_documenting")
    public static class SelfDocumenting implements PostPersistDocumentListener<SelfDocumenting>,
            PostLoadDocumentListener<SelfDocumenting> {
        @Id
        ObjectId id;
        String name;
        transient String copied;

        @Override
        public void postPersist(SelfDocumenting entity, Document document, Datastore datastore) {
            document.put("copy", document.getString("name"));
        }

        @Override
        public void postLoad(SelfDocumenting entity, Document document, Datastore datastore) {
            copied = document.getString("copy");
        }
    }

    @Entity("lifecycle_persists_twice")
    public static class PersistsTwice implements PostPersistListener<PersistsTwice>, PostPersistDocumentListener<PersistsTwice> {
        @Id
        ObjectId id;

        @Override
        public void postPersist(PersistsTwice entity, Datastore datastore) {
        }

        @Override
        public void postPersist(PersistsTwice entity, Document document, Datastore datastore) {
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
    public static class PersistsTheEntity implements PostPersistListener<Plain> {
        @Override
        public void postPersist(Plain entity, Datastore datastore) {
            CALLS.add("PostPersist:" + entity.name);
        }
    }

    /**
     * A configured global listener for {@code @PostLoad} that takes the Document.
     */
    public static class LoadsTheDocument implements PostLoadDocumentListener<Plain> {
        @Override
        public void postLoad(Plain entity, Document document, Datastore datastore) {
            CALLS.add("PostLoad:" + entity.name + ":" + document.getString("name"));
        }
    }

    /**
     * Signs the encoded document.
     */
    public static class Signs implements PostPersistDocumentListener<Plain> {
        @Override
        public void postPersist(Plain entity, Document document, Datastore datastore) {
            document.put("signature", entity.name + ":" + document.getString("name"));
        }
    }

    public static class PreLoadsTheDocument implements PreLoadListener<Plain> {
        @Override
        public void preLoad(Plain entity, Document document, Datastore datastore) {
            CALLS.add("PreLoad:" + entity.name + ":" + document.getString("name"));
        }
    }

    /**
     * Implements both interfaces for {@code @PostLoad}, which isn't allowed.
     */
    public static class LoadsTwice implements PostLoadListener<Object>, PostLoadDocumentListener<Object> {
        @Override
        public void postLoad(Object entity, Datastore datastore) {
        }

        @Override
        public void postLoad(Object entity, Document document, Datastore datastore) {
        }
    }

    /**
     * Looks a constructed entity up in the session from its {@code @PostLoad}, as a reference back to it would.
     */
    public static class LooksUpConstructed implements PostLoadListener<Constructed> {
        @Override
        public void postLoad(Constructed entity, Datastore datastore) {
            CALLS.add(DecodeSession.current().lookup("lifecycle_constructed", entity.id));
        }
    }

    /**
     * Records the {@code @PostLoad} events of every entity.
     */
    public static class RecordsLoads implements PostLoadListener<Object> {
        @Override
        public void postLoad(Object entity, Datastore datastore) {
            CALLS.add("PostLoad:" + entity.getClass().getSimpleName());
        }
    }
}
