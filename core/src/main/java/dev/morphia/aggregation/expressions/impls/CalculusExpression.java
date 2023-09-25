package dev.morphia.aggregation.expressions.impls;

import java.util.Locale;

import dev.morphia.MorphiaDatastore;
import dev.morphia.aggregation.expressions.TimeUnit;
import dev.morphia.aggregation.expressions.WindowExpressions;

import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import static dev.morphia.aggregation.codecs.ExpressionHelper.document;
import static dev.morphia.aggregation.codecs.ExpressionHelper.expression;

/**
 * Common type for $derivative and $integral
 *
 * @aggregation.expression $derivative
 * @aggregation.expression $integral
 * @mongodb.server.release 5.0
 * @see WindowExpressions#derivative(Expression)
 * @see WindowExpressions#integral(Expression)
 * @since 2.3
 */
public class CalculusExpression extends Expression {
    private final Expression input;
    private TimeUnit unit;

    public CalculusExpression(String operation, Expression input) {
        super(operation);
        this.input = input;
    }

    @Override
    public void encode(MorphiaDatastore datastore, BsonWriter writer, EncoderContext encoderContext) {
        document(writer, operation(), () -> {
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
    public CalculusExpression unit(TimeUnit unit) {
        this.unit = unit;
        return this;
    }
}
