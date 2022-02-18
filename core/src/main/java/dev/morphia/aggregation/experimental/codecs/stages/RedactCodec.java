package dev.morphia.aggregation.experimental.codecs.stages;

import dev.morphia.Datastore;
import dev.morphia.aggregation.experimental.stages.Redact;
import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import static dev.morphia.aggregation.experimental.codecs.ExpressionHelper.wrapExpression;

public class RedactCodec extends StageCodec<Redact> {
    public RedactCodec(Datastore datastore) {
        super(datastore);
    }

    @Override
    public Class<Redact> getEncoderClass() {
        return Redact.class;
    }

    @Override
    protected void encodeStage(BsonWriter writer, Redact value, EncoderContext encoderContext) {
        wrapExpression(getDatastore(), writer, value.getExpression(), encoderContext);
    }
}
