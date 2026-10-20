package dev.morphia.test.mapping.codec.harness;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;

import dev.morphia.MorphiaDatastore;
import dev.morphia.config.ManualMorphiaConfig;
import dev.morphia.config.MorphiaConfig;
import dev.morphia.mapping.DiscriminatorFunction;
import dev.morphia.mapping.MapperType;
import dev.morphia.mapping.codec.MorphiaCodecProvider;
import dev.morphia.mapping.codec.pojo.CodecPlan;
import dev.morphia.mapping.codec.pojo.EntityModel;
import dev.morphia.mapping.codec.pojo.MorphiaCodec;
import dev.morphia.mapping.codec.pojo.PropertyModel;
import dev.morphia.mapping.codec.pojo.critter.CritterEntityModel;
import dev.morphia.test.mapping.codec.harness.models.HarnessArrays;
import dev.morphia.test.mapping.codec.harness.models.HarnessCallbackCircle;
import dev.morphia.test.mapping.codec.harness.models.HarnessCallbackHolder;
import dev.morphia.test.mapping.codec.harness.models.HarnessCallbackShape;
import dev.morphia.test.mapping.codec.harness.models.HarnessCircle;
import dev.morphia.test.mapping.codec.harness.models.HarnessCollections;
import dev.morphia.test.mapping.codec.harness.models.HarnessConstructed;
import dev.morphia.test.mapping.codec.harness.models.HarnessDrawing;
import dev.morphia.test.mapping.codec.harness.models.HarnessIdField;
import dev.morphia.test.mapping.codec.harness.models.HarnessIdFieldAlias;
import dev.morphia.test.mapping.codec.harness.models.HarnessIdFieldLegacy;
import dev.morphia.test.mapping.codec.harness.models.HarnessLifecycle;
import dev.morphia.test.mapping.codec.harness.models.HarnessNested;
import dev.morphia.test.mapping.codec.harness.models.HarnessNoDiscriminator;
import dev.morphia.test.mapping.codec.harness.models.HarnessRecordHolder;
import dev.morphia.test.mapping.codec.harness.models.HarnessReferences;
import dev.morphia.test.mapping.codec.harness.models.HarnessRenames;
import dev.morphia.test.mapping.codec.harness.models.HarnessScalars;
import dev.morphia.test.mapping.codec.harness.models.HarnessShape;
import dev.morphia.test.mapping.codec.harness.models.HarnessStringListBox;

import org.bson.BsonBinaryReader;
import org.bson.BsonBinaryWriter;
import org.bson.BsonDocument;
import org.bson.BsonString;
import org.bson.BsonType;
import org.bson.RawBsonDocument;
import org.bson.codecs.BsonDocumentCodec;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.io.BasicOutputBuffer;
import org.bson.json.JsonMode;
import org.bson.json.JsonWriterSettings;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DynamicContainer;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * The differential codec harness: pins the BSON that Morphia's entity codecs write and how they read, so changes to the
 * encode/decode protocol can't silently change behavior.
 * <ul>
 * <li>Every case runs under both the reflection and the critter mapper, whatever {@code morphia.mapper} is set to,
 * and the two must produce identical BSON.</li>
 * <li>That BSON must match the golden fixtures in {@code src/test/resources/.../harness/}, one file per
 * configuration variant. Each sample records its encoding and its decode-then-encode round trip (which is not always
 * the identity, e.g. under {@code storeEmpties=false} with {@code storeNulls=true}). Each "foreign" document, one
 * Morphia never writes but must read, records what it decodes to.</li>
 * <li>Decoding goes through {@code MorphiaCodec.decode}, as in production, so it always runs inside a
 * {@code DecodeSession}, including #4257's {@code _id} pre-read.</li>
 * </ul>
 * Run with {@code -Dmorphia.harness.update=true} to rewrite the fixtures from the current code, then review the diff:
 * a fixture change is a behavior change.
 * <p>
 * Without pre-generated models the critter mapper generates them at runtime. To also check critter's AOT models, as
 * CI's critter AOT job does, generate them after compiling and run surefire alone, since recompiling replaces the woven
 * entity classes and leaves the generated accessors calling methods that no longer exist:
 *
 * <pre>
 * ./mvnw test-compile -pl :morphia-core
 * (cd core &amp;&amp; ../mvnw dev.morphia.morphia:critter-maven:generate-test-models)
 * (cd core &amp;&amp; ../mvnw surefire:test -Dtest=CodecHarnessTest)
 * </pre>
 */
@TestInstance(Lifecycle.PER_CLASS)
public class CodecHarnessTest {
    private static final boolean UPDATE = Boolean.getBoolean("morphia.harness.update");
    private static final String RESOURCE_DIR = "dev/morphia/test/mapping/codec/harness/";
    private static final JsonWriterSettings JSON = JsonWriterSettings.builder()
            .outputMode(JsonMode.EXTENDED)
            .indent(true)
            .build();
    private static final EncoderContext COLLECTIBLE = EncoderContext.builder().isEncodingCollectibleDocument(true).build();
    /** For re-encoding decoded entities: a collectible encode would give an entity without an id a random one. */
    private static final EncoderContext PLAIN = EncoderContext.builder().build();
    private static final DecoderContext DECODER = DecoderContext.builder().build();
    /** Critter can't generate models for array properties yet (game plan 4.8), so the whole entity uses reflection. */
    private static final Set<Class<?>> CRITTER_FALLBACKS = Set.of(HarnessArrays.class);

    private final MongoClient client = offlineClient();

    @AfterAll
    void close() {
        client.close();
    }

    @TestFactory
    List<DynamicNode> harness() {
        List<DynamicNode> nodes = new ArrayList<>();
        for (Variant variant : Variant.values()) {
            Map<String, Map<String, String>> reflection = run(variant, MapperType.REFLECTION);
            Map<String, Map<String, String>> critter = run(variant, MapperType.CRITTER);
            Map<String, Map<String, String>> golden;
            if (UPDATE) {
                List<String> diverging = reflection.keySet().stream()
                        .filter(name -> !reflection.get(name).equals(critter.get(name)))
                        .toList();
                assertTrue(diverging.isEmpty(), "the mappers disagree on " + diverging + "; fix that before updating "
                        + variant.file());
                write(variant, reflection);
                golden = reflection;
            } else {
                golden = read(variant);
            }

            List<DynamicNode> cases = new ArrayList<>();
            for (String name : reflection.keySet()) {
                cases.add(DynamicTest.dynamicTest(name, () -> {
                    assertEquals(reflection.get(name), critter.get(name), "reflection vs critter");
                    Map<String, String> expected = golden.get(name);
                    assertNotNull(expected, "no fixture for " + name + " in " + variant.file());
                    assertEquals(expected, reflection.get(name), "fixture " + variant.file());
                }));
            }
            cases.add(DynamicTest.dynamicTest("no stale fixtures",
                    () -> assertEquals(golden.keySet(), reflection.keySet(), "cases in " + variant.file())));
            nodes.add(DynamicContainer.dynamicContainer(variant.name(), cases));
        }
        return nodes;
    }

    @Test
    void generatesIds() {
        for (MapperType mapper : MapperType.values()) {
            MorphiaDatastore datastore = datastore(Variant.DEFAULTS, mapper);
            HarnessNoDiscriminator entity = new HarnessNoDiscriminator();
            encode(codec(datastore, HarnessNoDiscriminator.class), entity, COLLECTIBLE);
            assertInstanceOf(ObjectId.class, entity.getId(), mapper + " should generate an _id");
        }
    }

    @Test
    void runsLifecycleCallbacks() {
        for (MapperType mapper : MapperType.values()) {
            MorphiaDatastore datastore = datastore(Variant.DEFAULTS, mapper);
            Codec<HarnessLifecycle> codec = codec(datastore, HarnessLifecycle.class);
            HarnessLifecycle decoded = decode(codec, encode(codec, HarnessLifecycle.sample(), COLLECTIBLE));
            assertTrue(decoded.isLoaded(), mapper + " should run @PostLoad");
        }
    }

    @Test
    void runsPolymorphicLifecycleCallbacksOnce() {
        for (MapperType mapper : MapperType.values()) {
            MorphiaDatastore datastore = datastore(Variant.DEFAULTS, mapper);

            // a subtype held in a property declared as its supertype
            Codec<HarnessCallbackHolder> holderCodec = codec(datastore, HarnessCallbackHolder.class);
            HarnessCallbackHolder holder = HarnessCallbackHolder.sample();
            HarnessCallbackHolder decodedHolder = decode(holderCodec, encode(holderCodec, holder, COLLECTIBLE));
            assertCallbacks(mapper + " nested", holder.getShapes().get(0), 1, 1, 0, 0);
            assertCallbacks(mapper + " nested", decodedHolder.getShapes().get(0), 0, 0, 1, 1);

            // a subtype encoded and decoded through its supertype's codec
            Codec<HarnessCallbackShape> shapeCodec = codec(datastore, HarnessCallbackShape.class);
            HarnessCallbackCircle circle = new HarnessCallbackCircle(new ObjectId("650000000000000000000112"), 1);
            HarnessCallbackShape decoded = decode(shapeCodec, encode(shapeCodec, circle, COLLECTIBLE));
            assertCallbacks(mapper + " root", circle, 1, 1, 0, 0);
            assertInstanceOf(HarnessCallbackCircle.class, decoded);
            assertCallbacks(mapper + " root", decoded, 0, 0, 1, 1);
        }
    }

    private static void assertCallbacks(String message, HarnessCallbackShape shape, int prePersists, int postPersists,
            int preLoads, int postLoads) {
        assertEquals(List.of(prePersists, postPersists, preLoads, postLoads),
                List.of(shape.getPrePersists(), shape.getPostPersists(), shape.getPreLoads(), shape.getPostLoads()),
                message + ": @PrePersist, @PostPersist, @PreLoad, @PostLoad counts");
    }

    @Test
    void modelHashesAreStableAcrossEncoding() {
        for (MapperType mapper : MapperType.values()) {
            MorphiaDatastore datastore = datastore(Variant.DEFAULTS, mapper);
            EntityModel model = datastore.getMapper().getEntityModel(HarnessScalars.class);
            Set<EntityModel> models = new HashSet<>(Set.of(model));
            Set<PropertyModel> properties = new HashSet<>(model.getProperties());

            // encoding specializes each property's codec, which used to change its hash
            Codec<HarnessScalars> codec = codec(datastore, HarnessScalars.class);
            decode(codec, encode(codec, HarnessScalars.sample(), COLLECTIBLE));

            assertTrue(models.contains(model), mapper + ": the model's hash changed");
            for (PropertyModel property : model.getProperties()) {
                assertTrue(properties.contains(property), mapper + ": the hash of " + property.getName() + " changed");
            }
        }
    }

    @Test
    void concurrentCodecLookupsShareOneCodec() throws Exception {
        int threads = 8;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        try {
            for (MapperType mapper : MapperType.values()) {
                MorphiaDatastore datastore = datastore(Variant.DEFAULTS, mapper);
                for (int round = 0; round < 20; round++) {
                    MorphiaCodecProvider provider = new MorphiaCodecProvider(datastore);
                    CountDownLatch start = new CountDownLatch(1);
                    List<Future<Codec<HarnessScalars>>> results = new ArrayList<>();
                    for (int i = 0; i < threads; i++) {
                        results.add(executor.submit(() -> {
                            start.await();
                            return provider.get(HarnessScalars.class, datastore.getCodecRegistry());
                        }));
                    }
                    start.countDown();

                    Codec<HarnessScalars> first = results.get(0).get();
                    for (Future<Codec<HarnessScalars>> result : results) {
                        assertSame(first, result.get(), mapper + ": every lookup should get the one cached codec");
                    }
                }
            }
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void codecPlanResolvesEveryPropertyBySlot() {
        for (MapperType mapper : MapperType.values()) {
            MorphiaDatastore datastore = datastore(Variant.DEFAULTS, mapper);
            CodecPlan plan = ((MorphiaCodec<?>) codec(datastore, HarnessScalars.class)).getPlan();
            EntityModel model = datastore.getMapper().getEntityModel(HarnessScalars.class);

            assertSame(model, plan.model());
            for (PropertyModel property : model.getProperties()) {
                assertNotNull(plan.codec(property), mapper + ": no codec for " + property.getName());
            }
            // only types the codec can't fail part-way on skip the reader mark; drifted types keep it for the fallback
            PropertyModel name = model.getProperty("name");
            PropertyModel age = model.getProperty("age");
            assertTrue(plan.readsDirectly(name, BsonType.STRING), mapper + ": StringCodec reads STRING directly");
            assertFalse(plan.readsDirectly(name, BsonType.INT32), mapper + ": StringCodec rejects INT32");
            assertTrue(plan.readsDirectly(age, BsonType.INT32), mapper + ": IntegerCodec reads INT32 directly");
            assertFalse(plan.readsDirectly(age, BsonType.INT64), mapper + ": IntegerCodec may reject an INT64 after reading it");

            PropertyModel foreign = datastore.getMapper().getEntityModel(HarnessRenames.class).getProperties().get(1);
            assertThrows(IllegalArgumentException.class, () -> plan.codec(foreign),
                    mapper + ": a property of another model should be rejected");
        }
    }

    @Test
    void codecForDiscriminatorResolvesEachTypeOnce() {
        for (MapperType mapper : MapperType.values()) {
            MorphiaDatastore datastore = datastore(Variant.DEFAULTS, mapper);
            MorphiaCodec<HarnessShape> shapes = (MorphiaCodec<HarnessShape>) codec(datastore, HarnessShape.class);
            MorphiaCodec<?> circles = (MorphiaCodec<?>) codec(datastore, HarnessCircle.class);

            assertSame(circles, circles.codecForDiscriminator(circles.getEntityModel().discriminator()),
                    mapper + ": a codec's own discriminator resolves to itself");
            Codec<?> circle = shapes.codecForDiscriminator(circles.getEntityModel().discriminator());
            assertSame(circles, circle, mapper + ": a subtype's discriminator resolves to the subtype's codec");
            assertSame(circle, shapes.codecForDiscriminator(circles.getEntityModel().discriminator()),
                    mapper + ": and the same codec every time");
        }
    }

    @Test
    void readsLoadOnlyProperties() {
        // @LoadOnly values are never written, so the fixtures can't show that they are read
        BsonDocument document = BsonDocument.parse("{\"_id\": {\"$oid\": \"650000000000000000000105\"}, \"legacy\": \"kept\"}");
        for (MapperType mapper : MapperType.values()) {
            Codec<HarnessRenames> codec = codec(datastore(Variant.DEFAULTS, mapper), HarnessRenames.class);
            assertEquals("kept", decode(codec, bytes(document)).getLegacy(), mapper + " should read @LoadOnly");
        }
    }

    @Test
    void readsIdField() {
        // the fixtures record what was decoded, so this pins the @IdField property being read back from _id
        for (MapperType mapper : MapperType.values()) {
            MorphiaDatastore datastore = datastore(Variant.DEFAULTS, mapper);
            assertEquals("key", datastore.getMapper().getEntityModel(HarnessIdField.class).getIdProperty().getName(),
                    mapper + " should use the @IdField property as the id");
            Codec<HarnessIdField> codec = codec(datastore, HarnessIdField.class);
            HarnessIdField decoded = decode(codec, encode(codec, HarnessIdField.sample(), COLLECTIBLE));
            assertEquals("key-1", decoded.getKey(), mapper + " should read the @IdField property from _id");
            assertEquals("named by @IdField", decoded.getName());
        }
    }

    @Test
    void readsIdFieldThatAlsoLoadsFromId() {
        BsonDocument document = BsonDocument.parse("{\"_id\": \"alias-key\"}");
        for (MapperType mapper : MapperType.values()) {
            MorphiaDatastore datastore = datastore(Variant.DEFAULTS, mapper);
            assertEquals("key", datastore.getMapper().getEntityModel(HarnessIdFieldAlias.class).getIdProperty().getName());
            assertEquals("alias-key", decode(codec(datastore, HarnessIdFieldAlias.class), bytes(document)).getKey(),
                    mapper + " should read an @IdField property that also loads from _id");
        }
    }

    @Test
    void readsIdFieldFromItsOldNameWhenThatIsALoadName() {
        BsonDocument document = BsonDocument.parse("{\"legacy\": \"old-key\"}");
        for (MapperType mapper : MapperType.values()) {
            Codec<HarnessIdFieldLegacy> codec = codec(datastore(Variant.DEFAULTS, mapper), HarnessIdFieldLegacy.class);
            assertEquals("old-key", decode(codec, bytes(document)).getKey(),
                    mapper + " should still load the @IdField property from its declared load name");
        }
    }

    /**
     * @return case name → recorded results (encoding, round trip, or decoded foreign document) as canonical JSON
     */
    private Map<String, Map<String, String>> run(Variant variant, MapperType mapper) {
        MorphiaDatastore datastore = datastore(variant, mapper);
        if (mapper == MapperType.CRITTER) {
            assertCritterModels(datastore);
        }
        Map<String, Map<String, String>> results = new LinkedHashMap<>();

        sample(results, datastore, "scalars", HarnessScalars.class, HarnessScalars::sample);
        sample(results, datastore, "nested", HarnessNested.class, HarnessNested::sample);
        sample(results, datastore, "nested-sparse", HarnessNested.class, HarnessNested::sparse);
        sample(results, datastore, "nested-empty", HarnessNested.class, HarnessNested::empty);
        sample(results, datastore, "collections", HarnessCollections.class, HarnessCollections::sample);
        sample(results, datastore, "arrays", HarnessArrays.class, HarnessArrays::sample);
        sample(results, datastore, "polymorphic", HarnessDrawing.class, HarnessDrawing::sample);
        sample(results, datastore, "no-discriminator", HarnessNoDiscriminator.class, HarnessNoDiscriminator::sample);
        sample(results, datastore, "constructor", HarnessConstructed.class, HarnessConstructed::sample);
        sample(results, datastore, "records", HarnessRecordHolder.class, HarnessRecordHolder::sample);
        sample(results, datastore, "renames", HarnessRenames.class, HarnessRenames::sample);
        sample(results, datastore, "id-field", HarnessIdField.class, HarnessIdField::sample);
        sample(results, datastore, "lifecycle", HarnessLifecycle.class, HarnessLifecycle::sample);
        sample(results, datastore, "generic-binding", HarnessStringListBox.class, HarnessStringListBox::sample);
        encodeOnly(results, datastore, "references", HarnessReferences.class, HarnessReferences::sample);

        foreign(results, datastore, "scalars-reordered", HarnessScalars.class, """
                {"score": 7, "unknown": "ignored", "age": {"$numberLong": "5"}, "count": 9, "name": null,
                 "status": "OPEN", "_id": {"$oid": "650000000000000000000101"}}""");
        foreign(results, datastore, "scalars-discriminator-late", HarnessScalars.class, """
                {"_id": {"$oid": "650000000000000000000102"}, "age": 3.0,
                 "_t": "dev.morphia.test.mapping.codec.harness.models.HarnessScalars"}""");
        foreign(results, datastore, "scalars-schema-drift", HarnessScalars.class, """
                {"active": "true", "age": "42", "score": "1.5"}""");
        foreign(results, datastore, "nested-single-value-for-list", HarnessNested.class, """
                {"profile": {"interests": "solo", "address": {"city": "c"}}, "name": "n", "home": null,
                 "extra": {"a": 1}}""");
        foreign(results, datastore, "renames-alternate-names", HarnessRenames.class, """
                {"_id": {"$oid": "650000000000000000000103"}, "n": "x", "oldEmail": "old@example.com",
                 "legacy": "loaded only"}""");
        foreign(results, datastore, "polymorphic-subtype-in-declared-field", HarnessDrawing.class, """
                {"main": {"_t": "dev.morphia.test.mapping.codec.harness.models.HarnessCircle", "label": "m",
                          "radius": 2.0},
                 "shapes": [{"label": "s", "side": 1.0,
                             "_t": "dev.morphia.test.mapping.codec.harness.models.HarnessSquare"}]}""");
        foreign(results, datastore, "constructor-reordered", HarnessConstructed.class, """
                {"size": 4, "name": "late", "_id": {"$oid": "650000000000000000000104"}}""");
        return results;
    }

    private <T> void sample(Map<String, Map<String, String>> results, MorphiaDatastore datastore, String name, Class<T> type,
            Supplier<T> sample) {
        Codec<T> codec = codec(datastore, type);
        byte[] encoded = encode(codec, sample.get(), COLLECTIBLE);
        Map<String, String> result = new LinkedHashMap<>();
        result.put("encoded", json(encoded));
        result.put("roundTrip", recording(() -> encode(codec, decode(codec, encoded), PLAIN)));
        results.put(name, result);
    }

    private <T> void encodeOnly(Map<String, Map<String, String>> results, MorphiaDatastore datastore, String name,
            Class<T> type, Supplier<T> sample) {
        results.put(name, Map.of("encoded", json(encode(codec(datastore, type), sample.get(), COLLECTIBLE))));
    }

    private <T> void foreign(Map<String, Map<String, String>> results, MorphiaDatastore datastore, String name, Class<T> type,
            String document) {
        Codec<T> codec = codec(datastore, type);
        BsonDocument input = BsonDocument.parse(document);
        Map<String, String> result = new LinkedHashMap<>();
        result.put("input", input.toJson(JSON));
        result.put("decoded", recording(() -> encode(codec, decode(codec, bytes(input)), PLAIN)));
        results.put("foreign/" + name, result);
    }

    /**
     * {@code CritterMapper} quietly falls back to a reflective model when it can't generate one, which would turn the
     * reflection-vs-critter comparison into reflection-vs-reflection. Every harness model must therefore be a critter
     * model, except the known fallbacks, which must still fall back so that this list can't go stale.
     */
    private static void assertCritterModels(MorphiaDatastore datastore) {
        Set<Class<?>> fallbacks = new HashSet<>();
        for (EntityModel model : datastore.getMapper().getMappedEntities()) {
            if (model.getType().getPackageName().equals(HarnessScalars.class.getPackageName())
                    && !(model instanceof CritterEntityModel)) {
                fallbacks.add(model.getType());
            }
        }
        assertEquals(CRITTER_FALLBACKS, fallbacks, "harness models the critter mapper maps reflectively");
    }

    /**
     * Records a failure as part of the result, so that a document Morphia rejects today is pinned like any other outcome.
     */
    private static String recording(Supplier<byte[]> action) {
        try {
            return json(action.get());
        } catch (RuntimeException e) {
            return new BsonDocument("error", new BsonString(e.getClass().getName())).toJson(JSON);
        }
    }

    private MorphiaDatastore datastore(Variant variant, MapperType mapper) {
        MorphiaConfig config = ManualMorphiaConfig.configure()
                .database("codec_harness")
                .mapper(mapper)
                .packages(List.of(HarnessScalars.class.getPackageName()))
                // CI's critter AOT job generates these models from META-INF/morphia-config.properties, and AOT models
                // keep the build-time discriminator whatever the runtime config says (game plan 4.12), so match it here
                .discriminator(DiscriminatorFunction.className())
                .applyIndexes(false)
                .applyCaps(false)
                .applyDocumentValidations(false)
                .storeNulls(variant.storeNulls)
                .storeEmpties(variant.storeEmpties);
        return new MorphiaDatastore(client, config);
    }

    @SuppressWarnings("unchecked")
    private static <T> Codec<T> codec(MorphiaDatastore datastore, Class<T> type) {
        return (Codec<T>) datastore.getCodecRegistry().get(type);
    }

    private static <T> byte[] encode(Codec<T> codec, T entity, EncoderContext context) {
        BasicOutputBuffer buffer = new BasicOutputBuffer();
        try (BsonBinaryWriter writer = new BsonBinaryWriter(buffer)) {
            codec.encode(writer, entity, context);
        }
        return buffer.toByteArray();
    }

    private static <T> T decode(Codec<T> codec, byte[] bytes) {
        try (BsonBinaryReader reader = new BsonBinaryReader(ByteBuffer.wrap(bytes))) {
            return codec.decode(reader, DECODER);
        }
    }

    private static byte[] bytes(BsonDocument document) {
        return encode(new BsonDocumentCodec(), document, PLAIN);
    }

    private static String json(byte[] bytes) {
        return new RawBsonDocument(bytes).toJson(JSON);
    }

    private static Map<String, Map<String, String>> read(Variant variant) {
        try (InputStream stream = CodecHarnessTest.class.getClassLoader().getResourceAsStream(RESOURCE_DIR + variant.file())) {
            if (stream == null) {
                return fail("missing fixture " + variant.file() + "; generate it with -Dmorphia.harness.update=true");
            }
            BsonDocument document = BsonDocument.parse(new String(stream.readAllBytes(), StandardCharsets.UTF_8));
            Map<String, Map<String, String>> fixtures = new LinkedHashMap<>();
            document.forEach((name, value) -> {
                Map<String, String> result = new LinkedHashMap<>();
                value.asDocument().forEach((key, recorded) -> result.put(key, recorded.asDocument().toJson(JSON)));
                fixtures.put(name, result);
            });
            return fixtures;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void write(Variant variant, Map<String, Map<String, String>> results) {
        BsonDocument document = new BsonDocument();
        results.forEach((name, result) -> {
            BsonDocument recorded = new BsonDocument();
            result.forEach((key, json) -> recorded.put(key, BsonDocument.parse(json)));
            document.put(name, recorded);
        });
        // surefire runs with the module directory as the working directory
        Path path = Path.of("src/test/resources", RESOURCE_DIR, variant.file());
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, document.toJson(JSON) + "\n");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static MongoClient offlineClient() {
        // only used to build the codec registry; it never sends a command
        return MongoClients.create(MongoClientSettings.builder()
                .applyToClusterSettings(cluster -> cluster.serverSelectionTimeout(1, TimeUnit.SECONDS))
                .applyToServerSettings(server -> server.heartbeatFrequency(1, TimeUnit.HOURS))
                .applyToSocketSettings(socket -> socket.connectTimeout(100, TimeUnit.MILLISECONDS))
                .build());
    }

    private enum Variant {
        DEFAULTS(false, false),
        STORE_NULLS(true, false),
        STORE_EMPTIES(false, true),
        STORE_NULLS_AND_EMPTIES(true, true);

        private final boolean storeNulls;
        private final boolean storeEmpties;

        Variant(boolean storeNulls, boolean storeEmpties) {
            this.storeNulls = storeNulls;
            this.storeEmpties = storeEmpties;
        }

        String file() {
            return name().toLowerCase().replace('_', '-') + ".json";
        }
    }
}
