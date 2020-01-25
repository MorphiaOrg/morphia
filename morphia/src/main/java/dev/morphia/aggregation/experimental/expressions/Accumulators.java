package dev.morphia.aggregation.experimental.expressions;

import dev.morphia.aggregation.experimental.expressions.impls.Push;
import dev.morphia.mapping.Mapper;
import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import java.util.ArrayList;
import java.util.List;

import static dev.morphia.aggregation.experimental.codecs.ExpressionCodec.writeUnnamedExpression;
import static java.util.Arrays.asList;

/**
 * Defines helper methods for accumulator expressions
 *
 * @mongodb.driver.manual reference/operator/aggregation/#accumulators-group Accumulators
 * @since 2.0
 */
public final class Accumulators {
    private Accumulators() {
    }

    /**
     * Calculates and returns the sum of numeric values. $sum ignores non-numeric values.
     *
     * @param first      the first expression to sum
     * @param additional any subsequent expressions to include in the sum
     * @return the new expression
     * @mongodb.driver.manual reference/operator/aggregation/sum $sum
     */
    public static Expression sum(final Expression first, final Expression... additional) {
        List<Expression> expressions = new ArrayList<>();
        expressions.add(first);
        expressions.addAll(asList(additional));
        return new Accumulator("$sum", expressions);
    }

    /**
     * Returns an array of unique expression values for each group. Order of the array elements is undefined.
     *
     * @param value the value
     * @return the new expression
     * @mongodb.driver.manual reference/operator/aggregation/addToSet $addToSet
     */
    public static Expression addToSet(final Expression value) {
        return new Expression("$addToSet", value);
    }

    /**
     * Returns an average of numerical values. Ignores non-numeric values.
     *
     * @param value      the value
     * @param additional any subsequent expressions to include in the expression
     * @return the new expression
     * @mongodb.driver.manual reference/operator/aggregation/avg $avg
     */
    public static Expression avg(final Expression value, final Expression... additional) {
        List<Expression> expressions = new ArrayList<>();
        expressions.add(value);
        expressions.addAll(asList(additional));
        return new Accumulator("$avg", expressions);
    }

    /**
     * Returns a value from the first document for each group. Order is only defined if the documents are in a defined order.
     *
     * @param value the value
     * @return the new expression
     * @mongodb.driver.manual reference/operator/aggregation/first $first
     */
    public static Expression first(final Expression value) {
        return new Expression("$first", value);
    }

    /**
     * Returns a value from the last document for each group. Order is only defined if the documents are in a defined order.
     *
     * @param value the value
     * @return the new expression
     * @mongodb.driver.manual reference/operator/aggregation/last $last
     */
    public static Expression last(final Expression value) {
        return new Expression("$last", value);
    }

    /**
     * Returns the highest expression value for each group.
     *
     * @param value the value
     * @return the new expression
     * @mongodb.driver.manual reference/operator/aggregation/max $max
     */
    public static Expression max(final Expression value) {
        return new Expression("$max", value);
    }

    /**
     * Returns the lowest expression value for each group.
     *
     * @param value the value
     * @return the new expression
     * @mongodb.driver.manual reference/operator/aggregation/min $min
     */
    public static Expression min(final Expression value) {
        return new Expression("$min", value);
    }

    /**
     * Returns an array of expression values for each group.
     *
     * @param value the value
     * @return the new expression
     * @mongodb.driver.manual reference/operator/aggregation/push $push
     */
    public static Expression push(final Expression value) {
        return new Expression("$push", value);
    }

    /**
     * Returns an array of all values that result from applying an expression to each document in a group of documents that share the
     * same group by key.
     * <p>
     * $push is only available in the $group stage.
     *
     * @return the new expression
     * @mongodb.driver.manual reference/operator/aggregation/push $push
     */
    public static Push push() {
        return new Push();
    }

    /**
     * Returns the population standard deviation of the input values.
     *
     * @param value      the value
     * @param additional any subsequent expressions to include in the expression
     * @return the new expression
     * @mongodb.driver.manual reference/operator/aggregation/stdDevPop $stdDevPop
     */
    public static Expression stdDevPop(final Expression value, final Expression... additional) {
        List<Expression> expressions = new ArrayList<>();
        expressions.add(value);
        expressions.addAll(asList(additional));
        return new Accumulator("$stdDevPop", expressions);
    }

    /**
     * Returns the sample standard deviation of the input values.
     *
     * @param value      the value
     * @param additional any subsequent expressions to include in the expression
     * @return the new expression
     * @mongodb.driver.manual reference/operator/aggregation/stdDevSamp $stdDevSamp
     */
    public static Expression stdDevSamp(final Expression value, final Expression... additional) {
        List<Expression> expressions = new ArrayList<>();
        expressions.add(value);
        expressions.addAll(asList(additional));
        return new Accumulator("$stdDevSamp", expressions);
    }

    /**
     * Base class for the accumulator expression types.
     */
    public static class Accumulator extends Expression {
        private List<Expression> expressions = new ArrayList<>();

        protected Accumulator(final String operation, final List<Expression> values) {
            super(operation);
            expressions.addAll(values);
        }


        @Override
        public void encode(final Mapper mapper, final BsonWriter writer, final EncoderContext encoderContext) {
            writer.writeStartDocument();
            writer.writeName(getOperation());
            if (expressions.size() > 1) {
                writer.writeStartArray();
            }
            for (final Expression expression : expressions) {
                writeUnnamedExpression(mapper, writer, expression, encoderContext);
            }
            if (expressions.size() > 1) {
                writer.writeEndArray();
            }
            writer.writeEndDocument();
        }
    }
}

