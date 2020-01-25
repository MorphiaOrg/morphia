package dev.morphia.aggregation.experimental.expressions;

import dev.morphia.aggregation.experimental.expressions.impls.DocumentExpression;
import dev.morphia.aggregation.experimental.expressions.impls.Expression;
import dev.morphia.aggregation.experimental.expressions.impls.Literal;
import dev.morphia.aggregation.experimental.expressions.impls.Push;
import dev.morphia.aggregation.experimental.expressions.impls.ValueExpression;
import dev.morphia.mapping.Mapper;
import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;

/**
 * Defines helper methods for various expressions.
 *
 * @mongodb.driver.manual reference/operator/aggregation/ Aggregation pipeline operators
 * @since 2.0
 */
public final class Expressions {
    private Expressions() {
    }

    /**
     * Creates a field expression for the given value.  If the value does not already start with '$', it will be prepended automatically.
     *
     * @param name the field name
     * @return the new expression
     */
    public static Expression field(final String name) {
        return new ValueExpression(name.startsWith("$") ? name : "$" + name);
    }

    /**
     * Returns a value without parsing. Note that this is different from {@link #literal(Object)} in that the given value will dropped
     * directly in to the pipeline for use/evaluation in whatever context the value is used.
     *
     * @param value the value
     * @return the new expression
     */
    public static Expression value(final Object value) {
        return new ValueExpression(value);
    }

    /**
     * Returns a value without parsing. Use for values that the aggregation pipeline may interpret as an expression.
     *
     * @param value the value
     * @return the new expression
     * @mongodb.driver.manual reference/operator/aggregation/literal $literal
     */
    public static Expression literal(final Object value) {
        return new Literal(value);
    }

    /**
     * Creates a null expression
     *
     * @return the new expression
     */
    public static Expression nullExpression() {
        return new Expression(null) {
            @Override
            public void encode(final Mapper mapper, final BsonWriter writer, final EncoderContext encoderContext) {
                writer.writeNull();
            }
        };
    }

    /**
     * Returns an array of all values that result from applying an expression to each document in a group of documents that share the
     * same group by key.
     * <p>
     * $push is only available in the $group stage.
     *
     * @return the new expression
     * @mongodb.driver.manual reference/operator/aggregation/push $push
     */
    public static Push push() {
        return new Push();
    }

    /**
     * Creates a new DocumentExpression.
     *
     * @return the new expression
     */
    public static DocumentExpression of() {
        return new DocumentExpression();
    }

    /**
     * @param first
     * @param additional
     * @return
     * @morphia.internal
     */
    static List<Expression> toList(final Expression first, final Expression[] additional) {
        List<Expression> expressions = new ArrayList<>(asList(first));
        expressions.addAll(asList(additional));
        return expressions;
    }
}
