package dev.morphia.mapping.codec.expressions;

import java.util.List;

import dev.morphia.MorphiaDatastore;
import dev.morphia.aggregation.expressions.impls.Expression;
import dev.morphia.aggregation.expressions.impls.MergeObjects;

import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import static dev.morphia.mapping.codec.CodecHelper.encodeIfNotNull;

public class MergeObjectsCodec extends BaseExpressionCodec<MergeObjects> {
    public MergeObjectsCodec(MorphiaDatastore datastore) {
        super(datastore);
    }

    @Override
    public void encode(BsonWriter writer, MergeObjects value, EncoderContext encoderContext) {
        List<Expression> list = value.value().values();
        Expression merging = (list.size() == 1) ? list.get(0) : value.value();

        encodeIfNotNull(datastore.getCodecRegistry(), writer, value.operation(), merging, encoderContext);
    }

    @Override
    public Class<MergeObjects> getEncoderClass() {
        return MergeObjects.class;
    }
}
