package dev.morphia.mapping.codec;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.mongodb.lang.NonNull;
import com.mongodb.lang.Nullable;

import dev.morphia.MorphiaDatastore;
import dev.morphia.aggregation.expressions.impls.Expression;
import dev.morphia.aggregation.expressions.impls.SingleValuedExpression;
import dev.morphia.annotations.internal.MorphiaInternal;

import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecRegistry;

import static java.util.Arrays.asList;

public class CodecHelper {
    public static void array(BsonWriter writer, String name, Runnable body) {
        writer.writeStartArray(name);
        body.run();
        writer.writeEndArray();
    }

    public static void array(BsonWriter writer, Runnable body) {
        writer.writeStartArray();
        body.run();
        writer.writeEndArray();
    }

    public static void array(CodecRegistry codecRegistry, BsonWriter writer, String name, @Nullable List<Expression> list,
            EncoderContext encoderContext) {
        if (list != null) {
            array(writer, name, () -> {
                for (Expression expression : list) {
                    encodeIfNotNull(codecRegistry, writer, expression, encoderContext);
                }
            });
        }
    }

    @NonNull
    public static <T> List<T> coalesce(T first, T[] updates) {
        List<T> operators = new ArrayList<>();
        operators.add(first);
        operators.addAll(asList(updates));
        return operators;
    }

    public static void document(BsonWriter writer, String name, Runnable body) {
        writer.writeStartDocument(name);
        body.run();
        writer.writeEndDocument();
    }

    public static void document(BsonWriter writer, Runnable body) {
        writer.writeStartDocument();
        body.run();
        writer.writeEndDocument();
    }

    @MorphiaInternal
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static boolean encodeIfNotNull(CodecRegistry registry,
            BsonWriter writer,
            @Nullable Expression expression,
            EncoderContext encoderContext) {
        if (expression != null) {
            Codec codec = registry.get(expression.getClass());
            if (expression instanceof SingleValuedExpression) {
                codec.encode(writer, expression, encoderContext);
            } else {
                document(writer, () -> {
                    codec.encode(writer, expression, encoderContext);
                });
            }
            return true;
        }
        return false;
    }

    @MorphiaInternal
    public static boolean encodeIfNotNull(CodecRegistry registry, BsonWriter writer, String name, @Nullable Expression expression,
            EncoderContext encoderContext) {
        if (expression != null) {
            writer.writeName(name);
            encodeIfNotNull(registry, writer, expression, encoderContext);
            return true;
        }
        return false;
    }

    public static void value(BsonWriter writer, String name, @Nullable Boolean value) {
        if (value != null) {
            writer.writeBoolean(name, value);
        }
    }

    public static void value(BsonWriter writer, String name, @Nullable Double value) {
        if (value != null) {
            writer.writeDouble(name, value);
        }
    }

    public static void value(BsonWriter writer, String name, @Nullable Integer value) {
        if (value != null) {
            writer.writeInt32(name, value);
        }
    }

    public static void value(BsonWriter writer, String name, @Nullable Long value) {
        if (value != null) {
            writer.writeInt64(name, value);
        }
    }

    public static void value(BsonWriter writer, String name, @Nullable String value) {
        if (value != null) {
            writer.writeString(name, value);
        }
    }

    public static void value(BsonWriter writer, String name, @Nullable Enum<?> value) {
        if (value != null) {
            writer.writeString(name, value.name().toLowerCase(Locale.ROOT));
        }
    }

    @MorphiaInternal
    public static void value(CodecRegistry codecRegistry, BsonWriter writer, String name, @Nullable Object value,
            EncoderContext encoderContext) {
        if (value != null) {
            writer.writeName(name);
            Codec codec = codecRegistry.get(value.getClass());
            encoderContext.encodeWithChildContext(codec, writer, value);
        }
    }

    /**
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public static void namedValue(BsonWriter writer, MorphiaDatastore datastore, @Nullable String name, @Nullable Object value,
            EncoderContext encoderContext) {
        writer.writeName(name);
        if (value != null) {
            Codec codec = datastore.getCodecRegistry().get(value.getClass());
            encoderContext.encodeWithChildContext(codec, writer, value);
        } else {
            writer.writeNull();
        }
    }

    /**
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public static void unnamedValue(BsonWriter writer, MorphiaDatastore datastore, @Nullable Object value,
            EncoderContext encoderContext) {
        if (value != null) {
            Codec codec = datastore.getCodecRegistry().get(value.getClass());
            encoderContext.encodeWithChildContext(codec, writer, value);
        } else {
            writer.writeNull();
        }
    }
}
