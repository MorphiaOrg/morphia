package dev.morphia.aggregation.codecs.stages;

import dev.morphia.MorphiaDatastore;
import dev.morphia.aggregation.expressions.impls.PipelineField;
import dev.morphia.aggregation.stages.Projection;

import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import static dev.morphia.mapping.codec.CodecHelper.document;
import static dev.morphia.mapping.codec.CodecHelper.encodeIfNotNull;

public class ProjectionCodec extends StageCodec<Projection> {
    public ProjectionCodec(MorphiaDatastore datastore) {
        super(datastore);
    }

    @Override
    public Class<Projection> getEncoderClass() {
        return Projection.class;
    }

    @Override
    protected void encodeStage(BsonWriter writer, Projection projection, EncoderContext encoderContext) {
        document(writer, () -> {
            for (PipelineField field : projection.getFields()) {
                encodeIfNotNull(getCodecRegistry(), writer, field.name(), field.value(), encoderContext);
            }
        });
    }
}
