package dev.morphia.aggregation.experimental.expressions;

import dev.morphia.mapping.Mapper;
import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

class DateToString extends Expression {
    private final String format;
    private final Expression expression;
    private Expression timeZone;
    private Expression onNull;

    protected DateToString(final String format,
                           final Expression expression) {
        super("$dateToString");
        this.format = format;
        this.expression = expression;
    }

    @Override
    public void encode(final Mapper mapper, final BsonWriter writer, final EncoderContext encoderContext) {
        writer.writeStartDocument();
        writer.writeStartDocument(operation);
        writeNamedExpression(mapper, writer, "date", expression, encoderContext);
        if (format != null) {
            writer.writeString("format", format);
        }
        writeNamedExpression(mapper, writer, "timezone", timeZone, encoderContext);
        writeNamedExpression(mapper, writer, "onNull", onNull, encoderContext);

        writer.writeEndDocument();
        writer.writeEndDocument();
    }

    public DateToString onNull(final Expression onNull) {
        this.onNull = onNull;
        return this;
    }

    public DateToString timeZone(final Expression timeZone) {
        this.timeZone = timeZone;
        return this;
    }
}
