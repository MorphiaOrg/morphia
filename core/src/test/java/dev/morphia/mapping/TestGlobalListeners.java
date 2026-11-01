package dev.morphia.mapping;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;

import dev.morphia.Datastore;
import dev.morphia.MorphiaDatastore;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.PostLoad;
import dev.morphia.annotations.PrePersist;
import dev.morphia.config.ManualMorphiaConfig;
import dev.morphia.config.MorphiaConfig;

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
 * Global listeners named in {@link MorphiaConfig#listeners()} run for every entity, with their lifecycle methods found by
 * annotation and their arguments matched by type, as for an {@code @EntityListeners} class.
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

        // the mapper trims each name, as the file may space them out
        Assertions.assertEquals(List.of(Recording.class.getName(), Auditing.class.getName()),
                config.listeners().orElseThrow().stream().map(String::trim).toList());
    }

    @Test
    void setsTheListenersInCode() {
        MorphiaConfig config = ManualMorphiaConfig.configure().listeners(List.of(Recording.class));

        Assertions.assertEquals(Optional.of(List.of(Recording.class.getName())), config.listeners());
        Assertions.assertEquals(Optional.empty(), ManualMorphiaConfig.configure().listeners());
        Assertions.assertEquals(config.listeners(), ManualMorphiaConfig.configure(config).listeners(), "copies keep them");
    }

    @ParameterizedTest
    @EnumSource(MapperType.class)
    void runForEveryEntity(MapperType mapperType) {
        try (MongoClient client = MongoClients.create("mongodb://localhost:1")) {
            MorphiaDatastore datastore = datastore(client, config(mapperType).listeners(List.of(Recording.class)));
            Codec<Plain> codec = datastore.getCodecRegistry().get(Plain.class);
            Plain entity = new Plain();
            entity.name = "plain";

            decode(codec, encode(codec, entity));

            Assertions.assertEquals(List.of("prePersist:plain", "postLoad:plain:plain:true"), CALLS);
            Assertions.assertEquals(1, datastore.getMapper().getGlobalListeners().size());
            Assertions.assertTrue(datastore.getMapper().hasListeners());
        }
    }

    @ParameterizedTest
    @EnumSource(MapperType.class)
    void copiesShareTheListeners(MapperType mapperType) {
        try (MongoClient client = MongoClients.create("mongodb://localhost:1")) {
            MorphiaDatastore datastore = datastore(client, config(mapperType).listeners(List.of(Recording.class)));

            Assertions.assertSame(datastore.getMapper().getGlobalListeners(),
                    new MorphiaDatastore(datastore).getMapper().getGlobalListeners());
            Assertions.assertSame(datastore.getMapper().getGlobalListeners(), datastore.getMapper().copy().getGlobalListeners());
        }
    }

    @ParameterizedTest
    @EnumSource(MapperType.class)
    void anExceptionAListenerThrowsReachesTheCaller(MapperType mapperType) {
        try (MongoClient client = MongoClients.create("mongodb://localhost:1")) {
            MorphiaDatastore datastore = datastore(client, config(mapperType).listeners(List.of(Refusing.class)));
            Codec<Plain> codec = datastore.getCodecRegistry().get(Plain.class);

            IllegalStateException e = Assertions.assertThrows(IllegalStateException.class, () -> encode(codec, new Plain()));
            Assertions.assertEquals("refused", e.getMessage());
        }
    }

    @Test
    void rejectsAnUnknownClass() {
        MorphiaConfig config = config(MapperType.REFLECTION);
        ManualMorphiaConfig named = ManualMorphiaConfig.configure(config);
        MappingException e = Assertions.assertThrows(MappingException.class,
                () -> new ReflectiveMapper(withListenerNames(named, "dev.morphia.NoSuchListener")));
        Assertions.assertTrue(e.getMessage().contains("dev.morphia.NoSuchListener"), e.getMessage());
    }

    @Test
    void rejectsAClassWithoutLifecycleMethods() {
        MappingException e = Assertions.assertThrows(MappingException.class,
                () -> new ReflectiveMapper(config(MapperType.REFLECTION).listeners(List.of(NoCallbacks.class))));
        Assertions.assertTrue(e.getMessage().contains(NoCallbacks.class.getName()), e.getMessage());
    }

    private static MorphiaConfig withListenerNames(MorphiaConfig config, String... names) {
        // a class that can't be loaded can only be named, as in a config file
        return new ManualMorphiaConfig(config) {
            @Override
            public Optional<List<String>> listeners() {
                return Optional.of(List.of(names));
            }
        };
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

    /**
     * Records its calls; each method asks for different arguments.
     */
    public static class Recording {
        @PrePersist
        void prePersist(Plain entity) {
            CALLS.add("prePersist:" + entity.name);
        }

        @PostLoad
        void postLoad(Plain entity, Document document, Datastore datastore) {
            CALLS.add("postLoad:" + entity.name + ":" + document.getString("name") + ":" + (datastore != null));
        }
    }

    public static class Auditing {
        @PrePersist
        void audit(Object entity) {
        }
    }

    public static class Refusing {
        @PrePersist
        void refuse(Object entity) {
            throw new IllegalStateException("refused");
        }
    }

    public static class NoCallbacks {
        void notALifecycleMethod(Object entity) {
        }
    }
}
