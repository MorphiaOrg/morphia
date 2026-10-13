package dev.morphia.benchmarks.handwritten;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.codec.Conversions;

import org.bson.BsonReader;
import org.bson.BsonType;
import org.bson.codecs.BooleanCodec;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.DoubleCodec;
import org.bson.codecs.IntegerCodec;
import org.bson.codecs.LongCodec;
import org.bson.codecs.configuration.CodecRegistry;

/**
 * The small, shared runtime library a generated per-entity codec would call into. Everything here is either a fast path
 * that a generator would inline anyway, or a cold slow path that keeps Morphia's current decode semantics (numeric
 * widening/narrowing via the driver's number codecs, and the "decode as Object then {@link Conversions#convert}" fallback
 * that {@code EntityDecoder#decodeModel} uses when a value has an unexpected BSON type).
 */
public final class CodecSupport {
    private static final IntegerCodec INTEGER = new IntegerCodec();
    private static final LongCodec LONG = new LongCodec();
    private static final DoubleCodec DOUBLE = new DoubleCodec();
    private static final BooleanCodec BOOLEAN = new BooleanCodec();

    /** Per-datastore context a generated codec needs; one instance is shared by all generated codecs of a datastore. */
    public static final class Context {
        final CodecRegistry registry;
        final Conversions conversions;
        final Mapper mapper;
        final boolean storeNulls;
        final boolean storeEmpties;

        public Context(CodecRegistry registry, Mapper mapper) {
            this.registry = registry;
            this.mapper = mapper;
            this.conversions = mapper.getConversions();
            this.storeNulls = mapper.getConfig().storeNulls();
            this.storeEmpties = mapper.getConfig().storeEmpties();
        }
    }

    private CodecSupport() {
    }

    static VarHandle field(Class<?> owner, String name, Class<?> type) {
        try {
            return MethodHandles.privateLookupIn(owner, MethodHandles.lookup()).findVarHandle(owner, name, type);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    static final org.bson.codecs.EncoderContext CHILD_ENCODER = org.bson.codecs.EncoderContext.builder().build();

    /** {@code List<String>} the way {@code MorphiaCollectionCodec} reads it: an array, or a single value wrapped in a list. */
    static java.util.List<String> readStringList(Context context, BsonReader reader, DecoderContext ctx) {
        java.util.List<String> list = new java.util.ArrayList<>();
        if (reader.getCurrentBsonType() != BsonType.ARRAY) {
            list.add(readStringValue(context, reader, ctx));
            return list;
        }
        reader.readStartArray();
        while (reader.readBsonType() != BsonType.END_OF_DOCUMENT) {
            if (reader.getCurrentBsonType() == BsonType.NULL) {
                reader.readNull();
                list.add(null);
            } else {
                list.add(readStringValue(context, reader, ctx));
            }
        }
        reader.readEndArray();
        return list;
    }

    static String readStringValue(Context context, BsonReader reader, DecoderContext ctx) {
        return reader.getCurrentBsonType() == BsonType.STRING ? reader.readString()
                : (String) slowRead(context, reader, ctx, String.class);
    }

    static void writeStringList(org.bson.BsonWriter writer, java.util.List<String> list) {
        writer.writeStartArray();
        for (String value : list) {
            if (value == null) {
                writer.writeNull();
            } else {
                writer.writeString(value);
            }
        }
        writer.writeEndArray();
    }

    // Numeric BSON types go through the driver codec, which widens and narrows like Morphia's property codecs. Anything
    // else (schema drift such as a string) takes the same decode-as-Object + Conversions fallback EntityDecoder uses.

    static int readInt(Context context, BsonReader reader, DecoderContext ctx) {
        BsonType type = reader.getCurrentBsonType();
        return type == BsonType.INT32 ? reader.readInt32()
                : isNumber(type) ? INTEGER.decode(reader, ctx) : (Integer) slowRead(context, reader, ctx, int.class);
    }

    static long readLong(Context context, BsonReader reader, DecoderContext ctx) {
        BsonType type = reader.getCurrentBsonType();
        return type == BsonType.INT64 ? reader.readInt64()
                : isNumber(type) ? LONG.decode(reader, ctx) : (Long) slowRead(context, reader, ctx, long.class);
    }

    static double readDouble(Context context, BsonReader reader, DecoderContext ctx) {
        BsonType type = reader.getCurrentBsonType();
        return type == BsonType.DOUBLE ? reader.readDouble()
                : isNumber(type) ? DOUBLE.decode(reader, ctx) : (Double) slowRead(context, reader, ctx, double.class);
    }

    static boolean readBoolean(Context context, BsonReader reader, DecoderContext ctx) {
        return reader.getCurrentBsonType() == BsonType.BOOLEAN ? reader.readBoolean()
                : (Boolean) slowRead(context, reader, ctx, boolean.class);
    }

    private static boolean isNumber(BsonType type) {
        return type == BsonType.INT32 || type == BsonType.INT64 || type == BsonType.DOUBLE || type == BsonType.DECIMAL128;
    }

    /**
     * Mirrors the cold path of {@code EntityDecoder#decodeModel}: decode whatever is there as an Object and convert it.
     */
    static Object slowRead(Context context, BsonReader reader, DecoderContext ctx, Class<?> target) {
        Object value = context.registry.get(Object.class).decode(reader, ctx);
        return context.conversions.convert(value, target);
    }

    /**
     * A subtype's document showed up where this codec's type was expected: hand the whole document to the subtype's
     * codec, as {@code EntityDecoder#getCodecFromDocument} does today.
     */
    @SuppressWarnings("unchecked")
    static <T> Codec<T> subtypeCodec(Context context, String discriminator) {
        return (Codec<T>) context.registry.get(context.mapper.getDiscriminatorLookup().lookup(discriminator));
    }
}
