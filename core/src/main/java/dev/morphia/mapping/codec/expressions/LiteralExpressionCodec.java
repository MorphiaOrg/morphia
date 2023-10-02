package dev.morphia.mapping.codec.expressions;

import dev.morphia.MorphiaDatastore;
import dev.morphia.aggregation.expressions.impls.LiteralExpression;
import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import static dev.morphia.mapping.codec.expressions.ExpressionCodecHelper.encodeIfNotNull;

public class LiteralExpressionCodec extends BaseExpressionCodec<LiteralExpression> {
    public LiteralExpressionCodec(MorphiaDatastore datastore) {
        super(datastore);
    }

    @Override
    public void encode(BsonWriter writer, LiteralExpression expression, EncoderContext encoderContext) {
        if (!encodeIfNotNull(datastore.getCodecRegistry(), writer, expression.operation(), expression.value(), encoderContext)) {
            writer.writeNull();
        }
    }

    @Override
    public Class<LiteralExpression> getEncoderClass() {
        return LiteralExpression.class;
    }
}
