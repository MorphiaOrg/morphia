package dev.morphia.aggregation.experimental.expressions.impls;

import dev.morphia.mapping.Mapper;
import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import java.util.ArrayList;
import java.util.List;

import static dev.morphia.aggregation.experimental.codecs.ExpressionHelper.expression;

/**
 * Base class for the accumulator expression types.
 *
 * @since 2.0
 */
public class Accumulator extends Expression {
    private final List<Expression> expressions = new ArrayList<>();

    /**
     * @param operation
     * @param values
     * @morphia.internal
     */
    public Accumulator(String operation, List<Expression> values) {
        super(operation);
        expressions.addAll(values);
    }

    @Override
    public void encode(Mapper mapper, BsonWriter writer, EncoderContext encoderContext) {
        writer.writeStartDocument();
        writer.writeName(getOperation());
        if (expressions.size() > 1) {
            writer.writeStartArray();
        }
        for (Expression expression : expressions) {
            expression(mapper, writer, expression, encoderContext);
        }
        if (expressions.size() > 1) {
            writer.writeEndArray();
        }
        writer.writeEndDocument();
    }
}
