package dev.morphia.aggregation.expressions.impls;

import java.time.DayOfWeek;

import dev.morphia.aggregation.expressions.TimeUnit;
import dev.morphia.annotations.internal.MorphiaInternal;

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

    /**
     * @param startDate the start date
     * @param endDate   the end date
     * @param unit      the unit of time
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public DateDiffExpression(Expression startDate, Expression endDate, TimeUnit unit) {
        super("$dateDiff");
        this.startDate = startDate;
        this.endDate = endDate;
        this.unit = unit;
    }

    /**
     * @hidden
     * @morphia.internal
     * @return the start date
     */
    @MorphiaInternal
    public Expression startDate() {
        return startDate;
    }

    /**
     * @hidden
     * @morphia.internal
     * @return the end date
     */
    @MorphiaInternal
    public Expression endDate() {
        return endDate;
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
     * @return the start of week day
     */
    @MorphiaInternal
    public DayOfWeek startOfWeek() {
        return startOfWeek;
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
     * The timezone to carry out the operation. {@code timezone} must be a valid expression that resolves to a string formatted as either
     * an Olson Timezone Identifier or a UTC Offset. If no timezone is provided, the result is displayed in UTC.
     *
     * @param timezone the timezone expression
     * @since 2.3
     * @return this
     */
    public DateDiffExpression timezone(Expression timezone) {
        this.timezone = timezone;
        return this;
    }
}
