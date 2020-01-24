package dev.morphia.aggregation.experimental.expressions;

import java.util.List;

/**
 * Base class for the comparison expressions
 *
 * @mongodb.driver.manual reference/operator/aggregation/#comparison-expression-operators Comparison Expressions
 */
public class Comparison extends Expression {

    protected Comparison(final String operation, final Object value) {
        super(operation, value);
    }

    /**
     * Compares two values and returns:
     *
     * <li>true when the first value is greater than the second value.
     * <li>false when the first value is less than or equivalent to the second value.
     *
     * @param first  an expression for the value to compare
     * @param second an expression yielding the value to check against
     * @return the new expression
     * @mongodb.driver.manual manual/reference/operator/aggregation/gt $gt
     */
    public static Comparison gt(final Expression first, final Expression second) {
        return new Comparison("$gt", List.of(first, second));
    }

    /**
     * Compares two values and returns:
     *
     * <li>true when the first value is greater than or equivalent to the second value.
     * <li>false when the first value is less than the second value.
     *
     * @param first  an expression for the value to compare
     * @param second an expression yielding the value to check against
     * @return the new expression
     * @mongodb.driver.manual manual/reference/operator/aggregation/gte $gte
     */
    public static Comparison gte(final Expression first, final Expression second) {
        return new Comparison("$gte", List.of(first, second));
    }
}
