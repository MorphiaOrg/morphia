package dev.morphia.aggregation.expressions.impls;

import java.time.DayOfWeek;

import dev.morphia.aggregation.expressions.TimeUnit;
import dev.morphia.annotations.internal.MorphiaInternal;

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

    /**
     * @param date the date
     * @param unit the unit of time
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
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

    /**
     * @hidden
     * @morphia.internal
     * @return the date
     */
    @MorphiaInternal
    public Expression date() {
        return date;
    }

    /**
     * @hidden
     * @morphia.internal
     * @return the unit of time
     */
    @MorphiaInternal
    public TimeUnit unit() {
        return unit;
    }

    /**
     * @hidden
     * @morphia.internal
     * @return the timezone
     */
    @MorphiaInternal
    public Expression timezone() {
        return timezone;
    }

    /**
     * @hidden
     * @morphia.internal
     * @return the start of the week
     */
    @MorphiaInternal
    public DayOfWeek startOfWeek() {
        return startOfWeek;
    }

    /**
     * @hidden
     * @morphia.internal
     * @return the bin size
     */
    @MorphiaInternal
    public Long binSize() {
        return binSize;
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
     * The timezone to carry out the operation. {@code timezone} must be a valid expression that resolves to a string formatted as either
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
