package dev.morphia.aggregation.experimental.codecs.stages;

import dev.morphia.aggregation.experimental.stages.AddFields;
import dev.morphia.mapping.Mapper;
import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

public class AddFieldsCodec extends StageCodec<AddFields> {
    public AddFieldsCodec(final Mapper mapper) {
        super(mapper);
    }

    @Override
    public Class<AddFields> getEncoderClass() {
        return AddFields.class;
    }

    @Override
    protected void encodeStage(final BsonWriter writer, final AddFields value, final EncoderContext encoderContext) {
        value.getDocument().encode(getMapper(), writer, encoderContext);
    }
}
