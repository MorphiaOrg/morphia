package dev.morphia.aggregation.experimental.expressions.internal;

import dev.morphia.aggregation.experimental.codecs.ExpressionCodec;
import dev.morphia.aggregation.experimental.expressions.Expression;
import dev.morphia.mapping.Mapper;
import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

public class DateToString extends Expression {
    private Expression format;
    private Expression date;
    private Expression timeZone;
    private Expression onNull;

    public DateToString() {
        super("$dateToString");
    }

    public DateToString date(final String date) {
        return date(literal(date));
    }

    public DateToString date(final Expression date) {
        this.date = date;
        return this;
    }

    @Override
    public void encode(final Mapper mapper, final BsonWriter writer, final EncoderContext encoderContext) {
        writer.writeStartDocument();
        writer.writeStartDocument(getOperation());
        ExpressionCodec.writeNamedExpression(mapper, writer, "date", date, encoderContext);
        ExpressionCodec.writeNamedExpression(mapper, writer, "format", format, encoderContext);
        ExpressionCodec.writeNamedExpression(mapper, writer, "timezone", timeZone, encoderContext);
        ExpressionCodec.writeNamedExpression(mapper, writer, "onNull", onNull, encoderContext);

        writer.writeEndDocument();
        writer.writeEndDocument();
    }

    public DateToString format(final String format) {
        return format(literal(format));
    }

    public DateToString format(final Expression format) {
        this.format = format;
        return this;
    }

    public DateToString onNull(final String onNull) {
        return onNull(literal(onNull));
    }

    public DateToString onNull(final Expression onNull) {
        this.onNull = onNull;
        return this;
    }

    public DateToString timeZone(final String timeZone) {
        return timeZone(literal(timeZone));
    }

    public DateToString timeZone(final Expression timeZone) {
        this.timeZone = timeZone;
        return this;
    }
}
