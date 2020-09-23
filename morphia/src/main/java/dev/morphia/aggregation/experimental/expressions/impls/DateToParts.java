package dev.morphia.aggregation.experimental.expressions.impls;

import dev.morphia.mapping.Mapper;
import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import static dev.morphia.aggregation.experimental.codecs.ExpressionHelper.expression;
import static dev.morphia.aggregation.experimental.codecs.ExpressionHelper.value;

/**
 * Returns a document that contains the constituent parts of a given BSON Date value as individual properties. The properties returned
 * are year, month, day, hour, minute, second and millisecond.
 *
 * @morphia.internal
 */
public class DateToParts extends Expression {
    private final Expression date;
    private Expression timeZone;

    private Boolean iso8601;

    public DateToParts(Expression date) {
        super("$dateToParts");
        this.date = date;
    }

    @Override
    public void encode(Mapper mapper, BsonWriter writer, EncoderContext encoderContext) {
        writer.writeStartDocument();
        writer.writeName(getOperation());

        writer.writeStartDocument();
        expression(mapper, writer, "date", date, encoderContext);
        expression(mapper, writer, "timezone", timeZone, encoderContext);
        value(mapper, writer, "iso8601", iso8601, encoderContext);
        writer.writeEndDocument();

        writer.writeEndDocument();
    }

    /**
     * Optional. If set to true, modifies the output document to use ISO week date fields.
     * Defaults to false.
     *
     * @param iso8601 true to use ISO 8601
     * @return this
     */
    public DateToParts iso8601(boolean iso8601) {
        this.iso8601 = iso8601;
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
