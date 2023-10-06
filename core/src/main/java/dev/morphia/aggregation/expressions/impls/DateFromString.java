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
    public DateFromString dateString(String dateString) {
        return dateString(Expressions.value(dateString));
    }

    /**
     * @param dateString the date string
     * @return this
     */
    public DateFromString dateString(Expression dateString) {
        this.dateString = dateString;
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
    public DateFromString format(Expression format) {
        this.format = format;
        return this;
    }

    /**
     * @param format the format
     * @return this
     */
    public DateFromString format(String format) {
        return format(Expressions.value(format));
    }

    /**
     * @param onError the onError expression
     * @return this
     */
    public DateFromString onError(String onError) {
        return onError(Expressions.value(onError));
    }

    /**
     * @param onError the onError expression
     * @return this
     */
    public DateFromString onError(Expression onError) {
        this.onError = onError;
        return this;
    }

    /**
     * @param onNull the onNull expression
     * @return this
     */
    public DateFromString onNull(String onNull) {
        return onNull(Expressions.value(onNull));
    }

    /**
     * @param onNull the onNull expression
     * @return this
     */
    public DateFromString onNull(Expression onNull) {
        this.onNull = onNull;
        return this;
    }

    /**
     * @param timeZone the timezone
     * @return this
     */
    public DateFromString timeZone(String timeZone) {
        return timeZone(Expressions.value(timeZone));
    }

    /**
     * @param timeZone the timezone
     * @return this
     */
    public DateFromString timeZone(Expression timeZone) {
        this.timeZone = timeZone;
        return this;
    }
}
