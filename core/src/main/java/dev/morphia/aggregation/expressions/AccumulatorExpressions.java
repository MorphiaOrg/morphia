package dev.morphia.aggregation.expressions;

import dev.morphia.aggregation.expressions.impls.Accumulator;
import dev.morphia.aggregation.expressions.impls.AccumulatorExpression;
import dev.morphia.aggregation.expressions.impls.EndResultsExpression;
import dev.morphia.aggregation.expressions.impls.Expression;
import dev.morphia.aggregation.expressions.impls.FunctionExpression;
import dev.morphia.aggregation.expressions.impls.NRankedResultsExpression;
import dev.morphia.aggregation.expressions.impls.Push;
import dev.morphia.aggregation.expressions.impls.RankedResultsExpression;
import dev.morphia.query.Sort;

import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;

/**
 * Defines helper methods for accumulator expressions
 *
 * @mongodb.driver.manual reference/operator/aggregation/#accumulators-group Accumulators
 * @since 2.0
 */
public final class AccumulatorExpressions {
    private AccumulatorExpressions() {
    }

    /**
     * Returns an array of unique expression values for each group. Order of the array elements is undefined.
     *
     * @param initFunction       used to initialize the state. The init function receives its arguments from the initArgs expression.
     * @param accumulateFunction used to accumulate documents. The accumulate function receives its arguments from the current
     *                           state and accumulateArgs array expression.
     * @param accumulateArgs     Arguments passed to the accumulate function.
     * @param mergeFunction      used to merge two internal states.
     * @return the new expression
     * @aggregation.expression $accumulator
     * @since 2.1
     */
    public static AccumulatorExpression accumulator(String initFunction,
            String accumulateFunction,
            List<Expression> accumulateArgs,
            String mergeFunction) {
        return new AccumulatorExpression(initFunction, accumulateFunction, accumulateArgs, mergeFunction);
    }

    /**
     * Returns an array of unique expression values for each group. Order of the array elements is undefined.
     *
     * @param value the value
     * @return the new expression
     * @aggregation.expression $addToSet
     */
    public static Expression addToSet(Expression value) {
        return new Expression("$addToSet", value);
    }

    /**
     * Returns an average of numerical values. Ignores non-numeric values.
     *
     * @param value      the value
     * @param additional any subsequent expressions to include in the expression
     * @return the new expression
     * @aggregation.expression $avg
     */
    public static Expression avg(Expression value, Expression... additional) {
        List<Expression> expressions = new ArrayList<>();
        expressions.add(value);
        expressions.addAll(asList(additional));
        return new Accumulator("$avg", expressions);
    }

    /**
     * Returns the bottom element within a group according to the specified sort order.
     *
     * @param output the expression listing the fields to use
     * @param sortBy the sort order
     * @return the expression
     * @aggregation.expression $bottom
     * @mongodb.server.release 5.2
     * @since 2.3
     */
    public static Expression bottom(Expression output, Sort... sortBy) {
        return new RankedResultsExpression("$bottom", output, sortBy);
    }

    /**
     * Returns an aggregation of the bottom n elements within a group, according to the specified sort order. If the group contains fewer
     * than n elements, $bottomN returns all elements in the group.
     *
     * @param n      the number of results per group and has to be a positive integral expression that is either a constant or depends on
     *               the
     *               _id value for $group
     * @param output the expression listing the fields to use
     * @param sortBy the sort order
     * @return the expression
     * @aggregation.expression $bottomN
     * @mongodb.server.release 5.2
     * @since 2.3
     */
    public static Expression bottomN(Expression n, Expression output, Sort... sortBy) {
        return new NRankedResultsExpression("$bottomN", n, output, sortBy);
    }

    /**
     * Returns a value from the first document for each group. Order is only defined if the documents are in a defined order.
     *
     * @param value the value
     * @return the new expression
     * @aggregation.expression $first
     */
    public static Expression first(Expression value) {
        return new Expression("$first", value);
    }

    /**
     * Returns an aggregation of the first n elements within a group. The elements returned are meaningful only if in a specified sort
     * order. If the group contains fewer than n elements, $firstN returns all elements in the group.
     *
     * @param n     the number of results per group and has to be a positive integral expression that is either a constant or depends on
     *              the _id value for $group
     * @param input the expression listing the fields to use
     * @return the expression
     * @aggregation.expression $firstN
     * @mongodb.server.release 5.2
     * @since 2.3
     */
    public static Expression firstN(Expression n, Expression input) {
        return new EndResultsExpression("$firstN", n, input);
    }

    /**
     * Defines a custom aggregation function or expression in JavaScript.
     *
     * @param body the function body
     * @param args the function arguments
     * @return the new expression
     * @aggregation.expression $function
     * @since 2.1
     */
    public static Expression function(String body, Expression... args) {
        return new FunctionExpression(body, asList(args));
    }

    /**
     * Returns a value from the last document for each group. Order is only defined if the documents are in a defined order.
     *
     * @param value the value
     * @return the new expression
     * @aggregation.expression $last
     */
    public static Expression last(Expression value) {
        return new Expression("$last", value);
    }

    /**
     * Returns an aggregation of the last n elements within a group. The elements returned are meaningful only if in a specified sort
     * order. If the group contains fewer than n elements, $lastN returns all elements in the group.
     *
     * @param n     the number of results per group and has to be a positive integral expression that is either a constant or depends on
     *              the _id value for $group
     * @param input the expression listing the fields to use
     * @return the expression
     * @aggregation.expression $lastN
     * @mongodb.server.release 5.2
     * @since 2.3
     */
    public static Expression lastN(Expression n, Expression input) {
        return new EndResultsExpression("$lastN", n, input);
    }

    /**
     * Returns the highest expression value for each group.
     *
     * @param value the value
     * @return the new expression
     * @aggregation.expression $max
     */
    public static Expression max(Expression value) {
        return new Expression("$max", value);
    }

    /**
     * Returns an aggregation of the max n elements within a group. The elements returned are meaningful only if in a specified sort
     * order. If the group contains fewer than n elements, $maxN returns all elements in the group.
     *
     * @param n     the number of results per group and n has to be a positive integral expression that is either a constant or depends on
     *              the _id value for $group.
     * @param input the expression that is the input to $maxN. It is evaluated for each element in the group and $maxN preserves the
     *              maximum n values.
     * @return the expression
     * @aggregation.expression $maxN
     * @mongodb.server.release 5.2
     * @since 2.3
     */
    public static Expression maxN(Expression n, Expression input) {
        return new EndResultsExpression("$maxN", n, input);
    }

    /**
     * Returns the lowest expression value for each group.
     *
     * @param value the value
     * @return the new expression
     * @aggregation.expression $min
     */
    public static Expression min(Expression value) {
        return new Expression("$min", value);
    }

    /**
     * Returns an aggregation of the min n elements within a group. The elements returned are meaningful only if in a specified sort
     * order. If the group contains fewer than n elements, $minN returns all elements in the group.
     *
     * @param n     the number of results per group and n has to be a positive integral expression that is either a constant or depends on
     *              the _id value for $group.
     * @param input the expression that is the input to $minN. It is evaluated for each element in the group and $minN preserves the
     *              minimum n values.
     * @return the expression
     * @aggregation.expression $minN
     * @mongodb.server.release 5.2
     * @since 2.3
     */
    public static Expression minN(Expression n, Expression input) {
        return new EndResultsExpression("$minN", n, input);
    }

    /**
     * Returns an array of expression values for each group.
     *
     * @param value the value
     * @return the new expression
     * @aggregation.expression $push
     */
    public static Expression push(Expression value) {
        return new Expression("$push", value);
    }

    /**
     * Returns an array of all values that result from applying an expression to each document in a group of documents that share the
     * same group by key.
     * <p>
     * $push is only available in the $group stage.
     *
     * @return the new expression
     * @aggregation.expression $push
     */
    public static Push push() {
        return new Push();
    }

    /**
     * Calculates and returns the sum of numeric values. $sum ignores non-numeric values.
     *
     * @param first      the first expression to sum
     * @param additional any subsequent expressions to include in the sum
     * @return the new expression
     * @aggregation.expression $sum
     */
    public static Expression sum(Expression first, Expression... additional) {
        List<Expression> expressions = new ArrayList<>();
        expressions.add(first);
        expressions.addAll(asList(additional));
        return new Accumulator("$sum", expressions);
    }

    /**
     * Returns the top element within a group according to the specified sort order.
     *
     * @param output the expression listing the fields to use
     * @param sortBy the sort order
     * @return the expression
     * @aggregation.expression $top
     * @mongodb.server.release 5.2
     * @since 2.3
     */
    public static Expression top(Expression output, Sort... sortBy) {
        return new RankedResultsExpression("$top", output, sortBy);
    }

    /**
     * Returns an aggregation of the top n elements within a group, according to the specified sort order. If the group contains fewer
     * than n elements, $topN returns all elements in the group.
     *
     * @param n      the number of results per group and has to be a positive integral expression that is either a constant or depends on
     *               the
     *               _id value for $group
     * @param output the expression listing the fields to use
     * @param sortBy the sort order
     * @return the expression
     * @aggregation.expression $topN
     * @mongodb.server.release 5.2
     * @since 2.3
     */
    public static Expression topN(Expression n, Expression output, Sort... sortBy) {
        return new NRankedResultsExpression("$topN", n, output, sortBy);
    }
}
