package dev.morphia.aggregation.experimental.expressions;

import dev.morphia.aggregation.experimental.codecs.ExpressionCodec;
import dev.morphia.mapping.Mapper;
import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

public class DateFromString extends Expression {
    private Expression dateString;
    private Expression format;
    private Expression timeZone;
    private Expression onError;
    private Expression onNull;

    protected DateFromString() {
        super("$dateFromString");
    }

    @Override
    public void encode(final Mapper mapper, final BsonWriter writer, final EncoderContext encoderContext) {
        writer.writeStartDocument();
        writer.writeStartDocument(operation);

        ExpressionCodec.writeNamedExpression(mapper, writer, "dateString", dateString, encoderContext);
        ExpressionCodec.writeNamedExpression(mapper, writer, "format", format, encoderContext);
        ExpressionCodec.writeNamedExpression(mapper, writer, "timezone", timeZone, encoderContext);
        ExpressionCodec.writeNamedExpression(mapper, writer, "onError", onError, encoderContext);
        ExpressionCodec.writeNamedExpression(mapper, writer, "onNull", onNull, encoderContext);

        writer.writeEndDocument();
        writer.writeEndDocument();
    }

    public DateFromString format(final String format) {
        return format(literal(format));
    }

    public DateFromString format(final Expression format) {
        this.format = format;
        return this;
    }

    public DateFromString dateString(final String dateString) {
        return dateString(literal(dateString));
    }

    public DateFromString dateString(final Expression dateString) {
        this.dateString = dateString;
        return this;
    }

    public DateFromString onError(final String onError) {
        return onError(literal(onError));
    }

    public DateFromString onError(final Expression onError) {
        this.onError = onError;
        return this;
    }

    public DateFromString onNull(final String onNull) {
        return onNull(literal(onNull));
    }

    public DateFromString onNull(final Expression onNull) {
        this.onNull = onNull;
        return this;
    }

    public DateFromString timeZone(final String timeZone) {
        return timeZone(literal(timeZone));
    }

    public DateFromString timeZone(final Expression timeZone) {
        this.timeZone = timeZone;
        return this;
    }
}
