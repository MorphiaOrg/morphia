package dev.morphia.aggregation.codecs.stages;

import dev.morphia.MorphiaDatastore;
import dev.morphia.aggregation.expressions.impls.DocumentExpression;
import dev.morphia.aggregation.expressions.impls.Expression;
import dev.morphia.aggregation.stages.ReplaceWith;

import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.EncoderContext;

import static dev.morphia.aggregation.codecs.ExpressionHelper.wrapExpression;

public class ReplaceWithCodec extends StageCodec<ReplaceWith> {
    public ReplaceWithCodec(MorphiaDatastore datastore) {
        super(datastore);
    }

    @Override
    public Class<ReplaceWith> getEncoderClass() {
        return ReplaceWith.class;
    }

    @Override
    protected void encodeStage(BsonWriter writer, ReplaceWith replace, EncoderContext encoderContext) {
        Expression value = replace.getValue();

        if (value == null) {
            value = replace.getDocument();
        }

        Codec codec = getDatastore().getCodecRegistry().get(value.getClass());
        codec.encode(writer, value, encoderContext);

    }
}
