package dev.morphia.aggregation.codecs.stages;

import dev.morphia.MorphiaDatastore;
import dev.morphia.aggregation.stages.Sample;

import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import static dev.morphia.mapping.codec.CodecHelper.document;
import static dev.morphia.mapping.codec.CodecHelper.whole;

public class SampleCodec extends StageCodec<Sample> {
    public SampleCodec(MorphiaDatastore datastore) {
        super(datastore);
    }

    @Override
    public Class<Sample> getEncoderClass() {
        return Sample.class;
    }

    @Override
    protected void encodeStage(BsonWriter writer, Sample value, EncoderContext encoderContext) {
        document(writer, () -> whole(writer, "size", value.getSize()));
    }
}
