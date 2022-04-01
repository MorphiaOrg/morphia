package dev.morphia.aggregation.expressions.impls;

import dev.morphia.Datastore;
import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import java.util.List;

import static dev.morphia.aggregation.codecs.ExpressionHelper.wrapExpression;

/**
 * Base class for the accumulator expression types.
 *
 * @since 2.0
 */
public class Accumulator extends Expression {

    /**
     * @param operation
     * @param values
     * @morphia.internal
     */
    public Accumulator(String operation, List<Expression> values) {
        super(operation, new ExpressionList(values));
    }

    @Override
    public void encode(Datastore datastore, BsonWriter writer, EncoderContext encoderContext) {
        writer.writeName(getOperation());
        ExpressionList values = getValue();
        if (values != null) {
            List<Expression> list = values.getValues();
            if (list.size() > 1) {
                writer.writeStartArray();
            }
            for (Expression expression : list) {
                wrapExpression(datastore, writer, expression, encoderContext);
            }
            if (list.size() > 1) {
                writer.writeEndArray();
            }
        } else {
            writer.writeNull();
        }
    }

    @Override
    public ExpressionList getValue() {
        return (ExpressionList) super.getValue();
    }
}
