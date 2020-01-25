package dev.morphia.aggregation.experimental.expressions.impls;

import dev.morphia.mapping.Mapper;
import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import java.util.ArrayList;
import java.util.List;

import static dev.morphia.aggregation.experimental.codecs.ExpressionCodec.writeUnnamedExpression;

/**
 * Base class for the accumulator expression types.
 *
 * @since 2.0
 */
public class Accumulator extends Expression {
    private List<Expression> expressions = new ArrayList<>();

    /**
     * @param operation
     * @param values
     * @morphia.internal
     */
    public Accumulator(final String operation, final List<Expression> values) {
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
