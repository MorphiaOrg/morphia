package dev.morphia.aggregation.experimental.codecs.expressions;

import dev.morphia.aggregation.experimental.stages.Expression.PushExpression;
import dev.morphia.aggregation.experimental.stages.Expression.PushExpression.Field;
import dev.morphia.mapping.Mapper;
import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

public class PushExpressionCodec extends ExpressionCodec<PushExpression> {
    public PushExpressionCodec(final Mapper mapper) {
        super(mapper, PushExpression.class);
    }

    @Override
    public void encode(final BsonWriter writer, final PushExpression expression, final EncoderContext encoderContext) {
        writer.writeStartDocument(expression.getName());
        encodeExpression(writer, expression, encoderContext);
        writer.writeEndDocument();
    }

    private void encodeExpression(final BsonWriter writer, final PushExpression expression, final EncoderContext encoderContext) {
        writer.writeName(expression.getOperation());
        if (expression.getSource() != null) {
            String source = expression.getSource();
            writer.writeString(source.startsWith("$") ? source : "$" + source);
        } else if (expression.getFields() != null) {
            writer.writeStartDocument();
            for (final Field field : expression.getFields()) {
                String source = field.getSource();
                String renamed = field.getRenamed();
                if (!renamed.startsWith("$")) {
                    renamed = "$" + renamed;
                }
                writer.writeString(source, renamed);
            }
            writer.writeEndDocument();
        }
    }
}
