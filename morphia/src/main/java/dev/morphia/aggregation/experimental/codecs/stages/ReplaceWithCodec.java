package dev.morphia.aggregation.experimental.codecs.stages;

import dev.morphia.mapping.Mapper;
import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

public class ReplaceWithCodec extends StageCodec<ReplaceWith> {
    public ReplaceWithCodec(final Mapper mapper) {
        super(mapper);
    }

    @Override
    public Class<ReplaceWith> getEncoderClass() {
        return ReplaceWith.class;
    }

    @Override
    protected void encodeStage(final BsonWriter writer, final ReplaceWith replace, final EncoderContext encoderContext) {
        if(replace.getValue() != null) {
            replace.getValue().encode(getMapper(), writer, encoderContext);
        } else {
            replace.getDocument().encode(getMapper(), writer, encoderContext);
        }
    }
}
