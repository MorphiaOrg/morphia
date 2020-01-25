package dev.morphia.aggregation.experimental.expressions.impls;

import dev.morphia.mapping.Mapper;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.EncoderContext;

import java.util.StringJoiner;

/**
 * Base class for all the expression types.
 *
 * @mongodb.driver.manual reference/operator/aggregation/ Expressions
 * @since 2.0
 */
public class Expression {
    private final String operation;
    private final Object value;

    protected Expression(final String operation) {
        this.operation = operation;
        this.value = null;
    }

    /**
     * @param operation
     * @param value
     * @morphia.internal
     */
    public Expression(final String operation, final Object value) {
        this.operation = operation;
        this.value = value;
    }

    /**
     * @param mapper         the mapper
     * @param writer         the writer
     * @param encoderContext the context
     * @morphia.internal
     */
    @SuppressWarnings("rawtypes")
    public void encode(final Mapper mapper, final BsonWriter writer, final EncoderContext encoderContext) {
        writer.writeStartDocument();
        writer.writeName(operation);
        Codec codec = mapper.getCodecRegistry().get(value.getClass());
        encoderContext.encodeWithChildContext(codec, writer, value);
        writer.writeEndDocument();
    }

    /**
     * @return the value
     * @morphia.internal
     */
    public String getOperation() {
        return operation;
    }

    /**
     * @return the value
     * @morphia.internal
     */
    public Object getValue() {
        return value;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", Expression.class.getSimpleName() + "[", "]")
                   .add("operation='" + operation + "'")
                   .toString();
    }
}
