package dev.morphia.aggregation.expressions.impls;

import dev.morphia.aggregation.expressions.Expressions;
import dev.morphia.annotations.internal.MorphiaInternal;

/**
 * Converts a date/time string to a date object.
 */
public class DateFromString extends Expression {
    private Expression dateString;
    private Expression format;
    private Expression timeZone;
    private Expression onError;
    private Expression onNull;

    /**
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public DateFromString() {
        super("$dateFromString");
    }

    /**
     * @param dateString the date string
     * @return this
     */
    public DateFromString dateString(Object dateString) {
        this.dateString = Expressions.wrap(dateString);
        return this;
    }

    /**
     * @hidden
     * @morphia.internal
     * @return the date string
     */
    @MorphiaInternal
    public Expression dateString() {
        return dateString;
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
     * @return the timezone
     */
    @MorphiaInternal
    public Expression timeZone() {
        return timeZone;
    }

    /**
     * @hidden
     * @morphia.internal
     * @return the onError expression
     */
    @MorphiaInternal
    public Expression onError() {
        return onError;
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
    public DateFromString format(Object format) {
        this.format = Expressions.wrap(format);
        return this;
    }

    /**
     * @param onError the onError expression
     * @return this
     */
    public DateFromString onError(Object onError) {
        this.onError = Expressions.wrap(onError);
        return this;
    }

    /**
     * @param onNull the onNull expression
     * @return this
     */
    public DateFromString onNull(Object onNull) {
        this.onNull = Expressions.wrap(onNull);
        return this;
    }

    /**
     * @param timeZone the timezone
     * @return this
     */
    public DateFromString timeZone(Object timeZone) {
        this.timeZone = Expressions.wrap(timeZone);
        return this;
    }
}
