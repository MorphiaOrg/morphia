package dev.morphia.aggregation.expressions.impls;

import dev.morphia.Datastore;
import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import static dev.morphia.aggregation.codecs.ExpressionHelper.document;
import static dev.morphia.aggregation.codecs.ExpressionHelper.expression;

/**
 * Reusable type for ISO Date related expressions.
 *
 * @morphia.internal
 * @see dev.morphia.aggregation.expressions.DateExpressions#isoDayOfWeek(Expression)
 * @see dev.morphia.aggregation.expressions.DateExpressions#isoWeek(Expression)
 * @see dev.morphia.aggregation.expressions.DateExpressions#isoWeekYear(Expression)
 */
public class IsoDates extends Expression {
    private final Expression date;
    private Expression timezone;

    public IsoDates(String operation, Expression date) {
        super(operation);
        this.date = date;
    }

    @Override
    public void encode(Datastore datastore, BsonWriter writer, EncoderContext encoderContext) {
        document(writer, getOperation(), () -> {
            expression(datastore, writer, "date", date, encoderContext);
            expression(datastore, writer, "timezone", timezone, encoderContext);
        });
    }

    /**
     * The optional timezone to use to format the date. By default, it uses UTC.
     *
     * @param timezone the expression
     * @return this
     */
    public IsoDates timezone(Expression timezone) {
        this.timezone = timezone;
        return this;
    }
}
