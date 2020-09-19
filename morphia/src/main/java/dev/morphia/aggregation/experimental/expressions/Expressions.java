package dev.morphia.aggregation.experimental.expressions;

import dev.morphia.aggregation.experimental.expressions.impls.DocumentExpression;
import dev.morphia.aggregation.experimental.expressions.impls.Expression;
import dev.morphia.aggregation.experimental.expressions.impls.LiteralExpression;
import dev.morphia.aggregation.experimental.expressions.impls.MetaExpression;
import dev.morphia.aggregation.experimental.expressions.impls.ValueExpression;

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
    public static Expression field(String name) {
        return new ValueExpression(name.startsWith("$") ? name : "$" + name);
    }

    /**
     * Returns a value without parsing. Note that this is different from {@link #literal(Object)} in that the given value will dropped
     * directly in to the pipeline for use/evaluation in whatever context the value is used.
     *
     * @param value the value
     * @return the new expression
     */
    public static Expression value(Object value) {
        return new ValueExpression(value);
    }

    /**
     * Returns a value without parsing. Use for values that the aggregation pipeline may interpret as an expression.
     *
     * @param value the value
     * @return the new expression
     * @aggregation.expression $literal
     */
    public static Expression literal(Object value) {
        return new LiteralExpression(value);
    }

    /**
     * Returns the metadata associated with a document in a pipeline operations, e.g. "textScore" when performing text search.
     *
     * @return the new expression
     * @aggregation.expression $meta
     */
    public static Expression meta() {
        return new MetaExpression();
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
    static List<Expression> toList(Expression first, Expression[] additional) {
        List<Expression> expressions = new ArrayList<>(asList(first));
        expressions.addAll(asList(additional));
        return expressions;
    }
}
