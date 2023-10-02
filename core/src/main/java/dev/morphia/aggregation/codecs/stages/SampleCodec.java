package dev.morphia.aggregation.codecs.stages;

import dev.morphia.MorphiaDatastore;
import dev.morphia.aggregation.stages.Sample;

import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import static dev.morphia.mapping.codec.expressions.ExpressionCodecHelper.document;

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
        document(writer, () -> writer.writeInt64("size", value.getSize()));
    }
}
