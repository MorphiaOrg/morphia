package dev.morphia.aggregation.expressions.impls;

import dev.morphia.annotations.internal.MorphiaInternal;

/**
 * Reusable type for ISO Date related expressions.
 *
 * @see dev.morphia.aggregation.expressions.DateExpressions#isoDayOfWeek(Expression)
 * @see dev.morphia.aggregation.expressions.DateExpressions#isoWeek(Expression)
 * @see dev.morphia.aggregation.expressions.DateExpressions#isoWeekYear(Expression)
 */
public class IsoDates extends Expression {
    private final Expression date;
    private Expression timezone;

    /**
     * @param operation
     * @param date
     * @morphia.internal
     */
    @MorphiaInternal
    public IsoDates(String operation, Expression date) {
        super(operation);
        this.date = date;
    }

    public Expression date() {
        return date;
    }

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
