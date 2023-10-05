package dev.morphia.aggregation.codecs.stages;

import dev.morphia.MorphiaDatastore;
import dev.morphia.aggregation.stages.AutoBucket;

import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecRegistry;

import static dev.morphia.mapping.codec.CodecHelper.document;
import static dev.morphia.mapping.codec.CodecHelper.encodeIfNotNull;

public class AutoBucketCodec extends StageCodec<AutoBucket> {

    public AutoBucketCodec(MorphiaDatastore datastore) {
        super(datastore);
    }

    @Override
    public Class getEncoderClass() {
        return AutoBucket.class;
    }

    @Override
    protected void encodeStage(BsonWriter writer, AutoBucket value, EncoderContext encoderContext) {
        document(writer, () -> {
            CodecRegistry registry = getDatastore().getCodecRegistry();
            encodeIfNotNull(registry, writer, "groupBy", value.getGroupBy(), encoderContext);
            encodeIfNotNull(registry, writer, "buckets", value.getBuckets(), encoderContext);
            encodeIfNotNull(registry, writer, "granularity", value.getGranularity(), encoderContext);
            encodeIfNotNull(registry, writer, "output", value.getOutput(), encoderContext);
        });
    }
}
