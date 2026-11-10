package dev.morphia.mapping.codec.pojo;

import java.util.List;
import java.util.stream.Stream;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;

import dev.morphia.MorphiaDatastore;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.PostLoad;
import dev.morphia.config.ManualMorphiaConfig;
import dev.morphia.config.MorphiaConfig;
import dev.morphia.mapping.MapperType;
import dev.morphia.mapping.MappingException;
import dev.morphia.mapping.codec.DecodeSession;
import dev.morphia.mapping.codec.MorphiaCodecProvider;

import org.bson.BsonBinaryReader;
import org.bson.BsonDocument;
import org.bson.BsonDocumentWriter;
import org.bson.BsonReader;
import org.bson.BsonReaderMark;
import org.bson.BsonWriter;
import org.bson.RawBsonDocument;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecProvider;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.params.provider.Arguments.arguments;

/**
 * Whatever order a document stores its {@code _id}, discriminator and properties in, an entity is in the decode session
 * before any property that might refer back to it is decoded, which is what lets a reference cycle terminate, and decoding
 * never runs a constructor it then throws away. A {@link Probe} stands in for the reference: decoding one looks its entity
 * up in the session.
 */
public class TestDecodeOrder {
    private static final ObjectId ID = new ObjectId("650000000000000000000301");
    private static final String DOG = Dog.class.getName();
    private static final String CAT = Cat.class.getName();
    private static final String BUILT = Built.class.getName();

    private static int constructed;

    @BeforeEach
    void reset() {
        constructed = 0;
    }

    static Stream<Arguments> layouts() {
        String id = "\"_id\": {\"$oid\": \"%s\"}".formatted(ID.toHexString());
        String probe = "\"probe\": \"%s\"";
        return Stream.of(MapperType.values()).flatMap(mapper -> Stream.of(
                // as Morphia writes it
                arguments(mapper, "id, discriminator", Pet.class, "{%s, \"_t\": \"%s\", %s, \"name\": \"a\"}"
                        .formatted(id, DOG, probe.formatted("Dog"))),
                arguments(mapper, "discriminator, id", Pet.class, "{\"_t\": \"%s\", %s, %s}"
                        .formatted(DOG, id, probe.formatted("Dog"))),
                arguments(mapper, "discriminator, property, id", Pet.class, "{\"_t\": \"%s\", %s, %s}"
                        .formatted(DOG, probe.formatted("Dog"), id)),
                arguments(mapper, "id, property, discriminator", Pet.class, "{%s, \"name\": \"b\", \"_t\": \"%s\", %s}"
                        .formatted(id, DOG, probe.formatted("Dog"))),
                arguments(mapper, "property, id, discriminator", Pet.class, "{%s, %s, \"_t\": \"%s\"}"
                        .formatted(probe.formatted("Dog"), id, DOG)),
                arguments(mapper, "subtype's own codec", Dog.class, "{%s, \"_t\": \"%s\", %s}"
                        .formatted(id, DOG, probe.formatted("Dog"))),
                arguments(mapper, "no discriminator", Dog.class, "{%s, %s}".formatted(probe.formatted("Dog"), id)),
                arguments(mapper, "lifecycle subtype", Pet.class, "{%s, \"_t\": \"%s\", %s}"
                        .formatted(id, CAT, probe.formatted("Cat")))));
    }

    @ParameterizedTest(name = "{0}: {1}")
    @MethodSource("layouts")
    void entityIsInTheSessionBeforeItsProperties(MapperType mapper, String layout, Class<? extends Pet> type, String json) {
        withDatastore(mapper, datastore -> {
            Pet pet = decode(datastore, type, json);

            Assertions.assertEquals(ID, pet.id);
            Assertions.assertSame(pet, pet.probe.seen, "the probe found the entity being decoded");
            Assertions.assertEquals(1, constructed, "constructors run");
        });
    }

    static Stream<Arguments> constructedLayouts() {
        String id = "\"_id\": {\"$oid\": \"%s\"}".formatted(ID.toHexString());
        String probe = "\"probe\": \"Built\"";
        return Stream.of(MapperType.values()).flatMap(mapper -> Stream.of(
                arguments(mapper, "id, discriminator", "{%s, \"_t\": \"%s\", %s}".formatted(id, BUILT, probe)),
                arguments(mapper, "property, id", "{%s, %s, \"_t\": \"%s\"}".formatted(probe, id, BUILT))));
    }

    @ParameterizedTest(name = "{0}: {1}")
    @MethodSource("constructedLayouts")
    void anEntityBuiltFromItsValuesIsUnderConstruction(MapperType mapper, String layout, String json) {
        withDatastore(mapper, datastore -> {
            Built built = decode(datastore, Built.class, json);

            Assertions.assertEquals(ID, built.id);
            Assertions.assertEquals(Probe.CONSTRUCTING, built.probe.seen, "a reference back to it is a cycle it can't break");
        });
    }

    static Stream<Arguments> morphiaLayouts() {
        return Stream.of(MapperType.values()).flatMap(mapper -> Stream.of(
                arguments(mapper, Pet.class), arguments(mapper, Dog.class)));
    }

    /**
     * A document laid out as Morphia writes it is read in one pass. The only mark is the one taken at its start, to come back
     * to if the subtype's decoder can't pick up part-way; a second would mean the codec was found by reading ahead.
     */
    @ParameterizedTest(name = "{0}: {1}")
    @MethodSource("morphiaLayouts")
    void morphiaLayoutIsReadInOnePass(MapperType mapper, Class<? extends Pet> type) {
        withDatastore(mapper, datastore -> {
            Dog dog = new Dog();
            dog.id = ID;
            dog.name = "rex";
            dog.barks = 2;
            BsonDocument encoded = new BsonDocument();
            try (BsonDocumentWriter writer = new BsonDocumentWriter(encoded)) {
                datastore.getCodecRegistry().get(Dog.class).encode(writer, dog, EncoderContext.builder().build());
            }
            RawBsonDocument raw = RawBsonDocument.parse(encoded.toJson());
            Assertions.assertEquals(List.of("_id", "_t"), List.copyOf(raw.keySet()).subList(0, 2), "as Morphia writes it");

            int[] marks = { 0 };
            try (BsonBinaryReader reader = new BsonBinaryReader(raw.getByteBuffer().asNIO()) {
                @Override
                public BsonReaderMark getMark() {
                    marks[0]++;
                    return super.getMark();
                }
            }) {
                Pet pet = datastore.getCodecRegistry().get(type).decode(reader, DecoderContext.builder().build());
                Assertions.assertInstanceOf(Dog.class, pet);
                Assertions.assertEquals(2, ((Dog) pet).barks);
            }
            Assertions.assertEquals(1, marks[0], "marks taken");
        });
    }

    /**
     * A refresh decodes into the entity it was given, through the same pass, whatever subtype it is.
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("dev.morphia.mapping.codec.pojo.TestDecodeOrder#mappers")
    void refreshDecodesIntoTheGivenEntity(MapperType mapper) {
        withDatastore(mapper, datastore -> {
            Dog existing = new Dog();
            existing.id = ID;
            existing.barks = 1;
            constructed = 0;
            String json = "{\"_id\": {\"$oid\": \"%s\"}, \"_t\": \"%s\", \"name\": \"r\", \"barks\": 7, \"probe\": \"Dog\"}"
                    .formatted(ID.toHexString(), DOG);
            Codec<Dog> refresh = new MorphiaCodecProvider(datastore).getRefreshCodec(existing, datastore.getCodecRegistry());
            RawBsonDocument raw = RawBsonDocument.parse(json);
            Dog refreshed;
            try (BsonBinaryReader reader = new BsonBinaryReader(raw.getByteBuffer().asNIO())) {
                refreshed = refresh.decode(reader, DecoderContext.builder().build());
            }

            Assertions.assertSame(existing, refreshed);
            Assertions.assertEquals(7, existing.barks);
            Assertions.assertEquals("r", existing.name);
            Assertions.assertSame(existing, existing.probe.seen, "the entity was in the session before its properties");
            Assertions.assertEquals(0, constructed, "constructors run");
        });
    }

    static Stream<MapperType> mappers() {
        return Stream.of(MapperType.values());
    }

    private static <T> T decode(MorphiaDatastore datastore, Class<T> type, String json) {
        RawBsonDocument raw = RawBsonDocument.parse(BsonDocument.parse(json).toJson());
        try (BsonBinaryReader reader = new BsonBinaryReader(raw.getByteBuffer().asNIO())) {
            return datastore.getCodecRegistry().get(type).decode(reader, DecoderContext.builder().build());
        }
    }

    private static void withDatastore(MapperType mapper, java.util.function.Consumer<MorphiaDatastore> test) {
        CodecProvider probes = new CodecProvider() {
            @Override
            @SuppressWarnings("unchecked")
            public <T> Codec<T> get(Class<T> clazz, CodecRegistry registry) {
                return clazz == Probe.class ? (Codec<T>) new ProbeCodec() : null;
            }
        };
        MorphiaConfig config = ManualMorphiaConfig.configure()
                .database("decode_order")
                .mapper(mapper)
                .packages(List.of())
                .codecProvider(probes)
                .applyIndexes(false)
                .applyCaps(false)
                .applyDocumentValidations(false);
        try (MongoClient client = MongoClients.create("mongodb://localhost:1")) {
            MorphiaDatastore datastore = new MorphiaDatastore(client, config);
            datastore.getMapper().map(Pet.class, Dog.class, Cat.class, Built.class);
            test.accept(datastore);
        }
    }

    /**
     * Stored as the simple name of the entity it belongs to, whose collection it looks {@link #ID} up in.
     */
    public static class Probe {
        static final Object CONSTRUCTING = "constructing";

        Object seen;

        Probe() {
        }
    }

    private static class ProbeCodec implements Codec<Probe> {
        @Override
        public Probe decode(BsonReader reader, DecoderContext decoderContext) {
            Probe probe = new Probe();
            String collection = reader.readString();
            try {
                probe.seen = DecodeSession.current().lookup(collection, ID);
            } catch (MappingException e) {
                probe.seen = Probe.CONSTRUCTING;
            }
            return probe;
        }

        @Override
        public void encode(BsonWriter writer, Probe value, EncoderContext encoderContext) {
            writer.writeNull();
        }

        @Override
        public Class<Probe> getEncoderClass() {
            return Probe.class;
        }
    }

    @Entity("Pet")
    public abstract static class Pet {
        @Id
        ObjectId id;
        Probe probe;
        String name;

        protected Pet() {
            constructed++;
        }
    }

    @Entity("Dog")
    public static class Dog extends Pet {
        int barks;

        public Dog() {
        }
    }

    @Entity("Cat")
    public static class Cat extends Pet {
        public Cat() {
        }

        @PostLoad
        void loaded() {
        }
    }

    @Entity("Built")
    public static class Built {
        @Id
        final ObjectId id;
        final Probe probe;

        public Built(ObjectId id, Probe probe) {
            this.id = id;
            this.probe = probe;
        }
    }
}
