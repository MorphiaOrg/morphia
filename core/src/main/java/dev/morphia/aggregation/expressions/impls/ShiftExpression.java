package dev.morphia.aggregation.expressions.impls;

import dev.morphia.MorphiaDatastore;

import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import static dev.morphia.aggregation.codecs.ExpressionHelper.document;
import static dev.morphia.aggregation.codecs.ExpressionHelper.expression;

/**
 * @aggregation.expression $shift
 * @mongodb.server.release 5.0
 * @since 2.3
 */
public class ShiftExpression extends Expression {
    private final Expression output;
    private final long by;
    private final Expression defaultValue;

    public ShiftExpression(Expression output, long by, Expression defaultValue) {
        super("$shift");
        this.output = output;
        this.by = by;
        this.defaultValue = defaultValue;
    }

    @Override
    public void encode(MorphiaDatastore datastore, BsonWriter writer, EncoderContext encoderContext) {
        document(writer, operation(), () -> {
            expression(datastore, writer, "output", output, encoderContext);
            writer.writeInt64("by", by);
            expression(datastore, writer, "default", defaultValue, encoderContext);
        });
    }
}
