package dev.morphia.mapping.codec.expressions;

import dev.morphia.MorphiaDatastore;
import dev.morphia.aggregation.expressions.impls.RankExpression;

import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import static dev.morphia.mapping.codec.expressions.ExpressionCodecHelper.document;

public class RankExpressionCodec extends BaseExpressionCodec<RankExpression> {
    public RankExpressionCodec(MorphiaDatastore datastore) {
        super(datastore);
    }

    @Override
    public void encode(BsonWriter writer, RankExpression value, EncoderContext encoderContext) {
        document(writer, value.operation(), () -> {
        });

    }

    @Override
    public Class<RankExpression> getEncoderClass() {
        return RankExpression.class;
    }
}
