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
    public DateToString date(Object date) {
        this.date = Expressions.wrap(date);
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
    public DateToString format(Object format) {
        this.format = Expressions.wrap(format);
        return this;
    }

    /**
     * @param onNull the onNull expression
     * @return this
     */
    public DateToString onNull(Object onNull) {
        this.onNull = Expressions.wrap(onNull);
        return this;
    }

    /**
     * @param timeZone the timezone
     * @return this
     */
    public DateToString timeZone(Object timeZone) {
        this.timeZone = Expressions.wrap(timeZone);
        return this;
    }
}
