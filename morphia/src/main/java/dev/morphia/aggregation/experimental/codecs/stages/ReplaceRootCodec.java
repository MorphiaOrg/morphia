package dev.morphia.aggregation.experimental.codecs.stages;

import dev.morphia.aggregation.experimental.stages.ReplaceRoot;
import dev.morphia.mapping.Mapper;
import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

public class ReplaceRootCodec extends StageCodec<ReplaceRoot> {
    public ReplaceRootCodec(final Mapper mapper) {
        super(mapper);
    }

    @Override
    public Class<ReplaceRoot> getEncoderClass() {
        return ReplaceRoot.class;
    }

    @Override
    protected void encodeStage(final BsonWriter writer, final ReplaceRoot replace, final EncoderContext encoderContext) {
        writer.writeStartDocument();
        writer.writeName("newRoot");
        if(replace.getValue() != null) {
            replace.getValue().encode(getMapper(), writer, encoderContext);
        } else {
            replace.getDocument().encode(getMapper(), writer, encoderContext);
        }
        writer.writeEndDocument();
    }
}
