package dev.morphia.mapping.codec.expressions;

import dev.morphia.MorphiaDatastore;
import dev.morphia.aggregation.expressions.ObjectExpressions.MergeObjects;

import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import static dev.morphia.mapping.codec.expressions.ExpressionCodecHelper.encodeIfNotNull;

public class MergeObjectsCodec extends BaseExpressionCodec<MergeObjects> {
    public MergeObjectsCodec(MorphiaDatastore datastore) {
        super(datastore);
    }

    @Override
    public void encode(BsonWriter writer, MergeObjects value, EncoderContext encoderContext) {
        encodeIfNotNull(datastore.getCodecRegistry(), writer, value.operation(), value.value(), encoderContext);

    }

    @Override
    public Class<MergeObjects> getEncoderClass() {
        return MergeObjects.class;
    }
}
