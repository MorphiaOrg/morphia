package dev.morphia.aggregation.experimental.expressions.impls;

import dev.morphia.aggregation.experimental.codecs.ExpressionCodec;
import dev.morphia.aggregation.experimental.expressions.Expressions;
import dev.morphia.mapping.Mapper;
import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

public class DateFromString extends Expression {
    private Expression dateString;
    private Expression format;
    private Expression timeZone;
    private Expression onError;
    private Expression onNull;

    public DateFromString() {
        super("$dateFromString");
    }

    @Override
    public void encode(final Mapper mapper, final BsonWriter writer, final EncoderContext encoderContext) {
        writer.writeStartDocument();
        writer.writeStartDocument(getOperation());

        ExpressionCodec.writeNamedExpression(mapper, writer, "dateString", dateString, encoderContext);
        ExpressionCodec.writeNamedExpression(mapper, writer, "format", format, encoderContext);
        ExpressionCodec.writeNamedExpression(mapper, writer, "timezone", timeZone, encoderContext);
        ExpressionCodec.writeNamedExpression(mapper, writer, "onError", onError, encoderContext);
        ExpressionCodec.writeNamedExpression(mapper, writer, "onNull", onNull, encoderContext);

        writer.writeEndDocument();
        writer.writeEndDocument();
    }

    public DateFromString dateString(final String dateString) {
        return dateString(Expressions.value(dateString));
    }

    public DateFromString format(final Expression format) {
        this.format = format;
        return this;
    }

    public DateFromString format(final String format) {
        return format(Expressions.value(format));
    }

    public DateFromString dateString(final Expression dateString) {
        this.dateString = dateString;
        return this;
    }

    public DateFromString onError(final String onError) {
        return onError(Expressions.value(onError));
    }

    public DateFromString onError(final Expression onError) {
        this.onError = onError;
        return this;
    }

    public DateFromString onNull(final String onNull) {
        return onNull(Expressions.value(onNull));
    }

    public DateFromString onNull(final Expression onNull) {
        this.onNull = onNull;
        return this;
    }

    public DateFromString timeZone(final String timeZone) {
        return timeZone(Expressions.value(timeZone));
    }

    public DateFromString timeZone(final Expression timeZone) {
        this.timeZone = timeZone;
        return this;
    }
}
