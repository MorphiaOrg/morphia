package dev.morphia.aggregation.expressions;

import dev.morphia.aggregation.expressions.impls.DocumentExpression;
import dev.morphia.aggregation.expressions.impls.Expression;
import dev.morphia.aggregation.expressions.impls.GetFieldExpression;
import dev.morphia.aggregation.expressions.impls.SetFieldExpression;
import dev.morphia.aggregation.expressions.impls.UnsetFieldExpression;
import dev.morphia.aggregation.expressions.impls.ValueExpression;
import dev.morphia.query.filters.Filter;

import static dev.morphia.aggregation.expressions.Expressions.wrap;

/**
 * Defines miscellaneous operators for aggregations.
 *
 * @since 2.2
 */
public final class Miscellaneous {
    private Miscellaneous() {
    }

    /**
     * Returns the value of a specified field from a document. If you don't specify an object, $getField returns the value of the field
     * from $$CURRENT.
     *
     * @param field the field name
     * @return the new expression
     * @aggregation.expression $getField
     * @mongodb.server.release 5.0
     * @since 2.3
     */
    public static GetFieldExpression getField(String field) {
        return getField(new ValueExpression(field));
    }

    /**
     * Returns the value of a specified field from a document. If you don't specify an object, $getField returns the value of the field
     * from $$CURRENT.
     *
     * @param field the expression yielding the field name
     * @return the new expression
     * @aggregation.expression $getField
     * @mongodb.server.release 5.0
     * @since 2.3
     */
    public static GetFieldExpression getField(Object field) {
        return new GetFieldExpression(wrap(field));
    }

    /**
     * Returns a random float between 0 and 1.
     *
     * @return the filter
     * @aggregation.expression $rand
     * @since 2.2
     */
    public static Expression rand() {
        return new Expression("$rand", new DocumentExpression());
    }

    /**
     * Matches a random selection of input documents. The number of documents selected approximates the sample rate expressed as a
     * percentage of the total number of documents.
     *
     * @param rate the rate to check against
     * @return the filter
     * @aggregation.expression $sampleRate
     * @since 2.2
     */
    public static Filter sampleRate(double rate) {
        return new SampleRateFilter(rate);
    }

    /**
     * Adds, updates, or removes a specified field in a document.
     *
     * @param field Field in the input object that you want to add, update, or remove. field can be any valid expression that resolves to
     *              a string constant.
     * @param input Document that contains the field that you want to add or update. input must resolve to an object, missing, null, or
     *              undefined.
     * @param value The value that you want to assign to field. value can be any valid expression. Set to $$REMOVE to remove field from
     *              the input document.
     * @return the new expression
     * @aggregation.expression $setField
     * @mongodb.server.release 5.0
     * @since 2.3
     */
    public static Expression setField(Object field, Object input, Object value) {
        return new SetFieldExpression(wrap(field), input, wrap(value));
    }

    /**
     * Removes a specified field in a document.
     *
     * @param field the expression yielding the field name
     * @param input Document that contains the field that you want to add or update. input must resolve to an object, missing, null, or
     *              undefined.
     * @return the new expression
     * @aggregation.expression $unsetField
     * @mongodb.server.release 5.0
     * @since 2.3
     */
    public static Expression unsetField(Object field, Object input) {
        return new UnsetFieldExpression(wrap(field), wrap(input));
    }

}
