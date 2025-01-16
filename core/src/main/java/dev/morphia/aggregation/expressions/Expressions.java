package dev.morphia.aggregation.expressions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import dev.morphia.aggregation.expressions.impls.DocumentExpression;
import dev.morphia.aggregation.expressions.impls.Expression;
import dev.morphia.aggregation.expressions.impls.FilterExpression;
import dev.morphia.aggregation.expressions.impls.LiteralExpression;
import dev.morphia.aggregation.expressions.impls.MetaExpression;
import dev.morphia.aggregation.expressions.impls.ValueExpression;
import dev.morphia.annotations.internal.MorphiaInternal;

import static dev.morphia.aggregation.expressions.Expressions.wrap;
import static dev.morphia.aggregation.expressions.MetadataKeyword.*;
import static dev.morphia.mapping.codec.CodecHelper.coalesce;
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
    public static DocumentExpression document(String name, Object expression) {
        return new DocumentExpression()
                .field(name, expression);
    }

    /**
     * @param input An expression that resolves to an array.
     * @param cond  An expression that resolves to a boolean value used to determine if an element should be included in the output array.
     *              The expression references each element of the input array individually with the variable name specified in as.
     *
     * @return
     * @aggregation.expression $filter
     */
    public static FilterExpression filter(Object input, Object cond) {
        return new FilterExpression(wrap(input), wrap(cond));
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
        return meta(TEXTSCORE);
    }

    /**
     * Returns the metadata associated with a document in a pipeline operations, e.g. "textScore" when performing text search.
     *
     * @param metadataKeyword the keyword to use
     * @return the new expression
     * @aggregation.expression $meta
     * @since 3.0
     * @mongodb.server.release 4.4
     */
    public static MetaExpression meta(MetadataKeyword metadataKeyword) {
        return new MetaExpression(metadataKeyword);
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
     * @param elements the list items
     * @param <T>      the element type
     * @return a list of them all
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public static <T> List<T> toList(T... elements) {
        return new ArrayList<>(asList(elements));
    }

    /**
     * @hidden
     * @param value
     * @return
     */
    public static Expression wrap(Object value) {
        if (value instanceof Expression expression) {
            return expression;
        }
        if (value instanceof List<?> list) {
            throw new UnsupportedOperationException();
        } else {
            return new ValueExpression(value);
        }
    }

    /**
     * @param values
     * @return
     * @hidden
     */
    public static List<Expression> wrap(List<Object> values) {
        return values.stream()
                .map(Expressions::wrap)
                .collect(Collectors.toList());
    }

    /**
     * @param value
     * @return
     * @hidden
     */
    public static List<Expression> wrap(Object... value) {
        return Arrays.stream(value)
                .map(Expressions::wrap)
                .collect(Collectors.toList());
    }

    /**
     * @param value
     * @return
     * @hidden
     */
    public static List<Expression> wrap(Object first, Object... value) {
        return coalesce(first, value).stream()
                .map(Expressions::wrap)
                .collect(Collectors.toList());
    }
}
