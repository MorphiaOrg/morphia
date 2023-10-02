package dev.morphia.aggregation.codecs.stages;

import java.util.List;

import dev.morphia.MorphiaDatastore;
import dev.morphia.aggregation.expressions.impls.Expression;
import dev.morphia.aggregation.stages.Unset;

import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.EncoderContext;

import static dev.morphia.mapping.codec.expressions.ExpressionCodecHelper.encodeIfNotNull;

public class UnsetCodec extends StageCodec<Unset> {
    public UnsetCodec(MorphiaDatastore datastore) {
        super(datastore);
    }

    @Override
    public Class<Unset> getEncoderClass() {
        return Unset.class;
    }

    @Override
    protected void encodeStage(BsonWriter writer, Unset value, EncoderContext encoderContext) {
        List<Expression> fields = value.getFields();
        if (fields.size() == 1) {
            encodeIfNotNull(getCodecRegistry(), writer,fields.get(0), encoderContext);
        } else if (fields.size() > 1) {
            Codec codec = getCodecRegistry().get(fields.getClass());
            encoderContext.encodeWithChildContext(codec, writer, fields);
        }
    }
}
