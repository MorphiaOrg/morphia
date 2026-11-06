package dev.morphia.test.lifecycle;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;

import dev.morphia.Datastore;
import dev.morphia.EntityListener;
import dev.morphia.MorphiaDatastore;
import dev.morphia.PostLoadListener;
import dev.morphia.PrePersistListener;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.EntityListeners;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.PostLoad;
import dev.morphia.annotations.PrePersist;
import dev.morphia.config.ManualMorphiaConfig;
import dev.morphia.config.MorphiaConfig;
import dev.morphia.mapping.MapperType;
import dev.morphia.mapping.MappingException;
import dev.morphia.mapping.lifecycle.TypedListeners;
import dev.morphia.test.lifecycle.invalid.InvalidListeners;

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
 * Listeners that implement the per-event interfaces, {@link PrePersistListener} and the rest, are called for those events
 * alone, and only with entities of the interface's type argument.
 */
public class TestTypedListeners {
    static final List<String> CALLS = new ArrayList<>();

    @BeforeEach
    void reset() {
        CALLS.clear();
    }

    @ParameterizedTest
    @EnumSource(MapperType.class)
    void aTypedLambdaSeesEveryEntityToo(MapperType mapperType) {
        try (MongoClient client = MongoClients.create("mongodb://localhost:1")) {
            // the lambda's class implements the raw interface, so its type argument is lost, as the interfaces' javadoc says
            PrePersistListener<Plain> lambda = (entity, ds) -> CALLS.add(entity.name);
            MorphiaDatastore datastore = datastore(client, mapperType, lambda);
            datastore.getMapper().map(Other.class);
            Plain plain = new Plain();
            plain.name = "plain";

            encode(datastore.getCodecRegistry().get(Plain.class), plain);
            Assertions.assertEquals(List.of("plain"), CALLS);
            Codec<Other> others = datastore.getCodecRegistry().get(Other.class);
            Assertions.assertThrows(ClassCastException.class, () -> encode(others, new Other()));
        }
    }

    @ParameterizedTest
    @EnumSource(MapperType.class)
    void aTypedAnonymousClassKeepsItsTypeArgument(MapperType mapperType) {
        try (MongoClient client = MongoClients.create("mongodb://localhost:1")) {
            MorphiaDatastore datastore = datastore(client, mapperType, new PrePersistListener<Plain>() {
                @Override
                public void prePersist(Plain entity, Datastore ds) {
                    CALLS.add(entity.name);
                }
            });
            datastore.getMapper().map(Other.class);
            Plain plain = new Plain();
            plain.name = "plain";

            encode(datastore.getCodecRegistry().get(Plain.class), plain);
            encode(datastore.getCodecRegistry().get(Other.class), new Other());

            Assertions.assertEquals(List.of("plain"), CALLS);
        }
    }

    @ParameterizedTest
    @EnumSource(MapperType.class)
    void entityListenersClassesCanImplementTheInterfaces(MapperType mapperType) {
        try (MongoClient client = MongoClients.create("mongodb://localhost:1")) {
            MorphiaDatastore datastore = datastore(client, mapperType);
            datastore.getMapper().map(Listened.class);
            Codec<Listened> codec = datastore.getCodecRegistry().get(Listened.class);

            decode(codec, encode(codec, new Listened()));

            Assertions.assertEquals(List.of("created", "typed:1", "postLoad"), CALLS);
        }
    }

    @ParameterizedTest
    @EnumSource(MapperType.class)
    void entityListenersClassesCantDeclareAnnotatedMethods(MapperType mapperType) {
        try (MongoClient client = MongoClients.create("mongodb://localhost:1")) {
            MorphiaDatastore datastore = datastore(client, mapperType);

            MappingException e = Assertions.assertThrows(MappingException.class,
                    () -> datastore.getMapper().map(InvalidListeners.AnnotatedListened.class));
            Assertions.assertTrue(e.getMessage().contains(InvalidListeners.Annotated.class.getName() + " declares annotated()"),
                    e.getMessage());
        }
    }

    @ParameterizedTest
    @EnumSource(MapperType.class)
    void entityListenersClassesMustImplementAnInterface(MapperType mapperType) {
        try (MongoClient client = MongoClients.create("mongodb://localhost:1")) {
            MorphiaDatastore datastore = datastore(client, mapperType);

            MappingException e = Assertions.assertThrows(MappingException.class,
                    () -> datastore.getMapper().map(InvalidListeners.MarkerListened.class));
            Assertions.assertTrue(e.getMessage().contains(InvalidListeners.MarkerOnly.class.getName()), e.getMessage());
        }
    }

    @ParameterizedTest
    @EnumSource(MapperType.class)
    void anEntityListenersClassThatFailsToBeCreatedReportsWhy(MapperType mapperType) {
        try (MongoClient client = MongoClients.create("mongodb://localhost:1")) {
            MorphiaDatastore datastore = datastore(client, mapperType);

            MappingException e = Assertions.assertThrows(MappingException.class,
                    () -> datastore.getMapper().map(InvalidListeners.FailingListened.class));
            Assertions.assertTrue(e.getMessage().contains("no listener today"), e.getMessage());
        }
    }

    @ParameterizedTest
    @EnumSource(MapperType.class)
    void entityListenersClassesAreCreatedWhenTheEntityIsMapped(MapperType mapperType) {
        try (MongoClient client = MongoClients.create("mongodb://localhost:1")) {
            MorphiaDatastore datastore = datastore(client, mapperType);
            datastore.getMapper().map(Listened.class);

            Assertions.assertEquals(List.of("created"), CALLS);
        }
    }

    @ParameterizedTest
    @EnumSource(MapperType.class)
    void entitiesCanImplementTheInterfaces(MapperType mapperType) {
        try (MongoClient client = MongoClients.create("mongodb://localhost:1")) {
            MorphiaDatastore datastore = datastore(client, mapperType);
            datastore.getMapper().map(SelfListening.class);
            Codec<SelfListening> codec = datastore.getCodecRegistry().get(SelfListening.class);
            SelfListening entity = new SelfListening();

            decode(codec, encode(codec, entity));

            Assertions.assertEquals(List.of("self:" + entity.id), CALLS);
        }
    }

    @Test
    void findsTheEntityTypeThroughGenericSupertypes() {
        Assertions.assertTrue(TypedListeners.handles(PlainOnly.class, PrePersist.class));
        Assertions.assertFalse(TypedListeners.handles(PlainOnly.class, PostLoad.class));
        Assertions.assertFalse(TypedListeners.handlesAny(MarkerOnly.class));

        TypedListeners.invoke(new PlainOnly(), PrePersist.class, new Other(), new Document(), null);
        Assertions.assertEquals(List.of(), CALLS);
        Plain plain = new Plain();
        plain.name = "typed";
        TypedListeners.invoke(new PlainOnly(), PrePersist.class, plain, new Document(), null);
        Assertions.assertEquals(List.of("plainOnly:typed"), CALLS);
    }

    @Test
    void findsTheEntityTypeThroughAnEnclosingType() {
        PlainInner listener = new PlainInner(new Outer<>());

        TypedListeners.invoke(listener, PrePersist.class, new Other(), new Document(), null);
        Assertions.assertEquals(List.of(), CALLS);
        Plain plain = new Plain();
        plain.name = "inner";
        TypedListeners.invoke(listener, PrePersist.class, plain, new Document(), null);
        Assertions.assertEquals(List.of("inner:inner"), CALLS);
    }

    private static MorphiaDatastore datastore(MongoClient client, MapperType mapperType, EntityListener<?>... listeners) {
        MorphiaConfig config = ManualMorphiaConfig.configure()
                .database("typed_listeners")
                .mapper(mapperType)
                .packages(List.of())
                .applyIndexes(false)
                .applyCaps(false)
                .applyDocumentValidations(false)
                .listeners(List.of(listeners));
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

    @Entity("typed_listeners_plain")
    public static class Plain {
        @Id
        ObjectId id;
        String name;
    }

    @Entity("typed_listeners_other")
    public static class Other {
        @Id
        ObjectId id;
    }

    @Entity("typed_listeners_listened")
    @EntityListeners(Counting.class)
    public static class Listened {
        @Id
        ObjectId id;
    }

    @Entity("typed_listeners_self")
    public static class SelfListening implements PostLoadListener<SelfListening> {
        @Id
        ObjectId id = new ObjectId();

        @Override
        public void postLoad(SelfListening entity, Datastore datastore) {
            CALLS.add("self:" + id);
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

    public static class Outer<E> {
        public abstract class Inner implements PrePersistListener<E> {
        }
    }

    /**
     * Takes its entity type from its enclosing type's argument.
     */
    public static class PlainInner extends Outer<Plain>.Inner {
        PlainInner(Outer<Plain> outer) {
            outer.super();
        }

        @Override
        public void prePersist(Plain entity, Datastore datastore) {
            CALLS.add("inner:" + entity.name);
        }
    }

    public static class Counting implements PrePersistListener<Listened>, PostLoadListener<Listened> {
        private int count;

        Counting() {
            CALLS.add("created");
        }

        @Override
        public void prePersist(Listened entity, Datastore datastore) {
            CALLS.add("typed:" + ++count);
        }

        @Override
        public void postLoad(Listened entity, Datastore datastore) {
            CALLS.add("postLoad");
        }
    }

    public static class MarkerOnly implements EntityListener<Object> {
    }
}
