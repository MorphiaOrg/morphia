package dev.morphia.aggregation.experimental.codecs.stages;

import dev.morphia.aggregation.experimental.expressions.impls.Expression;
import dev.morphia.aggregation.experimental.stages.Unset;
import dev.morphia.mapping.Mapper;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.EncoderContext;

import java.util.List;

public class UnsetCodec extends StageCodec<Unset> {
    public UnsetCodec(Mapper mapper) {
        super(mapper);
    }

    @Override
    public Class<Unset> getEncoderClass() {
        return Unset.class;
    }

    @Override
    protected void encodeStage(BsonWriter writer, Unset value, EncoderContext encoderContext) {
        List<Expression> fields = value.getFields();
        if (fields.size() == 1) {
            fields.get(0).encode(getMapper(), writer, encoderContext);
        } else if (fields.size() > 1) {
            Codec codec = getCodecRegistry().get(fields.getClass());
            encoderContext.encodeWithChildContext(codec, writer, fields);
        }
    }
}
