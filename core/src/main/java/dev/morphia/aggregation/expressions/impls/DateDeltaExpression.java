package dev.morphia.aggregation.expressions.impls;

import dev.morphia.aggregation.expressions.Expressions;
import dev.morphia.aggregation.expressions.TimeUnit;
import dev.morphia.annotations.internal.MorphiaInternal;

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
     * @param operator  the operator name
     * @param startDate the start date
     * @param amount    the offset amount
     * @param unit      the unit of time
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public DateDeltaExpression(String operator, Expression startDate, long amount, TimeUnit unit) {
        super(operator);
        this.startDate = startDate;
        this.amount = amount;
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
     * @return the offset amount
     */
    @MorphiaInternal
    public long amount() {
        return amount;
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
     * The timezone to carry out the operation. {@code timezone} must be a valid expression that resolves to a string formatted as either
     * an Olson Timezone Identifier or a UTC Offset. If no timezone is provided, the result is displayed in UTC.
     *
     * @param timezone the timezone expression
     * @return this
     * @since 2.3
     */
    public DateDeltaExpression timezone(Object timezone) {
        this.timezone = Expressions.wrap(timezone);
        return this;
    }
}
