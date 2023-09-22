package dev.morphia.aggregation.codecs.stages;

import dev.morphia.MorphiaDatastore;
import dev.morphia.aggregation.stages.ReplaceRoot;

import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import static dev.morphia.aggregation.codecs.ExpressionHelper.document;
import static dev.morphia.aggregation.codecs.ExpressionHelper.wrapExpression;

public class ReplaceRootCodec extends StageCodec<ReplaceRoot> {
    public ReplaceRootCodec(MorphiaDatastore datastore) {
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
                wrapExpression(getDatastore(), writer, replace.getValue(), encoderContext);
            } else {
                replace.getDocument().encode(getDatastore(), writer, encoderContext);
            }
        });
    }
}
