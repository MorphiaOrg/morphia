package dev.morphia.aggregation.experimental.codecs;

import dev.morphia.aggregation.experimental.expressions.Expression;
import dev.morphia.mapping.Mapper;
import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecRegistry;

public class ExpressionCodec<T extends Expression> implements Codec<T> {
    private final Mapper mapper;

    public ExpressionCodec(final Mapper mapper) {
        this.mapper = mapper;
    }

    /**
     * @param mapper
     * @param writer
     * @param name
     * @param expression
     * @param encoderContext
     * @morphia.internal
     */
    public static void writeNamedExpression(final Mapper mapper, final BsonWriter writer, final String name, final Expression expression,
                                            final EncoderContext encoderContext) {
        if (expression != null) {
            writer.writeName(name);
            expression.encode(mapper, writer, encoderContext);
        }
    }

    /**
     *
     * @param mapper
     * @param writer
     * @param name
     * @param value
     * @param encoderContext
     * @morphia.internal
     */
    public static void writeNamedValue(final Mapper mapper, final BsonWriter writer, final String name, final Object value,
                                   final EncoderContext encoderContext) {
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
    public static void writeUnnamedExpression(final Mapper mapper, final BsonWriter writer, final Expression expression,
                                              final EncoderContext encoderContext) {
        if (expression != null) {
            expression.encode(mapper, writer, encoderContext);
        }
    }

    @Override
    public final T decode(final BsonReader reader, final DecoderContext decoderContext) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void encode(final BsonWriter writer, final T expression, final EncoderContext encoderContext) {
        if(expression != null) {
            expression.encode(mapper, writer, encoderContext);
        } else {
            writer.writeNull();
        }
    }

    @Override
    public final Class<T> getEncoderClass() {
        return (Class<T>) Expression.class;
    }

    protected CodecRegistry getCodecRegistry() {
        return mapper.getCodecRegistry();
    }

    protected Mapper getMapper() {
        return mapper;
    }
}
