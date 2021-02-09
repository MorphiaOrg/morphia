package dev.morphia.aggregation.experimental.expressions.impls;

import com.mongodb.lang.Nullable;
import dev.morphia.mapping.Mapper;
import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import java.util.StringJoiner;

import static dev.morphia.aggregation.experimental.codecs.ExpressionHelper.document;
import static dev.morphia.aggregation.experimental.codecs.ExpressionHelper.value;

/**
 * Base class for all the expression types.
 *
 * @mongodb.driver.manual reference/operator/aggregation/ Expressions
 * @since 2.0
 */
public class Expression {
    private final String operation;
    private final Object value;

    protected Expression(String operation) {
        this.operation = operation;
        this.value = null;
    }

    /**
     * @param operation
     * @param value
     * @morphia.internal
     */
    public Expression(String operation, @Nullable Object value) {
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
    public void encode(Mapper mapper, BsonWriter writer, EncoderContext encoderContext) {
        document(writer, () -> value(mapper, writer, operation, value, encoderContext));
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
