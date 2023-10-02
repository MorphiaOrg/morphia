package dev.morphia.aggregation.expressions.impls;

import dev.morphia.MorphiaDatastore;
import dev.morphia.aggregation.codecs.ExpressionHelper;
import dev.morphia.annotations.internal.MorphiaInternal;

import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import static dev.morphia.aggregation.codecs.ExpressionHelper.document;
import static dev.morphia.aggregation.codecs.ExpressionHelper.expression;

/**
 * Returns a document that contains the constituent parts of a given BSON Date value as individual properties. The properties returned
 * are year, month, day, hour, minute, second and millisecond.
 */
public class DateToParts extends Expression {
    private final Expression date;
    private Expression timeZone;

    private ValueExpression iso8601;

    /**
     * @param date
     * @morphia.internal
     */
    @MorphiaInternal
    public DateToParts(Expression date) {
        super("$dateToParts");
        this.date = date;
    }

    public Expression date() {
        return date;
    }

    public Expression timeZone() {
        return timeZone;
    }

    public ValueExpression iso8601() {
        return iso8601;
    }

    @Override
    public void encode(MorphiaDatastore datastore, BsonWriter writer, EncoderContext encoderContext) {
        throw new UnsupportedOperationException();
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
    public DateToParts timezone(Expression timezone) {
        this.timeZone = timezone;
        return this;
    }
}
