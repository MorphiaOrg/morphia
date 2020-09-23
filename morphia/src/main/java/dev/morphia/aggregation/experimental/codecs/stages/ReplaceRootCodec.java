package dev.morphia.aggregation.experimental.codecs.stages;

import dev.morphia.aggregation.experimental.stages.ReplaceRoot;
import dev.morphia.mapping.Mapper;
import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import static dev.morphia.aggregation.experimental.codecs.ExpressionHelper.document;

public class ReplaceRootCodec extends StageCodec<ReplaceRoot> {
    public ReplaceRootCodec(Mapper mapper) {
        super(mapper);
    }

    @Override
    public Class<ReplaceRoot> getEncoderClass() {
        return ReplaceRoot.class;
    }

    @Override
    protected void encodeStage(BsonWriter writer, ReplaceRoot replace, EncoderContext encoderContext) {
        document(writer, () -> {
            writer.writeName("newRoot");
            if (replace.getValue() != null) {
                replace.getValue().encode(getMapper(), writer, encoderContext);
            } else {
                replace.getDocument().encode(getMapper(), writer, encoderContext);
            }
        });
    }
}
