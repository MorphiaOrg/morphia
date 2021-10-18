package dev.morphia.aggregation.experimental.codecs.stages;

import dev.morphia.Datastore;
import dev.morphia.aggregation.experimental.stages.ReplaceRoot;
import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import static dev.morphia.aggregation.experimental.codecs.ExpressionHelper.document;

public class ReplaceRootCodec extends StageCodec<ReplaceRoot> {
    public ReplaceRootCodec(Datastore datastore) {
        super(datastore);
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
                replace.getValue().encode(getDatastore(), writer, encoderContext);
            } else {
                replace.getDocument().encode(getDatastore(), writer, encoderContext);
            }
        });
    }
}
