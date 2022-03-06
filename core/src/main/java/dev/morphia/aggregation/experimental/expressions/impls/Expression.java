package dev.morphia.aggregation.experimental.expressions.impls;

import com.mongodb.lang.Nullable;
import dev.morphia.Datastore;
import dev.morphia.aggregation.experimental.codecs.ExpressionHelper;
import dev.morphia.annotations.internal.MorphiaInternal;
import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import java.util.List;
import java.util.StringJoiner;

/**
 * Base class for all the expression types.
 *
 * @mongodb.driver.manual reference/operator/aggregation/ Expressions
 * @since 2.0
 */
public class Expression {
    private final String operation;
    private final Expression value;

    public Expression(String operation) {
        this.operation = operation;
        this.value = null;
    }

    /**
     * @param operation the expression name
     * @param value     the value
     * @morphia.internal
     */
    public Expression(String operation, Expression value) {
        this.operation = operation;
        this.value = value;
    }

    /**
     * @param operation the expression name
     * @param value     the value
     * @morphia.internal
     */
    public Expression(String operation, List<Expression> value) {
        this.operation = operation;
        this.value = new ExpressionList(value);
    }

    /**
     * @param datastore      the datastore
     * @param writer         the writer
     * @param encoderContext the context
     * @morphia.internal
     */
    @MorphiaInternal
    public void encode(Datastore datastore, BsonWriter writer, EncoderContext encoderContext) {
        ExpressionHelper.expression(datastore, writer, operation, value, encoderContext);
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
    @Nullable
    public Expression getValue() {
        return value;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", Expression.class.getSimpleName() + "[", "]")
                   .add("operation='" + operation + "'")
                   .toString();
    }
}
