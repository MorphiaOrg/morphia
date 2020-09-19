package dev.morphia.aggregation.experimental.codecs.stages;

import dev.morphia.aggregation.experimental.stages.Out;
import dev.morphia.mapping.Mapper;
import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

public class OutCodec extends StageCodec<Out> {
    public OutCodec(Mapper mapper) {
        super(mapper);
    }

    @Override
    public Class<Out> getEncoderClass() {
        return Out.class;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void encodeStage(BsonWriter writer, Out value, EncoderContext encoderContext) {
        if(value.getType() != null) {
            writer.writeString(getMapper().getCollection(value.getType()).getNamespace().getCollectionName());
        } else {
            writer.writeString(value.getCollection());
        }
    }
}
