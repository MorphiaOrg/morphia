package dev.morphia.aggregation.experimental.expressions;

import dev.morphia.aggregation.experimental.expressions.impls.Expression;

import java.util.List;

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
     * @mongodb.driver.manual manual/reference/operator/aggregation/allElementsTrue $allElementsTrue
     */
    public static Expression allElementsTrue(final Expression first, final Expression... additional) {
        return new Expression("$allElementsTrue", Expressions.toList(first, additional));
    }

    /**
     * Returns true if any elements of a set evaluate to true; otherwise, returns false. Accepts a single argument expression.
     *
     * @param first      an expression to evaluate
     * @param additional any additional expressions
     * @return the new expression
     * @mongodb.driver.manual manual/reference/operator/aggregation/anyElementTrue $anyElementTrue
     */
    public static Expression anyElementTrue(final Expression first, final Expression... additional) {
        return new Expression("$anyElementTrue", Expressions.toList(first, additional));
    }

    /**
     * Returns a set with elements that appear in the first set but not in the second set; i.e. performs a relative complement of the
     * second set relative to the first. Accepts exactly two argument expressions.
     *
     * @param first  the first array expression
     * @param second the second expression
     * @return the new expression
     * @mongodb.driver.manual manual/reference/operator/aggregation/setDifference $setDifference
     */
    public static Expression setDifference(final Expression first, final Expression second) {
        return new Expression("$setDifference", List.of(first, second));
    }

    /**
     * Returns true if the input sets have the same distinct elements. Accepts two or more argument expressions.
     *
     * @param first      the first array expression
     * @param additional additional expressions
     * @return the new expression
     * @mongodb.driver.manual manual/reference/operator/aggregation/setEquals $setEquals
     */
    public static Expression setEquals(final Expression first, final Expression... additional) {
        return new Expression("$setEquals", Expressions.toList(first, additional));
    }

    /**
     * Returns a set with elements that appear in all of the input sets. Accepts any number of argument expressions.
     *
     * @param first      the first array expression
     * @param additional additional expressions
     * @return the new expression
     * @mongodb.driver.manual manual/reference/operator/aggregation/setIntersection $setIntersection
     */
    public static Expression setIntersection(final Expression first, final Expression... additional) {
        return new Expression("$setIntersection", Expressions.toList(first, additional));
    }

    /**
     * Returns true if all elements of the first set appear in the second set, including when the first set equals the second set; i.e.
     * not a strict subset. Accepts exactly two argument expressions.
     *
     * @param first  the first array expression
     * @param second the second expression
     * @return the new expression
     * @mongodb.driver.manual manual/reference/operator/aggregation/setIsSubset $setIsSubset
     */
    public static Expression setIsSubset(final Expression first, final Expression second) {
        return new Expression("$setIsSubset", List.of(first, second));
    }

    /**
     * Returns a set with elements that appear in any of the input sets.
     *
     * @param first      the first array expression
     * @param additional additional expressions
     * @return the new expression
     * @mongodb.driver.manual manual/reference/operator/aggregation/setUnion $setUnion
     */
    public static Expression setUnion(final Expression first, final Expression... additional) {
        return new Expression("$setUnion", Expressions.toList(first, additional));
    }

}
