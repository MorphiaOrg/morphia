package dev.morphia.aggregation.codecs.stages;

import dev.morphia.MorphiaDatastore;
import dev.morphia.aggregation.expressions.impls.DocumentExpression;
import dev.morphia.aggregation.stages.Set;

import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.EncoderContext;

public class SetStageCodec extends StageCodec<Set> {
    public SetStageCodec(MorphiaDatastore datastore) {
        super(datastore);
    }

    @Override
    public Class<Set> getEncoderClass() {
        return Set.class;
    }

    @Override
    protected void encodeStage(BsonWriter writer, Set value, EncoderContext encoderContext) {
        DocumentExpression document = value.getDocument();
        Codec<DocumentExpression> codec = getCodecRegistry().get(DocumentExpression.class);
        codec.encode(writer, document, encoderContext);
    }
}
