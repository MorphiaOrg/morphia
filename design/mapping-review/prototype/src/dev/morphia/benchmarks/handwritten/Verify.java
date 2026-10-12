package dev.morphia.benchmarks.handwritten;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import com.mongodb.client.MongoClient;

import dev.morphia.benchmarks.Fixtures;
import dev.morphia.benchmarks.MapperVariant;
import dev.morphia.benchmarks.Models;
import dev.morphia.benchmarks.models.Address;
import dev.morphia.benchmarks.models.NestedEntity;
import dev.morphia.benchmarks.models.Profile;
import dev.morphia.benchmarks.models.SimpleEntity;
import dev.morphia.config.MorphiaConfig;
import dev.morphia.mapping.Mapper;

import org.bson.BsonBinaryReader;
import org.bson.BsonBinaryWriter;
import org.bson.BsonDocument;
import org.bson.BsonDouble;
import org.bson.BsonInt32;
import org.bson.BsonInt64;
import org.bson.BsonNull;
import org.bson.BsonString;
import org.bson.RawBsonDocument;
import org.bson.codecs.BsonDocumentCodec;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.io.BasicOutputBuffer;
import org.bson.types.ObjectId;

/**
 * Checks that the prototype codecs produce byte-for-byte the BSON Morphia's codec produces, and that decoding is
 * interchangeable in both directions. Usage: {@code Verify [variant ...]} (default: reflection critter-runtime). Run the
 * {@code critter} variant with the critter-aot jar ahead of the benchmarks jar on the classpath.
 */
public final class Verify {
    private static final EncoderContext COLLECTIBLE = EncoderContext.builder().isEncodingCollectibleDocument(true).build();
    private static final EncoderContext PLAIN = EncoderContext.builder().build();
    private static final DecoderContext DECODE = DecoderContext.builder().build();
    private static int checks;
    private static final List<String> failures = new ArrayList<>();

    public static void main(String[] args) {
        List<String> variants = args.length > 0 ? List.of(args) : List.of("reflection", "critter-runtime");
        for (String variant : variants) {
            for (boolean storeNulls : new boolean[] { false, true }) {
                verify(variant, storeNulls);
            }
        }
        System.out.printf("%d checks, %d failures%n", checks, failures.size());
        failures.forEach(f -> System.out.println("FAIL: " + f));
        System.exit(failures.isEmpty() ? 0 : 1);
    }

    private static void verify(String variantName, boolean storeNulls) {
        MapperVariant variant = MapperVariant.of(variantName);
        MorphiaConfig config = Fixtures.config(variant).storeNulls(storeNulls);
        Mapper mapper = Fixtures.mappedMapper(variant, config);
        try (MongoClient client = Fixtures.offlineClient()) {
            CodecRegistry registry = Fixtures.codecRegistry(client, config, mapper);
            String label = variantName + (storeNulls ? "/storeNulls" : "");
            System.out.printf("[%s] tiers: %s%n", label, MapperVariant.describeTiers(mapper));
            for (Models model : List.of(Models.SIMPLE, Models.NESTED)) {
                @SuppressWarnings("unchecked")
                Codec<Object> morphia = (Codec<Object>) registry.get(model.type());
                Codec<Object> hand = Codecs.handwritten(model, registry, mapper);
                Codec<Object> flat = Codecs.flat(model, registry, mapper);
                for (Object entity : samples(model)) {
                    String what = label + " " + model + " " + describe(morphia, entity);
                    // Morphia first: it generates and sets the id, so all three then see the same entity state.
                    byte[] expected = encode(morphia, entity, COLLECTIBLE);
                    check(what + " hand encode", expected, encode(hand, entity, COLLECTIBLE));
                    check(what + " flat encode", expected, encode(flat, entity, COLLECTIBLE));
                    roundTrips(what, morphia, expected, Function.identity(), hand, flat);
                }
                // Documents Morphia never writes but must still read: shuffled order, unknown fields, other numeric
                // types, explicit nulls, missing discriminator.
                for (BsonDocument doc : foreignDocuments(model)) {
                    byte[] bytes = toBytes(doc);
                    String what = label + " " + model + " foreign " + doc.toJson();
                    Object viaMorphia = decode(morphia, bytes);
                    byte[] expected = encode(morphia, viaMorphia, PLAIN);
                    check(what + " hand decode", expected, encode(morphia, decode(hand, bytes), PLAIN));
                    check(what + " flat decode (lenient numbers not expected)", expected, safeFlat(morphia, flat, bytes));
                }
                // id generation when encoding a collectible document
                Object fresh = model == Models.SIMPLE ? new SimpleEntity() : new NestedEntity();
                encode(hand, fresh, COLLECTIBLE);
                check(label + " " + model + " hand generates _id", true, mapper.getId(fresh) instanceof ObjectId);
            }
        }
    }

    private static byte[] safeFlat(Codec<Object> morphia, Codec<Object> flat, byte[] bytes) {
        try {
            return encode(morphia, decode(flat, bytes), PLAIN);
        } catch (RuntimeException e) {
            return ("flat codec threw " + e).getBytes();
        }
    }

    private static void roundTrips(String what, Codec<Object> morphia, byte[] expected, Function<Object, Object> unused,
            Codec<Object> hand, Codec<Object> flat) {
        // Compare against Morphia's own decode -> encode, which is not always the identity (e.g. an empty list skipped
        // by storeEmpties=false decodes as null and is then written as null under storeNulls=true).
        byte[] morphiaRoundTrip = encode(morphia, decode(morphia, expected), COLLECTIBLE);
        check(what + " hand decode -> morphia encode", morphiaRoundTrip, encode(morphia, decode(hand, expected), COLLECTIBLE));
        check(what + " flat decode -> morphia encode", morphiaRoundTrip, encode(morphia, decode(flat, expected), COLLECTIBLE));
        check(what + " morphia decode -> hand encode", morphiaRoundTrip, encode(hand, decode(morphia, expected), COLLECTIBLE));
        check(what + " hand decode -> hand encode", morphiaRoundTrip, encode(hand, decode(hand, expected), COLLECTIBLE));
    }

    private static List<Object> samples(Models model) {
        List<Object> list = new ArrayList<>();
        list.add(model.sample());
        if (model == Models.SIMPLE) {
            list.add(new SimpleEntity()); // nulls everywhere but the primitives
        } else {
            NestedEntity nested = new NestedEntity(); // only name null-free fields missing
            list.add(nested);
            NestedEntity partial = NestedEntity.sample();
            set(partial, "home", null);
            set(partial, "profile", new Profile(null, new Address(null, "x", null, null), new ArrayList<>()));
            list.add(partial);
        }
        return list;
    }

    private static List<BsonDocument> foreignDocuments(Models model) {
        List<BsonDocument> docs = new ArrayList<>();
        if (model == Models.SIMPLE) {
            docs.add(new BsonDocument()
                    .append("score", new BsonInt32(7)) // int32 into a double
                    .append("unknown", new BsonString("ignored"))
                    .append("age", new BsonInt64(5)) // int64 into an int
                    .append("count", new BsonInt32(9)) // int32 into a long
                    .append("name", BsonNull.VALUE)
                    .append("status", new BsonString("CLOSED"))
                    .append("_id", new org.bson.BsonObjectId(new ObjectId())));
            docs.add(new BsonDocument("_t", new BsonString("SimpleEntity")).append("age", new BsonDouble(3.0)));
        } else {
            docs.add(new BsonDocument()
                    .append("profile", new BsonDocument("interests", new BsonString("solo")) // single value -> list
                            .append("address", new BsonDocument("city", new BsonString("c"))))
                    .append("name", new BsonString("n"))
                    .append("home", BsonNull.VALUE)
                    .append("extra", new BsonDocument("a", new BsonInt32(1))));
        }
        return docs;
    }

    private static void set(Object target, String field, Object value) {
        try {
            var f = target.getClass().getDeclaredField(field);
            f.setAccessible(true);
            f.set(target, value);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String describe(Codec<Object> morphia, Object entity) {
        return new RawBsonDocument(encode(morphia, entity, PLAIN)).toJson().replaceAll("\\{\"\\$oid\": \"[0-9a-f]+\"}", "oid");
    }

    static byte[] encode(Codec<Object> codec, Object entity, EncoderContext context) {
        BasicOutputBuffer buffer = new BasicOutputBuffer(512);
        try (BsonBinaryWriter writer = new BsonBinaryWriter(buffer)) {
            codec.encode(writer, entity, context);
        }
        return buffer.toByteArray();
    }

    static Object decode(Codec<Object> codec, byte[] bytes) {
        try (BsonBinaryReader reader = new BsonBinaryReader(ByteBuffer.wrap(bytes))) {
            return codec.decode(reader, DECODE);
        }
    }

    private static byte[] toBytes(BsonDocument doc) {
        BasicOutputBuffer buffer = new BasicOutputBuffer();
        try (BsonBinaryWriter writer = new BsonBinaryWriter(buffer)) {
            new BsonDocumentCodec().encode(writer, doc, PLAIN);
        }
        return buffer.toByteArray();
    }

    private static void check(String what, byte[] expected, byte[] actual) {
        checks++;
        if (!Arrays.equals(expected, actual)) {
            failures.add(what + "\n   expected " + json(expected) + "\n   actual   " + json(actual));
        }
    }

    private static void check(String what, boolean expected, boolean actual) {
        checks++;
        if (expected != actual) {
            failures.add(what);
        }
    }

    private static String json(byte[] bytes) {
        try {
            return new RawBsonDocument(bytes).toJson();
        } catch (RuntimeException e) {
            return new String(bytes);
        }
    }
}
