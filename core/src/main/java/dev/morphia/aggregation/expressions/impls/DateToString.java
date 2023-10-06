package dev.morphia.aggregation.expressions.impls;

import dev.morphia.aggregation.expressions.Expressions;
import dev.morphia.annotations.internal.MorphiaInternal;

/**
 * Returns the date as a formatted string.
 */
public class DateToString extends Expression {
    private Expression format;
    private Expression date;
    private Expression timeZone;
    private Expression onNull;

    /**
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public DateToString() {
        super("$dateToString");
    }

    /**
     * @param date the date
     * @return this
     */
    public DateToString date(String date) {
        return date(Expressions.value(date));
    }

    /**
     * @param date the date
     * @return this
     */
    public DateToString date(Expression date) {
        this.date = date;
        return this;
    }

    /**
     * @hidden
     * @morphia.internal
     * @return the format
     */
    @MorphiaInternal
    public Expression format() {
        return format;
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
    public Expression timeZone() {
        return timeZone;
    }

    /**
     * @hidden
     * @morphia.internal
     * @return the onNull expression
     */
    @MorphiaInternal
    public Expression onNull() {
        return onNull;
    }

    /**
     * @param format the format
     * @return this
     */
    public DateToString format(String format) {
        return format(Expressions.value(format));
    }

    /**
     * @param format the format
     * @return this
     */
    public DateToString format(Expression format) {
        this.format = format;
        return this;
    }

    /**
     * @param onNull the onNull expression
     * @return this
     */
    public DateToString onNull(String onNull) {
        return onNull(Expressions.value(onNull));
    }

    /**
     * @param onNull the onNull expression
     * @return this
     */
    public DateToString onNull(Expression onNull) {
        this.onNull = onNull;
        return this;
    }

    /**
     * @param timeZone the timezone
     * @return this
     */
    public DateToString timeZone(String timeZone) {
        return timeZone(Expressions.value(timeZone));
    }

    /**
     * @param timeZone the timezone
     * @return this
     */
    public DateToString timeZone(Expression timeZone) {
        this.timeZone = timeZone;
        return this;
    }
}
