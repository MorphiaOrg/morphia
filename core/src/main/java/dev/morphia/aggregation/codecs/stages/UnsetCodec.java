package dev.morphia.aggregation.codecs.stages;

import dev.morphia.Datastore;
import dev.morphia.aggregation.expressions.impls.Expression;
import dev.morphia.aggregation.stages.Unset;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.EncoderContext;

import java.util.List;

public class UnsetCodec extends StageCodec<Unset> {
    public UnsetCodec(Datastore datastore) {
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
            fields.get(0).encode(getDatastore(), writer, encoderContext);
        } else if (fields.size() > 1) {
            Codec codec = getCodecRegistry().get(fields.getClass());
            encoderContext.encodeWithChildContext(codec, writer, fields);
        }
    }
}
