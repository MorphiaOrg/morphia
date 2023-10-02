package dev.morphia.mapping.codec.expressions;

import dev.morphia.MorphiaDatastore;
import dev.morphia.aggregation.expressions.DenseRankExpression;
import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import static dev.morphia.mapping.codec.expressions.ExpressionCodecHelper.document;

public class DenseRankExpressionCodec extends BaseExpressionCodec<DenseRankExpression> {
    public DenseRankExpressionCodec(MorphiaDatastore datastore) {
        super(datastore);
    }

    @Override
    public void encode(BsonWriter writer, DenseRankExpression value, EncoderContext encoderContext) {
        document(writer, value.operation(), () -> {
        });

    }

    @Override
    public Class<DenseRankExpression> getEncoderClass() {
        return DenseRankExpression.class;
    }
}
