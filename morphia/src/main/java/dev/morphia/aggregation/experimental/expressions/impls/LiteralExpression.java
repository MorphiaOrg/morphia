package dev.morphia.aggregation.experimental.expressions.impls;

import dev.morphia.mapping.Mapper;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.EncoderContext;

public class LiteralExpression extends Expression {
    public LiteralExpression(Object value) {
        super("$literal", value);
    }

    @Override
    public void encode(Mapper mapper, BsonWriter writer, EncoderContext encoderContext) {
        writer.writeStartDocument();
        writer.writeName(getOperation());
        Codec codec = mapper.getCodecRegistry().get(getValue().getClass());
        encoderContext.encodeWithChildContext(codec, writer, getValue());
        writer.writeEndDocument();
    }
}
