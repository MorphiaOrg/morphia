package dev.morphia.aggregation.expressions;

import java.util.List;

import dev.morphia.aggregation.expressions.impls.Expression;

import static dev.morphia.aggregation.expressions.Expressions.wrap;

/**
 * Defines helper methods for the set expressions
 *
 * @mongodb.driver.manual reference/operator/aggregation/#set-expression-operators Set Expressions
 * @since 2.0
 */
public final class SetExpressions {
    private SetExpressions() {
    }

    /**
     * Returns true if no element of a set evaluates to false, otherwise, returns false. Accepts a single argument expression.
     *
     * @param first      an expression to evaluate
     * @param additional any additional expressions
     * @return the new expression
     * @aggregation.expression $allElementsTrue
     */
    public static Expression allElementsTrue(Object first, Object... additional) {
        return new Expression("$allElementsTrue", wrap(first, additional));
    }

    /**
     * eturns true if any elements of a set evaluate to true; otherwise, returns false. Accepts a single argument expression.
     *
     * @param first      an expression to evaluate
     * @param additional any additional expressions
     * @return the new expression
     * @aggregation.expression $anyElementTrue
     */
    public static Expression anyElementTrue(Object first, Object... additional) {
        return new Expression("$anyElementTrue", wrap(first, additional));
    }

    /**
     * Returns a set with elements that appear in the first set but not in the second set; i.e. performs a relative complement of the
     * second set relative to the first. Accepts exactly two argument expressions.
     *
     * @param first  the first array expression
     * @param second the second expression
     * @return the new expression
     * @aggregation.expression $setDifference
     */
    public static Expression setDifference(Object first, Object second) {
        return new Expression("$setDifference", wrap(List.of(first, second)));
    }

    /**
     * Returns true if the input sets have the same distinct elements. Accepts two or more argument expressions.
     *
     * @param first      the first array expression
     * @param additional additional expressions
     * @return the new expression
     * @aggregation.expression $setEquals
     */
    public static Expression setEquals(Object first, Object... additional) {
        return new Expression("$setEquals", wrap(first, additional));
    }

    /**
     * Returns a set with elements that appear in all of the input sets. Accepts any number of argument expressions.
     *
     * @param first      the first array expression
     * @param additional additional expressions
     * @return the new expression
     * @aggregation.expression $setIntersection
     */
    public static Expression setIntersection(Object first, Object... additional) {
        return new Expression("$setIntersection", wrap(first, additional));
    }

    /**
     * Returns true if all elements of the first set appear in the second set, including when the first set equals the second set; i.e.
     * not a strict subset. Accepts exactly two argument expressions.
     *
     * @param first  the first array expression
     * @param second the second expression
     * @return the new expression
     * @aggregation.expression $setIsSubset
     */
    public static Expression setIsSubset(Object first, Object second) {
        return new Expression("$setIsSubset", wrap(List.of(first, second)));
    }

    /**
     * Returns a set with elements that appear in any of the input sets.
     *
     * @param first      the first array expression
     * @param additional additional expressions
     * @return the new expression
     * @aggregation.expression $setUnion
     */
    public static Expression setUnion(Object first, Object... additional) {
        return new Expression("$setUnion", wrap(first, additional));
    }

}
