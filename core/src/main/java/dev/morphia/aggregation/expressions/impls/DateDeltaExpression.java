package dev.morphia.aggregation.expressions.impls;

import java.util.Locale;

import dev.morphia.MorphiaDatastore;
import dev.morphia.aggregation.expressions.TimeUnit;
import dev.morphia.annotations.internal.MorphiaInternal;

import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import static dev.morphia.aggregation.codecs.ExpressionHelper.document;
import static dev.morphia.aggregation.codecs.ExpressionHelper.expression;

/**
 * Changes a Date object by a specified number of time units.
 *
 * @mongodb.server.release 5.0
 * @since 2.3
 */
public class DateDeltaExpression extends Expression {
    private final Expression startDate;
    private final long amount;
    private final TimeUnit unit;
    private Expression timezone;

    /**
     * @param operator
     * @param startDate
     * @param amount
     * @param unit
     * @morphia.internal
     */
    @MorphiaInternal
    public DateDeltaExpression(String operator, Expression startDate, long amount, TimeUnit unit) {
        super(operator);
        this.startDate = startDate;
        this.amount = amount;
        this.unit = unit;
    }

    @Override
    public void encode(MorphiaDatastore datastore, BsonWriter writer, EncoderContext encoderContext) {
        document(writer, operation(), () -> {
            expression(datastore, writer, "startDate", startDate, encoderContext);
            writer.writeString("unit", unit.name().toLowerCase(Locale.ROOT));
            writer.writeInt64("amount", amount);
            expression(datastore, writer, "timezone", timezone, encoderContext);
        });
    }

    /**
     * The timezone to carry out the operation. <tzExpression> must be a valid expression that resolves to a string formatted as either
     * an Olson Timezone Identifier or a UTC Offset. If no timezone is provided, the result is displayed in UTC.
     *
     * @param timezone the timezone expression
     * @return this
     * @since 2.3
     */
    public DateDeltaExpression timezone(Expression timezone) {
        this.timezone = timezone;
        return this;
    }
}
