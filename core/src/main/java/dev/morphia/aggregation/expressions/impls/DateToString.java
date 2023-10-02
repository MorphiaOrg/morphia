package dev.morphia.aggregation.expressions.impls;

import dev.morphia.aggregation.expressions.Expressions;
import dev.morphia.annotations.internal.MorphiaInternal;

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

    public DateToString date(String date) {
        return date(Expressions.value(date));
    }

    public DateToString date(Expression date) {
        this.date = date;
        return this;
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
    public Expression date() {
        return date;
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
    public Expression onNull() {
        return onNull;
    }

    public DateToString format(String format) {
        return format(Expressions.value(format));
    }

    public DateToString format(Expression format) {
        this.format = format;
        return this;
    }

    public DateToString onNull(String onNull) {
        return onNull(Expressions.value(onNull));
    }

    public DateToString onNull(Expression onNull) {
        this.onNull = onNull;
        return this;
    }

    public DateToString timeZone(String timeZone) {
        return timeZone(Expressions.value(timeZone));
    }

    public DateToString timeZone(Expression timeZone) {
        this.timeZone = timeZone;
        return this;
    }
}
