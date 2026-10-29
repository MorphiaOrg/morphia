package dev.morphia.mapping.codec;

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;

import dev.morphia.MorphiaDatastore;
import dev.morphia.config.ManualMorphiaConfig;
import dev.morphia.config.MorphiaConfig;

import org.bson.BsonArray;
import org.bson.BsonBoolean;
import org.bson.BsonDecimal128;
import org.bson.BsonDocument;
import org.bson.BsonDocumentReader;
import org.bson.BsonDocumentWriter;
import org.bson.BsonDouble;
import org.bson.BsonInt32;
import org.bson.BsonInt64;
import org.bson.BsonNull;
import org.bson.BsonReader;
import org.bson.BsonString;
import org.bson.BsonType;
import org.bson.BsonValue;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecProvider;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.types.Decimal128;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * The primitive array codecs read and write exactly what the boxed path did: each element through the component
 * type's codec, collected into a list and copied into the array. Every BSON type an element might be stored as is
 * compared, including the ones that fail, which must fail with the same exception and message.
 */
public class TestPrimitiveArrayCodecs {
    private static final List<Class<?>> TYPES = List.of(boolean.class, Boolean.class, char.class, Character.class, double.class,
            Double.class, float.class, Float.class, int.class, Integer.class, long.class, Long.class, short.class, Short.class);

    private static final List<BsonValue> ELEMENTS = List.of(
            new BsonInt32(0), new BsonInt32(1), new BsonInt32(-1), new BsonInt32(Short.MAX_VALUE), new BsonInt32(Short.MIN_VALUE),
            new BsonInt32(Short.MAX_VALUE + 1), new BsonInt32(Short.MIN_VALUE - 1), new BsonInt32(Integer.MAX_VALUE),
            new BsonInt32(Integer.MIN_VALUE),
            new BsonInt64(0), new BsonInt64(5), new BsonInt64(Integer.MAX_VALUE + 1L), new BsonInt64(Long.MAX_VALUE),
            new BsonInt64(Long.MIN_VALUE), new BsonInt64((1L << 53) + 1),
            new BsonDouble(0.0), new BsonDouble(-0.0), new BsonDouble(1.5), new BsonDouble(3.0), new BsonDouble(-7.0),
            new BsonDouble(Double.NaN), new BsonDouble(Double.POSITIVE_INFINITY), new BsonDouble(Double.NEGATIVE_INFINITY),
            new BsonDouble(Double.MAX_VALUE), new BsonDouble(Double.MIN_VALUE), new BsonDouble(Float.MAX_VALUE),
            new BsonDouble(-Float.MAX_VALUE), new BsonDouble(Float.MAX_VALUE * 2.0), new BsonDouble(1e10), new BsonDouble(0.1),
            new BsonDecimal128(new Decimal128(1)), new BsonDecimal128(new Decimal128(new BigDecimal("1.5"))),
            new BsonDecimal128(new Decimal128(new BigDecimal("1e30"))),
            BsonNull.VALUE, new BsonString("a"), new BsonString("é"), new BsonString("ab"), new BsonString(""),
            BsonBoolean.TRUE, BsonBoolean.FALSE);

    @Test
    void decodesLikeTheBoxedPath() {
        withDatastore(ManualMorphiaConfig.configure(), datastore -> {
            for (Class<?> type : TYPES) {
                Codec<Object> arrays = arrayCodec(datastore, type);
                Codec<Object> elements = elementCodec(datastore, type);

                List<BsonValue> decodable = new ArrayList<>();
                for (BsonValue element : ELEMENTS) {
                    BsonArray array = new BsonArray(List.of(element));
                    Object expected = outcome(() -> referenceDecode(elements, type, array));
                    Object actual = outcome(() -> decode(arrays, array));
                    assertSame(expected, actual, type.getSimpleName() + " from " + element);
                    if (!(expected instanceof Failure)) {
                        decodable.add(element);
                    }
                }

                // every element that decodes on its own, repeated past the initial capacity, in one array
                List<BsonValue> many = new ArrayList<>();
                while (many.size() < 40) {
                    many.addAll(decodable);
                }
                for (int size : new int[] { 0, 1, 15, 16, 17, 32, many.size() }) {
                    BsonArray array = new BsonArray(many.subList(0, size));
                    assertSame(outcome(() -> referenceDecode(elements, type, array)), outcome(() -> decode(arrays, array)),
                            type.getSimpleName() + " array of " + size);
                }
            }
        });
    }

    @Test
    void encodesLikeTheBoxedPath() {
        withDatastore(ManualMorphiaConfig.configure(), datastore -> {
            Object[] samples = {
                    new boolean[] { true, false, true },
                    new Boolean[] { true, null, false },
                    new char[] { 'a', 'é', '\u0000', Character.MAX_VALUE },
                    new Character[] { 'a', null, 'é' },
                    new double[] { 0.0, -0.0, 1.5, Double.NaN, Double.POSITIVE_INFINITY, Double.MAX_VALUE, Double.MIN_VALUE },
                    new Double[] { 1.5, null, -0.0 },
                    new float[] { 0.0f, -0.0f, 0.1f, Float.NaN, Float.NEGATIVE_INFINITY, Float.MAX_VALUE, Float.MIN_VALUE },
                    new Float[] { 0.1f, null },
                    new int[] { 0, -1, Integer.MAX_VALUE, Integer.MIN_VALUE },
                    new Integer[] { 1, null, Integer.MIN_VALUE },
                    new long[] { 0L, -1L, Long.MAX_VALUE, Long.MIN_VALUE },
                    new Long[] { null, Long.MAX_VALUE },
                    new short[] { 0, -1, Short.MAX_VALUE, Short.MIN_VALUE },
                    new Short[] { Short.MIN_VALUE, null },
                    new double[0],
                    new int[40],
            };
            for (Object sample : samples) {
                Class<?> type = sample.getClass().getComponentType();
                BsonArray expected = referenceEncode(elementCodec(datastore, type), sample);
                Assertions.assertEquals(expected, encode(arrayCodec(datastore, type), sample), type.getSimpleName() + " array");
                // not every encoded array reads back: the component codecs reject nulls, and FloatCodec rejects infinities.
                // those fail just as they did; everything else round-trips
                Object readBack = outcome(() -> decode(arrayCodec(datastore, type), expected));
                Object reference = outcome(() -> referenceDecode(elementCodec(datastore, type), type, expected));
                assertSame(reference, readBack, type.getSimpleName() + " read back");
                if (!(reference instanceof Failure)) {
                    assertSame(sample, readBack, type.getSimpleName() + " round trip");
                }
            }
        });
    }

    @Test
    void replacedComponentCodecIsStillUsed() {
        CodecProvider provider = new CodecProvider() {
            @Override
            @SuppressWarnings("unchecked")
            public <T> Codec<T> get(Class<T> clazz, CodecRegistry registry) {
                return clazz == double.class || clazz == Double.class ? (Codec<T>) new TenthsCodec() : null;
            }
        };
        withDatastore(ManualMorphiaConfig.configure().codecProvider(provider), datastore -> {
            Codec<Object> arrays = arrayCodec(datastore, double.class);
            BsonArray encoded = new BsonArray(List.of(new BsonInt64(15), new BsonInt64(-5)));
            Assertions.assertEquals(encoded, encode(arrays, new double[] { 1.5, -0.5 }));
            Assertions.assertArrayEquals(new double[] { 1.5, -0.5 }, (double[]) decode(arrays, encoded));
        });
    }

    private static void withDatastore(MorphiaConfig config, java.util.function.Consumer<MorphiaDatastore> test) {
        try (MongoClient client = MongoClients.create("mongodb://localhost:1")) {
            test.accept(new MorphiaDatastore(client, config
                    .database("primitive_array_codecs")
                    .applyIndexes(false)
                    .applyCaps(false)
                    .applyDocumentValidations(false)));
        }
    }

    @SuppressWarnings("unchecked")
    private static Codec<Object> arrayCodec(MorphiaDatastore datastore, Class<?> type) {
        return (Codec<Object>) datastore.getCodecRegistry().get(Array.newInstance(type, 0).getClass());
    }

    @SuppressWarnings("unchecked")
    private static Codec<Object> elementCodec(MorphiaDatastore datastore, Class<?> type) {
        return (Codec<Object>) datastore.getCodecRegistry().get(type);
    }

    /**
     * What {@code TypedArrayCodec} did before the primitive codecs: decode each element with the component codec,
     * collect them in a list, and copy them in with {@code Array.set}.
     */
    private static Object referenceDecode(Codec<Object> elements, Class<?> type, BsonArray array) {
        BsonReader reader = reader(array);
        reader.readStartArray();
        List<Object> list = new ArrayList<>();
        while (reader.readBsonType() != BsonType.END_OF_DOCUMENT) {
            list.add(elements.decode(reader, DecoderContext.builder().build()));
        }
        reader.readEndArray();
        Object result = Array.newInstance(type, list.size());
        for (int i = 0; i < list.size(); i++) {
            Array.set(result, i, list.get(i));
        }
        return result;
    }

    private static BsonArray referenceEncode(Codec<Object> elements, Object value) {
        return write(writer -> {
            writer.writeStartArray();
            for (int i = 0; i < Array.getLength(value); i++) {
                Object element = Array.get(value, i);
                if (element == null) {
                    writer.writeNull();
                } else {
                    elements.encode(writer, element, EncoderContext.builder().build());
                }
            }
            writer.writeEndArray();
        });
    }

    private static Object decode(Codec<Object> codec, BsonArray array) {
        BsonReader reader = reader(array);
        return codec.decode(reader, DecoderContext.builder().build());
    }

    private static BsonArray encode(Codec<Object> codec, Object value) {
        return write(writer -> codec.encode(writer, value, EncoderContext.builder().build()));
    }

    private static BsonReader reader(BsonArray array) {
        BsonDocumentReader reader = new BsonDocumentReader(new BsonDocument("value", array));
        reader.readStartDocument();
        reader.readName();
        return reader;
    }

    private static BsonArray write(java.util.function.Consumer<BsonWriter> body) {
        BsonDocument document = new BsonDocument();
        BsonDocumentWriter writer = new BsonDocumentWriter(document);
        writer.writeStartDocument();
        writer.writeName("value");
        body.accept(writer);
        writer.writeEndDocument();
        return document.getArray("value");
    }

    private static Object outcome(java.util.function.Supplier<Object> action) {
        try {
            return action.get();
        } catch (RuntimeException e) {
            return new Failure(e.getClass(), e.getMessage());
        }
    }

    private static void assertSame(Object expected, Object actual, String message) {
        Assertions.assertTrue(Objects.deepEquals(expected, actual),
                () -> message + ": expected " + describe(expected) + " but was " + describe(actual));
    }

    private static String describe(Object value) {
        return value != null && value.getClass().isArray() ? java.util.Arrays.asList(boxed(value)).toString() : String.valueOf(value);
    }

    private static Object[] boxed(Object array) {
        Object[] boxed = new Object[Array.getLength(array)];
        for (int i = 0; i < boxed.length; i++) {
            boxed[i] = Array.get(array, i);
        }
        return boxed;
    }

    private record Failure(Class<?> type, String message) {
    }

    /**
     * Stores a double as a count of tenths, so a codec that bypasses it is obvious.
     */
    private static class TenthsCodec implements Codec<Double> {
        @Override
        public Double decode(BsonReader reader, DecoderContext decoderContext) {
            return reader.readInt64() / 10.0;
        }

        @Override
        public void encode(BsonWriter writer, Double value, EncoderContext encoderContext) {
            writer.writeInt64(Math.round(value * 10));
        }

        @Override
        public Class<Double> getEncoderClass() {
            return Double.class;
        }
    }
}
