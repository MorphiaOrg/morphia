package dev.morphia.aggregation.expressions;

import dev.morphia.aggregation.expressions.impls.Expression;

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
     * @aggregation.expression $allElementsTrue
     */
    public static Expression allElementsTrue(Expression first, Expression... additional) {
        return new Expression("$allElementsTrue", Expressions.toList(first, additional));
    }

    /**
     * Returns true if any elements of a set evaluate to true; otherwise, returns false. Accepts a single argument expression.
     *
     * @param first      an expression to evaluate
     * @param additional any additional expressions
     * @return the new expression
     * @aggregation.expression $anyElementTrue
     */
    public static Expression anyElementTrue(Expression first, Expression... additional) {
        return new Expression("$anyElementTrue", Expressions.toList(first, additional));
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
    public static Expression setDifference(Expression first, Expression second) {
        return new Expression("$setDifference", List.of(first, second));
    }

    /**
     * Returns true if the input sets have the same distinct elements. Accepts two or more argument expressions.
     *
     * @param first      the first array expression
     * @param additional additional expressions
     * @return the new expression
     * @aggregation.expression $setEquals
     */
    public static Expression setEquals(Expression first, Expression... additional) {
        return new Expression("$setEquals", Expressions.toList(first, additional));
    }

    /**
     * Returns a set with elements that appear in all of the input sets. Accepts any number of argument expressions.
     *
     * @param first      the first array expression
     * @param additional additional expressions
     * @return the new expression
     * @aggregation.expression $setIntersection
     */
    public static Expression setIntersection(Expression first, Expression... additional) {
        return new Expression("$setIntersection", Expressions.toList(first, additional));
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
    public static Expression setIsSubset(Expression first, Expression second) {
        return new Expression("$setIsSubset", List.of(first, second));
    }

    /**
     * Returns a set with elements that appear in any of the input sets.
     *
     * @param first      the first array expression
     * @param additional additional expressions
     * @return the new expression
     * @aggregation.expression $setUnion
     */
    public static Expression setUnion(Expression first, Expression... additional) {
        return new Expression("$setUnion", Expressions.toList(first, additional));
    }

}
