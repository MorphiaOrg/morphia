package dev.morphia.aggregation.codecs.stages;

import dev.morphia.MorphiaDatastore;
import dev.morphia.aggregation.stages.Redact;

import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import static dev.morphia.mapping.codec.expressions.ExpressionCodecHelper.encodeIfNotNull;

public class RedactCodec extends StageCodec<Redact> {
    public RedactCodec(MorphiaDatastore datastore) {
        super(datastore);
    }

    @Override
    public Class<Redact> getEncoderClass() {
        return Redact.class;
    }

    @Override
    protected void encodeStage(BsonWriter writer, Redact value, EncoderContext encoderContext) {
        encodeIfNotNull(getDatastore().getCodecRegistry(), writer, value.getExpression(), encoderContext);
    }
}
