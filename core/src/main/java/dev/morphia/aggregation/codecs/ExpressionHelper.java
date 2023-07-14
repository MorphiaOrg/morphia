package dev.morphia.aggregation.codecs;

import java.util.List;
import java.util.function.Consumer;

import com.mongodb.lang.Nullable;

import dev.morphia.Datastore;
import dev.morphia.aggregation.expressions.impls.ArrayLiteral;
import dev.morphia.aggregation.expressions.impls.Expression;
import dev.morphia.aggregation.expressions.impls.ExpressionList;
import dev.morphia.aggregation.expressions.impls.SingleValuedExpression;
import dev.morphia.aggregation.expressions.impls.ValueExpression;
import dev.morphia.annotations.internal.MorphiaInternal;
import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.codec.writer.DocumentWriter;
import dev.morphia.query.Sort;

import org.bson.BsonWriter;
import org.bson.Document;
import org.bson.codecs.Codec;
import org.bson.codecs.EncoderContext;

/**
 * @morphia.internal
 * @since 2.1
 */
@MorphiaInternal
public final class ExpressionHelper {
    private ExpressionHelper() {
    }

    public static void array(BsonWriter writer, Runnable body) {
        writer.writeStartArray();
        body.run();
        writer.writeEndArray();
    }

    public static void array(Datastore datastore, BsonWriter writer, String name, @Nullable List<Expression> list,
            EncoderContext encoderContext) {
        if (list != null) {
            array(writer, name, () -> {
                for (Expression expression : list) {
                    wrapExpression(datastore, writer, expression, encoderContext);
                }
            });
        }
    }

    public static void array(BsonWriter writer, String name, Runnable body) {
        writer.writeStartArray(name);
        body.run();
        writer.writeEndArray();
    }

    /**
     * @param datastore
     * @param writer
     * @param expression
     * @param encoderContext
     * @morphia.internal
     * @since 2.3
     */
    @MorphiaInternal
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static void wrapExpression(Datastore datastore, BsonWriter writer, @Nullable Expression expression,
            EncoderContext encoderContext) {
        if (expression != null) {
            if (expression instanceof SingleValuedExpression) {
                expression.encode(datastore, writer, encoderContext);
            } else {
                document(writer, () -> {
                    expression.encode(datastore, writer, encoderContext);
                });
            }
        }
    }

    public static void document(BsonWriter writer, Runnable body) {
        writer.writeStartDocument();
        body.run();
        writer.writeEndDocument();
    }

    public static void document(BsonWriter writer, String name, Runnable body) {
        writer.writeStartDocument(name);
        body.run();
        writer.writeEndDocument();
    }

    public static Document document(Mapper mapper, Document seed, Consumer<BsonWriter> body) {
        DocumentWriter writer = new DocumentWriter(mapper.getConfig(), seed);
        writer.writeStartDocument();
        body.accept(writer);
        writer.writeEndDocument();

        return writer.getDocument();
    }

    public static void encode(BsonWriter writer, Sort sort) {
        document(writer, () -> {
            writer.writeInt64(sort.getField(), sort.getOrder());
        });
    }

    /**
     * @param datastore
     * @param writer
     * @param name
     * @param expression
     * @param encoderContext
     * @morphia.internal
     */
    @MorphiaInternal
    public static void expression(Datastore datastore, BsonWriter writer, String name, @Nullable Expression expression,
            EncoderContext encoderContext) {
        if (expression != null) {
            writer.writeName(name);
            wrapExpression(datastore, writer, expression, encoderContext);
        }
    }

    /**
     * @param datastore
     * @param writer
     * @param expression
     * @param encoderContext
     * @morphia.internal
     */
    @MorphiaInternal
    public static void expression(Datastore datastore, BsonWriter writer, @Nullable Expression expression, EncoderContext encoderContext) {
        if (expression != null) {
            expression.encode(datastore, writer, encoderContext);
        }
    }

    /**
     * @param datastore
     * @param writer
     * @param name
     * @param value
     * @param encoderContext
     * @morphia.internal
     */
    @MorphiaInternal
    public static void value(Datastore datastore, BsonWriter writer, String name, @Nullable Object value, EncoderContext encoderContext) {
        if (value != null) {
            if (value instanceof List) {
                List<Object> list = (List<Object>) value;
                array(writer, name, () -> {
                    for (Object object : list) {
                        if (object != null) {
                            Codec codec = datastore.getCodecRegistry().get(object.getClass());
                            encoderContext.encodeWithChildContext(codec, writer, object);
                        } else {
                            writer.writeNull();
                        }
                    }
                });
            } else {
                writer.writeName(name);
                Codec codec = datastore.getCodecRegistry().get(value.getClass());
                encoderContext.encodeWithChildContext(codec, writer, value);
            }
        }
    }

    public static void value(BsonWriter writer, String name, @Nullable Boolean value) {
        if (value != null) {
            writer.writeBoolean(name, value);
        }
    }

    public static void value(BsonWriter writer, String name, @Nullable Integer value) {
        if (value != null) {
            writer.writeInt32(name, value);
        }
    }

    public static void value(BsonWriter writer, String name, @Nullable Double value) {
        if (value != null) {
            writer.writeDouble(name, value);
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

    /**
     * @param datastore
     * @param writer
     * @param value
     * @param encoderContext
     * @morphia.internal
     */
    @MorphiaInternal
    public static void value(Datastore datastore, BsonWriter writer, @Nullable Object value, EncoderContext encoderContext) {
        if (value != null) {
            if (value instanceof Expression) {
                ((Expression) value).encode(datastore, writer, encoderContext);
            } else {
                Codec codec = datastore.getCodecRegistry().get(value.getClass());
                encoderContext.encodeWithChildContext(codec, writer, value);
            }
        }
    }

    /**
     * @param datastore
     * @param writer
     * @param name
     * @param expression
     * @param encoderContext
     * @morphia.internal
     * @since 2.3
     */
    @MorphiaInternal
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static void wrapExpression(Datastore datastore, BsonWriter writer, String name, @Nullable Expression expression,
            EncoderContext encoderContext) {
        if (expression != null) {
            writer.writeName(name);
            if (expression instanceof ValueExpression || expression instanceof ArrayLiteral || expression instanceof ExpressionList) {
                expression.encode(datastore, writer, encoderContext);
            } else {
                wrapExpression(datastore, writer, expression, encoderContext);
            }
        }
    }
}
