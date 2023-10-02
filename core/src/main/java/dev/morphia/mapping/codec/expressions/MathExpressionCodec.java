package dev.morphia.mapping.codec.expressions;

import dev.morphia.MorphiaDatastore;
import dev.morphia.aggregation.expressions.impls.Expression;
import dev.morphia.aggregation.expressions.impls.ExpressionList;
import dev.morphia.aggregation.expressions.impls.MathExpression;
import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import java.util.List;

import static dev.morphia.aggregation.codecs.ExpressionHelper.wrapExpression;
import static dev.morphia.mapping.codec.expressions.ExpressionCodecHelper.encodeIfNotNull;

public class MathExpressionCodec extends BaseExpressionCodec<MathExpression> {
    public MathExpressionCodec(MorphiaDatastore datastore) {
        super(datastore);
    }

    @Override
    public void encode(BsonWriter writer, MathExpression expression, EncoderContext encoderContext) {
        ExpressionList value = expression.value();
        if (value != null) {
            final List<Expression> operands = value.getValues();
            writer.writeName(expression.operation());
            if (operands.size() > 1) {
                writer.writeStartArray();
            }
            for (Expression operand : operands) {
                if (operand != null) {
                    encodeIfNotNull(datastore.getCodecRegistry(), writer, operand, encoderContext);
                } else {
                    writer.writeNull();
                }
            }
            if (operands.size() > 1) {
                writer.writeEndArray();
            }
        }
    }

    @Override
    public Class<MathExpression> getEncoderClass() {
        return MathExpression.class;
    }
}
