package dev.morphia.aggregation.experimental.expressions;

import dev.morphia.mapping.Mapper;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.EncoderContext;

class DateToStringExpression extends Expression {
    private final String format;
    private final Expression expression;
    private Expression timeZone;
    private Expression onNull;

    protected DateToStringExpression(final String format,
                                  final Expression expression) {
        super("$dateToString");
        this.format = format;
        this.expression = expression;
    }

    @Override
    public void encode(final Mapper mapper, final BsonWriter writer, final EncoderContext encoderContext) {
        writer.writeStartDocument();
        writer.writeStartDocument(operation);
        writeExpression(mapper, writer, "date", expression, encoderContext);
        if (format != null) {
            writer.writeString("format", format);
        }
        writeExpression(mapper, writer, "timezone", timeZone, encoderContext);
        writeExpression(mapper, writer, "onNull", onNull, encoderContext);

        writer.writeEndDocument();
        writer.writeEndDocument();
    }

    private void writeExpression(final Mapper mapper, final BsonWriter writer, final String name, final Expression expression,
                                 final EncoderContext encoderContext) {
        if (expression != null) {
            writer.writeName(name);
            Object value = expression.getValue();
            Codec codec = mapper.getCodecRegistry().get(value.getClass());
            encoderContext.encodeWithChildContext(codec, writer, value);
        }
    }

    public DateToStringExpression onNull(final Expression onNull) {
        this.onNull = onNull;
        return this;
    }

    public DateToStringExpression timeZone(final Expression timeZone) {
        this.timeZone = timeZone;
        return this;
    }
}
