package dev.morphia.aggregation.expressions.impls;

import java.time.DayOfWeek;
import java.util.Locale;

import dev.morphia.MorphiaDatastore;
import dev.morphia.aggregation.expressions.TimeUnit;

import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import static dev.morphia.aggregation.codecs.ExpressionHelper.document;
import static dev.morphia.aggregation.codecs.ExpressionHelper.expression;

/**
 * Truncates a date.
 *
 * @mongodb.server.release 5.0
 * @aggregation.expression $dateTrunc
 * @since 2.3
 */
public class DateTruncExpression extends Expression {
    private final Expression date;
    private final TimeUnit unit;
    private Expression timezone;
    private DayOfWeek startOfWeek;
    private Long binSize;

    public DateTruncExpression(Expression date, TimeUnit unit) {
        super("$dateTrunc");
        this.date = date;
        this.unit = unit;
    }

    /**
     * The numeric time value, specified as an expression that must resolve to a positive non-zero number. Defaults to 1.
     * <p>
     * Together, binSize and unit specify the time period used in the $dateTrunc calculation.
     *
     * @param binSize the size to use
     * @return this
     * @since 2.3
     */
    public DateTruncExpression binSize(long binSize) {
        this.binSize = binSize;
        return this;
    }

    @Override
    public void encode(MorphiaDatastore datastore, BsonWriter writer, EncoderContext encoderContext) {
        document(writer, operation(), () -> {
            expression(datastore, writer, "date", date, encoderContext);
            writer.writeString("unit", unit.name().toLowerCase(Locale.ROOT));
            if (binSize != null) {
                writer.writeInt64("binSize", binSize);
            }
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
    public DateTruncExpression startOfWeek(DayOfWeek startOfWeek) {
        this.startOfWeek = startOfWeek;
        return this;
    }

    /**
     * The timezone to carry out the operation. <tzExpression> must be a valid expression that resolves to a string formatted as either
     * an Olson Timezone Identifier or a UTC Offset. If no timezone is provided, the result is displayed in UTC.
     *
     * @param timezone the timezone expression
     * @return this
     * @since 2.3
     */
    public DateTruncExpression timezone(Expression timezone) {
        this.timezone = timezone;
        return this;
    }
}
