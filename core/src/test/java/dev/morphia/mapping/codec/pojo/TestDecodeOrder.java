package dev.morphia.mapping.codec.pojo;

import java.util.List;
import java.util.stream.Stream;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;

import dev.morphia.MorphiaDatastore;
import dev.morphia.annotations.AlsoLoad;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.PostLoad;
import dev.morphia.annotations.Property;
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
import org.bson.Document;
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

    /**
     * An {@code _id} mapped as an ordinary property, on a model with no id property, is decoded like any other property
     * even when it comes before the discriminator.
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("mappers")
    void idMappedAsAnOrdinaryPropertyIsKept(MapperType mapper) {
        withDatastore(mapper, datastore -> {
            Note note = decode(datastore, Note.class, "{\"_id\": \"k1\", \"_t\": \"%s\", \"text\": \"t\"}"
                    .formatted(Memo.class.getName()));

            Assertions.assertInstanceOf(Memo.class, note);
            Assertions.assertEquals("k1", note.key);
            Assertions.assertEquals("t", note.text);
        });
    }

    /**
     * An entity whose id is a document is in the session before the id's own properties are decoded, since one of them
     * might refer back to it.
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("mappers")
    void entityWithACompoundIdIsInTheSessionBeforeItsId(MapperType mapper) {
        withDatastore(mapper, datastore -> {
            Keyed keyed = decode(datastore, Keyed.class, "{\"_id\": {\"a\": \"x\", \"probe\": \"Keyed\"}, \"name\": \"n\"}");

            Assertions.assertEquals("x", keyed.id.a);
            Assertions.assertSame(keyed, keyed.id.probe.seen, "the probe in the id found the entity being decoded");
        });
    }

    /**
     * A codec provider can supply its own {@link MorphiaCodec} subclass for a subtype. Its {@code decode} runs whichever
     * layout the document has.
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("mappers")
    void aSubtypeCodecSubclassStillDecodes(MapperType mapper) {
        MorphiaDatastore[] holder = new MorphiaDatastore[1];
        int[] decodes = { 0 };
        CodecProvider custom = new CodecProvider() {
            @Override
            @SuppressWarnings("unchecked")
            public <T> Codec<T> get(Class<T> clazz, CodecRegistry registry) {
                if (clazz != Dog.class || holder[0] == null) {
                    return null;
                }
                MorphiaDatastore datastore = holder[0];
                return (Codec<T>) new CountingCodec<>(datastore, datastore.getMapper().getEntityModel(Dog.class), registry,
                        decodes);
            }
        };
        withDatastore(mapper, List.of(custom), datastore -> {
            holder[0] = datastore;
            Assertions.assertInstanceOf(MorphiaCodec.class, datastore.getCodecRegistry().get(Dog.class));
            Assertions.assertNotSame(MorphiaCodec.class, datastore.getCodecRegistry().get(Dog.class).getClass(),
                    "the provider's codec is the one used");

            Pet pet = decode(datastore, Pet.class, "{\"_id\": {\"$oid\": \"%s\"}, \"_t\": \"%s\", \"name\": \"c\"}"
                    .formatted(ID.toHexString(), DOG));

            Assertions.assertInstanceOf(Dog.class, pet);
            Assertions.assertEquals(1, decodes[0], "decodes through the provider's codec");
        });
    }

    /**
     * A subtype that doesn't use a discriminator can map the root's discriminator key as an ordinary property, which it then
     * decodes like any other.
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("mappers")
    void subtypeMappingTheDiscriminatorKeyKeepsIt(MapperType mapper) {
        withDatastore(mapper, datastore -> {
            Pet pet = decode(datastore, Pet.class, "{\"_id\": {\"$oid\": \"%s\"}, \"_t\": \"%s\", \"name\": \"l\"}"
                    .formatted(ID.toHexString(), Lizard.class.getName()));

            Assertions.assertInstanceOf(Lizard.class, pet);
            Assertions.assertEquals(Lizard.class.getName(), ((Lizard) pet).kind);
            Assertions.assertEquals(ID, pet.id);
        });
    }

    /**
     * An id read first under one of its load names isn't the key references use, so the entity is registered under the
     * stored {@code _id} found further on.
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("mappers")
    void idLoadNameDoesNotKeyTheEntity(MapperType mapper) {
        withDatastore(mapper, datastore -> {
            Aliased aliased = decode(datastore, Aliased.class, "{\"legacyId\": {\"$oid\": \"650000000000000000000399\"}, "
                    + "\"probe\": \"Aliased\", \"_id\": {\"$oid\": \"%s\"}}".formatted(ID.toHexString()));

            Assertions.assertEquals(ID, aliased.id);
            Assertions.assertSame(aliased, aliased.probe.seen, "the entity is in the session under its _id");
        });
    }

    private static <T> T decode(MorphiaDatastore datastore, Class<T> type, String json) {
        RawBsonDocument raw = RawBsonDocument.parse(BsonDocument.parse(json).toJson());
        try (BsonBinaryReader reader = new BsonBinaryReader(raw.getByteBuffer().asNIO())) {
            return datastore.getCodecRegistry().get(type).decode(reader, DecoderContext.builder().build());
        }
    }

    private static void withDatastore(MapperType mapper, java.util.function.Consumer<MorphiaDatastore> test) {
        withDatastore(mapper, List.of(), test);
    }

    private static void withDatastore(MapperType mapper, List<CodecProvider> providers,
            java.util.function.Consumer<MorphiaDatastore> test) {
        CodecProvider probes = new CodecProvider() {
            @Override
            @SuppressWarnings("unchecked")
            public <T> Codec<T> get(Class<T> clazz, CodecRegistry registry) {
                if (clazz == Probe.class) {
                    return (Codec<T>) new ProbeCodec();
                }
                for (CodecProvider provider : providers) {
                    Codec<T> codec = provider.get(clazz, registry);
                    if (codec != null) {
                        return codec;
                    }
                }
                return null;
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
            datastore.getMapper().map(Pet.class, Dog.class, Cat.class, Built.class, Note.class, Memo.class, Keyed.class, Lizard.class,
                    Aliased.class);
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
                // a Keyed is keyed by its compound id as stored, which is the one the probe is part of
                Object id = "Keyed".equals(collection) ? new Document("a", "x").append("probe", "Keyed") : ID;
                probe.seen = DecodeSession.current().lookup(collection, id);
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

    @Entity("Note")
    public abstract static class Note {
        @Property("_id")
        String key;
        String text;
    }

    @Entity("Memo")
    public static class Memo extends Note {
        public Memo() {
        }
    }

    @Entity(value = "Keyed", useDiscriminator = false)
    public static class Keyed {
        @Id
        CompoundId id;
        String name;

        public Keyed() {
        }
    }

    @Entity(useDiscriminator = false)
    public static class CompoundId {
        String a;
        Probe probe;

        public CompoundId() {
        }
    }

    @Entity(value = "Lizard", useDiscriminator = false)
    public static class Lizard extends Pet {
        @Property("_t")
        String kind;

        public Lizard() {
        }
    }

    @Entity(value = "Aliased", useDiscriminator = false)
    public static class Aliased {
        @Id
        @AlsoLoad("legacyId")
        ObjectId id;
        Probe probe;

        public Aliased() {
        }
    }

    /**
     * A codec a provider supplies for an entity, which counts its decodes.
     */
    private static class CountingCodec<T> extends MorphiaCodec<T> {
        private final int[] decodes;

        CountingCodec(MorphiaDatastore datastore, EntityModel model, CodecRegistry registry, int[] decodes) {
            super(datastore, model, List.of(), datastore.getMapper().getDiscriminatorLookup(), registry,
                    datastore.getMapper().getConversions());
            this.decodes = decodes;
        }

        @Override
        public T decode(BsonReader reader, DecoderContext decoderContext) {
            decodes[0]++;
            return super.decode(reader, decoderContext);
        }
    }
}
