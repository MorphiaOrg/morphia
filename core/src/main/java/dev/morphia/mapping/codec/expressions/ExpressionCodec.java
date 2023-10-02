package dev.morphia.mapping.codec.expressions;

import dev.morphia.MorphiaDatastore;
import dev.morphia.aggregation.expressions.impls.Expression;

import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import static dev.morphia.mapping.codec.expressions.ExpressionCodecHelper.encodeIfNotNull;

public class ExpressionCodec extends BaseExpressionCodec<Expression> {

    public ExpressionCodec(MorphiaDatastore datastore) {
        super(datastore);
    }

    @Override
    public void encode(BsonWriter writer, Expression expression, EncoderContext encoderContext) {
        if (!encodeIfNotNull(datastore.getCodecRegistry(), writer, expression.operation(), expression.value(), encoderContext)) {
            writer.writeNull();
        }
    }

    @Override
    public final Class<Expression> getEncoderClass() {
        return Expression.class;
    }
}
