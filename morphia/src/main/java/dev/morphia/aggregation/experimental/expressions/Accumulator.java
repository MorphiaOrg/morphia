package dev.morphia.aggregation.experimental.expressions;

import dev.morphia.mapping.Mapper;
import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import java.util.ArrayList;
import java.util.List;

import static dev.morphia.aggregation.experimental.codecs.ExpressionCodec.writeUnnamedExpression;
import static java.util.Arrays.asList;

/**
 * Base class for the accumulator expression types.
 *
 * @mongodb.driver.manual reference/operator/aggregation/#accumulators-group Accumulator Expressions
 */
public class Accumulator extends Expression {
    private List<Expression> expressions = new ArrayList<>();

    protected Accumulator(final String operation, final List<Expression> values) {
        super(operation);
        expressions.addAll(values);
    }

    /**
     * Calculates and returns the sum of numeric values. $sum ignores non-numeric values.
     *
     * @param first      the first expression to sum
     * @param additional any subsequent expressions to include in the sum
     * @return the new expression
     * @mongodb.driver.manual reference/operator/aggregation/sum $sum
     */
    public static Accumulator sum(final Expression first, final Expression... additional) {
        List<Expression> expressions = new ArrayList<>();
        expressions.add(first);
        expressions.addAll(asList(additional));
        return new Accumulator("$sum", expressions);
    }

    /**
     * Adds numbers together or adds numbers and a date. If one of the arguments is a date, $add treats the other arguments as
     * milliseconds to add to the date.
     *
     * @param first      the first expression to sum
     * @param additional any subsequent expressions to include in the sum
     * @return the new expression
     * @mongodb.driver.manual reference/operator/aggregation/add $add
     */
    public static Accumulator add(final Expression first, final Expression... additional) {
        List<Expression> expressions = new ArrayList<>();
        expressions.add(first);
        expressions.addAll(asList(additional));
        return new Accumulator("$add", expressions);
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
