package dev.morphia.aggregation.experimental.expressions.impls;

import dev.morphia.mapping.Mapper;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.EncoderContext;

public class ValueExpression extends Expression {
    public ValueExpression(Object value) {
        super(null, value);
    }

    @Override
    public void encode(Mapper mapper, BsonWriter writer, EncoderContext encoderContext) {
        if (getValue() != null) {
            Codec codec = mapper.getCodecRegistry().get(getValue().getClass());
            encoderContext.encodeWithChildContext(codec, writer, getValue());
        } else {
            writer.writeNull();
        }
    }
}
