package dev.morphia.aggregation.experimental.codecs;

import dev.morphia.aggregation.experimental.expressions.impls.Expression;
import dev.morphia.mapping.Mapper;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.EncoderContext;

/**
 * @morphia.internal
 * @since 2.1
 */
public final class ExpressionHelper {
    private ExpressionHelper() {
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

    /**
     * @param mapper
     * @param writer
     * @param name
     * @param expression
     * @param encoderContext
     * @morphia.internal
     */
    public static void writeNamedExpression(Mapper mapper, BsonWriter writer, String name, Expression expression,
                                            EncoderContext encoderContext) {
        if (expression != null) {
            writer.writeName(name);
            expression.encode(mapper, writer, encoderContext);
        }
    }

    /**
     * @param mapper
     * @param writer
     * @param name
     * @param value
     * @param encoderContext
     * @morphia.internal
     */
    public static void writeNamedValue(Mapper mapper, BsonWriter writer, String name, Object value,
                                       EncoderContext encoderContext) {
        if (value != null) {
            writer.writeName(name);
            Codec codec = mapper.getCodecRegistry().get(value.getClass());
            encoderContext.encodeWithChildContext(codec, writer, value);
        }
    }

    /**
     * @param mapper
     * @param writer
     * @param expression
     * @param encoderContext
     * @morphia.internal
     */
    public static void writeUnnamedExpression(Mapper mapper, BsonWriter writer, Expression expression,
                                              EncoderContext encoderContext) {
        if (expression != null) {
            expression.encode(mapper, writer, encoderContext);
        }
    }

    /**
     * @param mapper
     * @param writer
     * @param value
     * @param encoderContext
     * @morphia.internal
     */
    public static void writeUnnamedValue(Mapper mapper, BsonWriter writer, Object value,
                                         EncoderContext encoderContext) {
        if (value != null) {
            Codec codec = mapper.getCodecRegistry().get(value.getClass());
            encoderContext.encodeWithChildContext(codec, writer, value);
        }
    }
}
