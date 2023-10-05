package dev.morphia.aggregation.expressions;

import java.util.ArrayList;
import java.util.List;

import com.mongodb.lang.Nullable;

import dev.morphia.aggregation.expressions.impls.DocumentExpression;
import dev.morphia.aggregation.expressions.impls.Expression;
import dev.morphia.aggregation.expressions.impls.LiteralExpression;
import dev.morphia.aggregation.expressions.impls.MetaExpression;
import dev.morphia.aggregation.expressions.impls.ValueExpression;
import dev.morphia.annotations.internal.MorphiaInternal;

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
     * Creates a new DocumentExpression.
     *
     * @return the new expression
     * @since 2.3
     */
    public static DocumentExpression document() {
        return new DocumentExpression();
    }

    /**
     * Creates a new DocumentExpression.
     *
     * @param name       the first field name
     * @param expression the first field value
     * @return the new expression
     * @since 2.3
     */
    public static DocumentExpression document(String name, Expression expression) {
        return new DocumentExpression()
                .field(name, expression);
    }

    /**
     * Creates a field expression for the given value. If the value does not already start with '$', it will be prepended automatically.
     *
     * @param name the field name
     * @return the new expression
     */
    public static Expression field(String name) {
        return new ValueExpression(name.startsWith("$") ? name : "$" + name);
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
     * @param first      the first item
     * @param additional additional items
     * @param <T>        the element type
     * @return a list of them all
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public static <T> List<T> toList(T first, T... additional) {
        List<T> expressions = new ArrayList<>();
        expressions.add(first);
        expressions.addAll(asList(additional));
        return expressions;
    }

    /**
     * Returns a value without parsing. Note that this is different from {@link #literal(Object)} in that the given value will dropped
     * directly in to the pipeline for use/evaluation in whatever context the value is used.
     *
     * @param value the value
     * @return the new expression
     */
    public static ValueExpression value(@Nullable Object value) {
        return new ValueExpression(value);
    }
}
