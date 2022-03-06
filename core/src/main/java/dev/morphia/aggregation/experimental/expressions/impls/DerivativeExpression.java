package dev.morphia.aggregation.experimental.expressions.impls;

import dev.morphia.Datastore;
import dev.morphia.aggregation.experimental.expressions.TimeUnit;
import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import java.util.Locale;

import static dev.morphia.aggregation.experimental.codecs.ExpressionHelper.document;
import static dev.morphia.aggregation.experimental.codecs.ExpressionHelper.expression;

/**
 * Returns the average rate of change within the specified window.
 *
 * @aggregation.expression $derivative
 * @mongodb.server.release 5.0
 * @since 2.3
 */
public class DerivativeExpression extends Expression {
    private final Expression input;
    private TimeUnit unit;

    public DerivativeExpression(Expression input) {
        super("$derivative");
        this.input = input;
    }

    @Override
    public void encode(Datastore datastore, BsonWriter writer, EncoderContext encoderContext) {
        document(writer, getOperation(), () -> {
            expression(datastore, writer, "input", input, encoderContext);
            if (unit != null) {
                writer.writeString("unit", unit.name().toLowerCase(Locale.ROOT));
            }
        });
    }

    /**
     * Sets the time unit for the expression
     *
     * @param unit the unit
     * @return this
     */
    public DerivativeExpression unit(TimeUnit unit) {
        this.unit = unit;
        return this;
    }
}
