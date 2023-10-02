package dev.morphia.aggregation.expressions.impls;

import dev.morphia.aggregation.expressions.Expressions;
import dev.morphia.annotations.internal.MorphiaInternal;

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

    public DateFromString dateString(String dateString) {
        return dateString(Expressions.value(dateString));
    }

    public DateFromString dateString(Expression dateString) {
        this.dateString = dateString;
        return this;
    }

    /**
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public Expression dateString() {
        return dateString;
    }

    /**
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public Expression format() {
        return format;
    }

    /**
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public Expression timeZone() {
        return timeZone;
    }

    /**
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public Expression onError() {
        return onError;
    }

    /**
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public Expression onNull() {
        return onNull;
    }

    public DateFromString format(Expression format) {
        this.format = format;
        return this;
    }

    public DateFromString format(String format) {
        return format(Expressions.value(format));
    }

    public DateFromString onError(String onError) {
        return onError(Expressions.value(onError));
    }

    public DateFromString onError(Expression onError) {
        this.onError = onError;
        return this;
    }

    public DateFromString onNull(String onNull) {
        return onNull(Expressions.value(onNull));
    }

    public DateFromString onNull(Expression onNull) {
        this.onNull = onNull;
        return this;
    }

    public DateFromString timeZone(String timeZone) {
        return timeZone(Expressions.value(timeZone));
    }

    public DateFromString timeZone(Expression timeZone) {
        this.timeZone = timeZone;
        return this;
    }
}
