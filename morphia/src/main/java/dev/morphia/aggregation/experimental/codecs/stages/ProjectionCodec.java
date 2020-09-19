package dev.morphia.aggregation.experimental.codecs.stages;

import dev.morphia.aggregation.experimental.expressions.impls.PipelineField;
import dev.morphia.aggregation.experimental.stages.Projection;
import dev.morphia.mapping.Mapper;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.EncoderContext;

public class ProjectionCodec extends StageCodec<Projection> {

    public ProjectionCodec(Mapper mapper) {
        super(mapper);
    }

    @Override
    protected void encodeStage(BsonWriter writer, Projection value, EncoderContext encoderContext) {
        writer.writeStartDocument();
        for (PipelineField field : value.getFields()) {
            write(writer, field, encoderContext);
        }
        writer.writeEndDocument();
    }

    private void write(BsonWriter writer, PipelineField field, EncoderContext encoderContext) {
        writer.writeName(field.getName());
        Class aClass = field.getValue().getClass();
        Codec codec = getCodecRegistry().get(aClass);
        encoderContext.encodeWithChildContext(codec, writer, field.getValue());
    }

    @Override
    public Class<Projection> getEncoderClass() {
        return Projection.class;
    }
}
