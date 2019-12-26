package dev.morphia.aggregation.experimental.codecs.expressions;

import dev.morphia.aggregation.experimental.stages.DateExpression.DateToStringExpression;
import dev.morphia.aggregation.experimental.stages.Expression;
import dev.morphia.mapping.Mapper;
import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecRegistry;

public class DateToStringExpressionCodec extends ExpressionCodec<DateToStringExpression> {
    public DateToStringExpressionCodec(final Mapper mapper) {
        super(mapper, DateToStringExpression.class);
    }

    @Override
    public void encode(final BsonWriter writer, final DateToStringExpression expression, final EncoderContext encoderContext) {
        writer.writeStartDocument(expression.getOperation());
        writeExpression(writer, "date", (Expression) expression.getValue(), encoderContext);
        if(expression.getName() != null) {
            writer.writeString("format", expression.getName());
        }
        writeExpression(writer, "timezone", expression.getTimeZone(), encoderContext);
        writeExpression(writer, "onNull", expression.getOnNull(), encoderContext);

        writer.writeEndDocument();
    }

    private void writeExpression(final BsonWriter writer,
                                 final String name, final Expression expression,
                                 final EncoderContext encoderContext) {
        if (expression != null) {
            writer.writeName(name);
            Object value = expression.getValue();
            Codec codec = getCodecRegistry().get(value.getClass());
            encoderContext.encodeWithChildContext(codec, writer, value);
        }
    }
}
