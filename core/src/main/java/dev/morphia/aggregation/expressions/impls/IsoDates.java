package dev.morphia.aggregation.expressions.impls;

import dev.morphia.aggregation.expressions.DateExpressions;
import dev.morphia.annotations.internal.MorphiaInternal;

/**
 * Reusable type for ISO Date related expressions.
 *
 * @see DateExpressions#isoDayOfWeek(Expression)
 * @see DateExpressions#isoWeek(Expression)
 * @see DateExpressions#isoWeekYear(Expression)
 */
public class IsoDates extends Expression {
    private final Expression date;
    private Expression timezone;

    /**
     * @param operation the operation name
     * @param date      the date
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public IsoDates(String operation, Expression date) {
        super(operation);
        this.date = date;
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
     * @return the timezone
     */
    @MorphiaInternal
    public Expression timezone() {
        return timezone;
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
