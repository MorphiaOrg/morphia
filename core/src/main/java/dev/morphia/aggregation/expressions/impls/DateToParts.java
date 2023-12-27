package dev.morphia.aggregation.expressions.impls;

import dev.morphia.aggregation.expressions.Expressions;
import dev.morphia.annotations.internal.MorphiaInternal;

/**
 * Returns a document that contains the constituent parts of a given BSON Date value as individual properties. The properties returned
 * are year, month, day, hour, minute, second and millisecond.
 */
public class DateToParts extends Expression {
    private final Expression date;
    private Expression timeZone;

    private ValueExpression iso8601;

    /**
     * @param date the date
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public DateToParts(Expression date) {
        super("$dateToParts");
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
    public Expression timeZone() {
        return timeZone;
    }

    /**
     * @hidden
     * @morphia.internal
     * @return If set to true, modifies the output document to use ISO week date fields.
     */
    @MorphiaInternal
    public ValueExpression iso8601() {
        return iso8601;
    }

    /**
     * Optional. If set to true, modifies the output document to use ISO week date fields.
     * Defaults to false.
     *
     * @param iso8601 true to use ISO 8601
     * @return this
     */
    public DateToParts iso8601(boolean iso8601) {
        this.iso8601 = new ValueExpression(iso8601);
        return this;
    }

    /**
     * The optional timezone to use to format the date. By default, it uses UTC.
     *
     * @param timezone the expression
     * @return this
     */
    public DateToParts timezone(Object timezone) {
        this.timeZone = Expressions.wrap(timezone);
        return this;
    }
}
