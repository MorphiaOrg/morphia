package dev.morphia.mapping.codec.expressions;

import dev.morphia.MorphiaDatastore;
import dev.morphia.aggregation.expressions.impls.CountExpression;

import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import static dev.morphia.mapping.codec.CodecHelper.document;

public class CountExpressionCodec extends BaseExpressionCodec<CountExpression> {
    public CountExpressionCodec(MorphiaDatastore datastore) {
        super(datastore);
    }

    @Override
    public void encode(BsonWriter bsonWriter, CountExpression countExpression, EncoderContext encoderContext) {
        document(bsonWriter, "$count", () -> {
        });
    }

    @Override
    public Class<CountExpression> getEncoderClass() {
        return CountExpression.class;
    }
}
