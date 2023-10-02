package dev.morphia.aggregation.codecs.stages;

import dev.morphia.MorphiaDatastore;
import dev.morphia.aggregation.stages.Bucket;

import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import static dev.morphia.mapping.codec.expressions.ExpressionCodecHelper.document;
import static dev.morphia.mapping.codec.expressions.ExpressionCodecHelper.encodeIfNotNull;
import static dev.morphia.mapping.codec.expressions.ExpressionCodecHelper.value;

public class BucketCodec extends StageCodec<Bucket> {
    public BucketCodec(MorphiaDatastore datastore) {
        super(datastore);
    }

    @Override
    public Class getEncoderClass() {
        return Bucket.class;
    }

    @Override
    protected void encodeStage(BsonWriter writer, Bucket value, EncoderContext encoderContext) {
        document(writer, () -> {
            encodeIfNotNull(getCodecRegistry(), writer, "groupBy", value.getGroupBy(), encoderContext);
            encodeIfNotNull(getCodecRegistry(), writer, "boundaries", value.getBoundaries(), encoderContext);
            value(getDatastore().getCodecRegistry(), writer, "default", value.getDefaultValue(), encoderContext);
            encodeIfNotNull(getCodecRegistry(), writer, "output", value.getOutput(), encoderContext);
        });
    }
}
