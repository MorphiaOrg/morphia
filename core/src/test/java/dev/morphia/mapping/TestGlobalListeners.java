package dev.morphia.mapping;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;

import dev.morphia.Datastore;
import dev.morphia.EntityListener;
import dev.morphia.MorphiaDatastore;
import dev.morphia.PostLoadListener;
import dev.morphia.PostPersistListener;
import dev.morphia.PreLoadListener;
import dev.morphia.PrePersistListener;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.PreLoad;
import dev.morphia.config.ManualMorphiaConfig;
import dev.morphia.config.MorphiaConfig;
import dev.morphia.config.converters.EntityListenerConverter;

import org.bson.BsonBinaryReader;
import org.bson.BsonBinaryWriter;
import org.bson.Document;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.io.BasicOutputBuffer;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Global listeners listed in {@link MorphiaConfig#listeners()} run for every entity their per-event interfaces accept.
 * {@code TestTypedListeners} covers how a listener's entity type is found.
 */
public class TestGlobalListeners {
    static final List<String> CALLS = new ArrayList<>();

    @BeforeEach
    void reset() {
        CALLS.clear();
    }

    @Test
    void readsTheListenersFromAConfigFile() {
        MorphiaConfig config = MorphiaConfig.load("META-INF/morphia-config-listeners.properties");

        // the file spaces the names out
        Assertions.assertEquals(List.of(Recording.class, Auditing.class),
                config.listeners().orElseThrow().stream().map(Object::getClass).toList());
    }

    @Test
    void setsTheListenersInCode() {
        Recording recording = new Recording();
        MorphiaConfig config = ManualMorphiaConfig.configure().listeners(List.of(recording));

        Assertions.assertEquals(Optional.of(List.of(recording)), config.listeners());
        Assertions.assertEquals(Optional.empty(), ManualMorphiaConfig.configure().listeners());
        Assertions.assertEquals(config.listeners(), ManualMorphiaConfig.configure(config).listeners(), "copies keep them");
    }

    @ParameterizedTest
    @EnumSource(MapperType.class)
    void runForEveryEntity(MapperType mapperType) {
        try (MongoClient client = MongoClients.create("mongodb://localhost:1")) {
            MorphiaDatastore datastore = datastore(client, config(mapperType).listeners(List.of(new Recording())));
            Codec<Plain> codec = datastore.getCodecRegistry().get(Plain.class);
            Plain entity = new Plain();
            entity.name = "plain";

            decode(codec, encode(codec, entity));

            Assertions.assertEquals(List.of("prePersist:plain", "postPersist:plain", "preLoad:plain", "postLoad:plain:true"), CALLS);
            Assertions.assertEquals(1, datastore.getMapper().getGlobalListeners().size());
            Assertions.assertTrue(datastore.getMapper().hasListeners());
            Assertions.assertTrue(datastore.hasLifecycle(datastore.getMapper().getEntityModel(Plain.class), PreLoad.class));
        }
    }

    @ParameterizedTest
    @EnumSource(MapperType.class)
    void listenersSeeOnlyTheEntitiesTheyAccept(MapperType mapperType) {
        try (MongoClient client = MongoClients.create("mongodb://localhost:1")) {
            MorphiaDatastore datastore = datastore(client, config(mapperType).listeners(List.of(new Recording(), new PlainOnly())));
            datastore.getMapper().map(Other.class, Holder.class);
            Codec<Other> others = datastore.getCodecRegistry().get(Other.class);
            Codec<Holder> holders = datastore.getCodecRegistry().get(Holder.class);

            // both listeners take a Plain: an unrelated entity doesn't reach them
            decode(others, encode(others, new Other()));
            Assertions.assertEquals(List.of(), CALLS);

            // a Plain held by another entity does
            Holder holder = new Holder();
            holder.plain = new Plain();
            holder.plain.name = "held";
            encode(holders, holder);
            Assertions.assertEquals(List.of("prePersist:held", "plainOnly:held", "postPersist:held"), CALLS);
        }
    }

    @ParameterizedTest
    @EnumSource(MapperType.class)
    void aLambdaSeesEveryEntity(MapperType mapperType) {
        try (MongoClient client = MongoClients.create("mongodb://localhost:1")) {
            PrePersistListener<Object> lambda = (entity, datastore) -> CALLS.add(entity.getClass().getSimpleName());
            MorphiaDatastore datastore = datastore(client, config(mapperType).listeners(List.of(lambda)));
            datastore.getMapper().map(Other.class);

            encode(datastore.getCodecRegistry().get(Plain.class), new Plain());
            encode(datastore.getCodecRegistry().get(Other.class), new Other());

            Assertions.assertEquals(List.of("Plain", "Other"), CALLS);
        }
    }

    @ParameterizedTest
    @EnumSource(MapperType.class)
    void aPersistListenerLeavesConstructorMappedEntitiesReadable(MapperType mapperType) {
        try (MongoClient client = MongoClients.create("mongodb://localhost:1")) {
            MorphiaDatastore datastore = datastore(client, config(mapperType).listeners(List.of(new Auditing())));
            datastore.getMapper().map(Immutable.class);
            Codec<Immutable> codec = datastore.getCodecRegistry().get(Immutable.class);

            Immutable decoded = decode(codec, encode(codec, new Immutable(new ObjectId(), "kept")));

            Assertions.assertEquals("kept", decoded.name);
        }
    }

    @ParameterizedTest
    @EnumSource(MapperType.class)
    void copiesShareTheListeners(MapperType mapperType) {
        try (MongoClient client = MongoClients.create("mongodb://localhost:1")) {
            Recording recording = new Recording();
            MorphiaDatastore datastore = datastore(client, config(mapperType).listeners(List.of(recording)));

            Assertions.assertSame(datastore.getMapper(), new MorphiaDatastore(datastore).getMapper());
            Assertions.assertSame(recording, datastore.getMapper().copy().getGlobalListeners().get(0));
        }
    }

    @ParameterizedTest
    @EnumSource(MapperType.class)
    void anExceptionAListenerThrowsReachesTheCaller(MapperType mapperType) {
        try (MongoClient client = MongoClients.create("mongodb://localhost:1")) {
            PrePersistListener<Object> refusing = (entity, datastore) -> {
                throw new IllegalStateException("refused");
            };
            MorphiaDatastore datastore = datastore(client, config(mapperType).listeners(List.of(refusing)));
            Codec<Plain> codec = datastore.getCodecRegistry().get(Plain.class);

            IllegalStateException e = Assertions.assertThrows(IllegalStateException.class, () -> encode(codec, new Plain()));
            Assertions.assertEquals("refused", e.getMessage());
        }
    }

    @Test
    void rejectsAListenerThatHandlesNoEvents() {
        MappingException e = Assertions.assertThrows(MappingException.class,
                () -> new ReflectiveMapper(config(MapperType.REFLECTION).listeners(List.of(new NoCallbacks()))));
        Assertions.assertTrue(e.getMessage().contains(NoCallbacks.class.getName()), e.getMessage());
    }

    @Test
    void theConverterRejectsBadClasses() {
        EntityListenerConverter converter = new EntityListenerConverter();
        Assertions.assertInstanceOf(Recording.class, converter.convert("  " + Recording.class.getName() + " "));

        for (String name : List.of("dev.morphia.NoSuchListener", NotAListener.class.getName(), NeedsArguments.class.getName())) {
            MappingException e = Assertions.assertThrows(MappingException.class, () -> converter.convert(name));
            Assertions.assertTrue(e.getMessage().contains(name), e.getMessage());
        }

        // a constructor's failure is reported with the class and the failure itself, and kept as the cause
        MappingException e = Assertions.assertThrows(MappingException.class, () -> converter.convert(Failing.class.getName()));
        Assertions.assertTrue(e.getMessage().contains(Failing.class.getName()), e.getMessage());
        Assertions.assertTrue(e.getMessage().contains("no configuration"), e.getMessage());
        Assertions.assertInstanceOf(IllegalStateException.class, e.getCause());

        // so is a static initializer's, which loading the class runs
        String failingStatic = FailingStatic.class.getName();
        MappingException initializer = Assertions.assertThrows(MappingException.class, () -> converter.convert(failingStatic));
        Assertions.assertTrue(initializer.getMessage().contains(failingStatic), initializer.getMessage());
        Assertions.assertTrue(initializer.getMessage().contains("no static configuration"), initializer.getMessage());
    }

    private static MorphiaConfig config(MapperType mapperType) {
        return ManualMorphiaConfig.configure()
                .database("global_listeners")
                .mapper(mapperType)
                .packages(List.of())
                .applyIndexes(false)
                .applyCaps(false)
                .applyDocumentValidations(false);
    }

    private static MorphiaDatastore datastore(MongoClient client, MorphiaConfig config) {
        MorphiaDatastore datastore = new MorphiaDatastore(client, config);
        datastore.getMapper().map(Plain.class);
        return datastore;
    }

    private static <T> byte[] encode(Codec<T> codec, T entity) {
        BasicOutputBuffer buffer = new BasicOutputBuffer();
        try (BsonBinaryWriter writer = new BsonBinaryWriter(buffer)) {
            codec.encode(writer, entity, EncoderContext.builder().isEncodingCollectibleDocument(true).build());
        }
        return buffer.toByteArray();
    }

    private static <T> T decode(Codec<T> codec, byte[] bytes) {
        try (BsonBinaryReader reader = new BsonBinaryReader(ByteBuffer.wrap(bytes))) {
            return codec.decode(reader, DecoderContext.builder().build());
        }
    }

    @Entity("global_listeners_plain")
    public static class Plain {
        @Id
        ObjectId id;
        String name;
    }

    @Entity("global_listeners_immutable")
    public static class Immutable {
        @Id
        final ObjectId id;
        final String name;

        public Immutable(ObjectId id, String name) {
            this.id = id;
            this.name = java.util.Objects.requireNonNull(name);
        }
    }

    @Entity("global_listeners_other")
    public static class Other {
        @Id
        ObjectId id;
    }

    @Entity("global_listeners_holder")
    public static class Holder {
        @Id
        ObjectId id;
        Plain plain;
    }

    /**
     * Records its calls for every event.
     */
    public static class Recording implements PrePersistListener<Plain>, PostPersistListener<Plain>, PreLoadListener<Plain>,
            PostLoadListener<Plain> {
        @Override
        public void prePersist(Plain entity, Datastore datastore) {
            CALLS.add("prePersist:" + entity.name);
        }

        @Override
        public void postPersist(Plain entity, Datastore datastore) {
            CALLS.add("postPersist:" + entity.name);
        }

        @Override
        public void preLoad(Plain entity, Document document, Datastore datastore) {
            CALLS.add("preLoad:" + document.getString("name"));
        }

        @Override
        public void postLoad(Plain entity, Datastore datastore) {
            CALLS.add("postLoad:" + entity.name + ":" + (datastore != null));
        }
    }

    public abstract static class Typed<E> implements PrePersistListener<E> {
    }

    /**
     * Takes its entity type from a generic superclass.
     */
    public static class PlainOnly extends Typed<Plain> {
        @Override
        public void prePersist(Plain entity, Datastore datastore) {
            CALLS.add("plainOnly:" + entity.name);
        }
    }

    public static class Auditing implements PrePersistListener<Object> {
        @Override
        public void prePersist(Object entity, Datastore datastore) {
        }
    }

    public static class NeedsArguments implements PrePersistListener<Object> {
        public NeedsArguments(String argument) {
        }

        @Override
        public void prePersist(Object entity, Datastore datastore) {
        }
    }

    public static class Failing implements PrePersistListener<Object> {
        public Failing() {
            throw new IllegalStateException("no configuration");
        }

        @Override
        public void prePersist(Object entity, Datastore datastore) {
        }
    }

    public static class FailingStatic implements PrePersistListener<Object> {
        static {
            if (Boolean.TRUE) {
                throw new IllegalStateException("no static configuration");
            }
        }

        @Override
        public void prePersist(Object entity, Datastore datastore) {
        }
    }

    public static class NoCallbacks implements EntityListener<Object> {
    }

    public static class NotAListener {
    }
}
