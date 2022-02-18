package dev.morphia.aggregation.experimental.expressions;

import dev.morphia.Datastore;
import dev.morphia.aggregation.experimental.expressions.impls.Expression;
import dev.morphia.aggregation.experimental.expressions.impls.ExpressionList;
import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import java.util.List;

/**
 * Defines helper methods for the comparison expressions
 *
 * @mongodb.driver.manual reference/operator/aggregation/#comparison-expression-operators Comparison Expressions
 * @since 2.0
 */
public final class ComparisonExpressions {
    private ComparisonExpressions() {
    }

    /**
     * Returns 0 if the two values are equivalent, 1 if the first value is greater than the second, and -1 if the first value is less than
     * the second.
     *
     * @param first  an expression for the value to compare
     * @param second an expression yielding the value to check against
     * @return the new expression
     * @aggregation.expression $cmp
     */
    public static Expression cmp(Expression first, Expression second) {
        return new Expression("$cmp", List.of(first, second));
    }

    /**
     * Returns true if the values are equivalent.
     *
     * @param first  an expression for the value to compare
     * @param second an expression yielding the value to check against
     * @return the new expression
     * @aggregation.expression $eq
     */
    public static Expression eq(Expression first, Expression second) {
        return new Expression("$eq", List.of(first, second));
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
     * @aggregation.expression $gt
     */
    public static Expression gt(Expression first, Expression second) {
        return new Expression("$gt", new ExpressionList(first, second)) {
            @Override
            public void encode(Datastore datastore, BsonWriter writer, EncoderContext encoderContext) {
                writer.writeName(getOperation());
                getValue().encode(datastore, writer, encoderContext);
            }
        };
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
     * @aggregation.expression $gte
     */
    public static Expression gte(Expression first, Expression second) {
        return new Expression("$gte", List.of(first, second));
    }

    /**
     * Returns true if the first value is less than the second.
     *
     * @param first  an expression for the value to compare
     * @param second an expression yielding the value to check against
     * @return the new expression
     * @aggregation.expression $lt
     */
    public static Expression lt(Expression first, Expression second) {
        return new Expression("$lt", List.of(first, second));
    }

    /**
     * Compares two values and returns:
     *
     * <li>true when the first value is less than or equivalent to the second value.
     * <li>false when the first value is greater than the second value.
     *
     * @param first  an expression for the value to compare
     * @param second an expression yielding the value to check against
     * @return the new expression
     * @aggregation.expression $lte
     */
    public static Expression lte(Expression first, Expression second) {
        return new Expression("$lte", List.of(first, second));
    }

    /**
     * Returns true if the values are not equivalent.
     *
     * @param first  an expression for the value to compare
     * @param second an expression yielding the value to check against
     * @return the new expression
     * @aggregation.expression $ne
     */
    public static Expression ne(Expression first, Expression second) {
        return new Expression("$ne", List.of(first, second));
    }

}
