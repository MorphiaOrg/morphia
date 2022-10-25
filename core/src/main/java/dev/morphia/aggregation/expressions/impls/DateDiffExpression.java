package dev.morphia.aggregation.expressions.impls;

import java.time.DayOfWeek;
import java.util.Locale;

import dev.morphia.Datastore;
import dev.morphia.aggregation.expressions.TimeUnit;

import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import static dev.morphia.aggregation.codecs.ExpressionHelper.document;
import static dev.morphia.aggregation.codecs.ExpressionHelper.expression;

/**
 * Returns the difference between two dates.
 *
 * @mongodb.server.release 5.0
 * @aggregation.expression $dateDiff
 * @since 2.3
 */
public class DateDiffExpression extends Expression {
    private final Expression startDate;
    private final Expression endDate;
    private final TimeUnit unit;
    private Expression timezone;
    private DayOfWeek startOfWeek;

    public DateDiffExpression(Expression startDate, Expression endDate, TimeUnit unit) {
        super("$dateDiff");
        this.startDate = startDate;
        this.endDate = endDate;
        this.unit = unit;
    }

    @Override
    public void encode(Datastore datastore, BsonWriter writer, EncoderContext encoderContext) {
        document(writer, getOperation(), () -> {
            expression(datastore, writer, "startDate", startDate, encoderContext);
            expression(datastore, writer, "endDate", endDate, encoderContext);
            writer.writeString("unit", unit.name().toLowerCase(Locale.ROOT));
            expression(datastore, writer, "timezone", timezone, encoderContext);
            if (startOfWeek != null) {
                writer.writeString("startOfWeek", startOfWeek.name().toLowerCase(Locale.ROOT));
            }
        });
    }

    /**
     * The start of the week. Used when unit is week. Defaults to Sunday.
     *
     * @param startOfWeek the start of the week
     * @return this
     */
    public DateDiffExpression startOfWeek(DayOfWeek startOfWeek) {
        this.startOfWeek = startOfWeek;
        return this;
    }

    /**
     * The timezone to carry out the operation. <tzExpression> must be a valid expression that resolves to a string formatted as either
     * an Olson Timezone Identifier or a UTC Offset. If no timezone is provided, the result is displayed in UTC.
     *
     * @param timezone the timezone expression
     * @since 2.3
     */
    public DateDiffExpression timezone(Expression timezone) {
        this.timezone = timezone;
        return this;
    }
}
