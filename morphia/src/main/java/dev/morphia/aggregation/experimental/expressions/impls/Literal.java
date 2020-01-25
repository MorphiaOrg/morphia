package dev.morphia.aggregation.experimental.expressions.impls;

import dev.morphia.aggregation.experimental.expressions.Expression;
import dev.morphia.mapping.Mapper;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.EncoderContext;

public class Literal extends Expression {
    public Literal(final Object value) {
        super("$literal", value);
    }

    @Override
    public void encode(final Mapper mapper, final BsonWriter writer, final EncoderContext encoderContext) {
        writer.writeStartDocument();
        writer.writeName(getOperation());
        Codec codec = mapper.getCodecRegistry().get(getValue().getClass());
        encoderContext.encodeWithChildContext(codec, writer, getValue());
        writer.writeEndDocument();
    }
}
