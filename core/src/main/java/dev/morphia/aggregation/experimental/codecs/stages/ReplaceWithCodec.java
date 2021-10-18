package dev.morphia.aggregation.experimental.codecs.stages;

import dev.morphia.Datastore;
import dev.morphia.aggregation.experimental.expressions.impls.Expression;
import dev.morphia.aggregation.experimental.stages.ReplaceWith;
import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

public class ReplaceWithCodec extends StageCodec<ReplaceWith> {
    public ReplaceWithCodec(Datastore datastore) {
        super(datastore);
    }

    @Override
    public Class<ReplaceWith> getEncoderClass() {
        return ReplaceWith.class;
    }

    @Override
    protected void encodeStage(BsonWriter writer, ReplaceWith replace, EncoderContext encoderContext) {
        Expression value = replace.getValue();
        if (value != null) {
            value.encode(getDatastore(), writer, encoderContext);
        } else {
            replace.getDocument().encode(getDatastore(), writer, encoderContext);
        }
    }
}
