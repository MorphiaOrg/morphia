package dev.morphia.aggregation.experimental.codecs.expressions;

import dev.morphia.aggregation.experimental.stages.Expression.Literal;
import dev.morphia.mapping.Mapper;
import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecRegistry;

public class ExpressionLiteralCodec extends ExpressionCodec<Literal> {
    public ExpressionLiteralCodec(final Mapper mapper) {
        super(mapper, Literal.class);
    }

    @Override
    public void encode(final BsonWriter writer, final Literal expression, final EncoderContext encoderContext) {
        Object value = expression.getValue();
        Codec codec = getCodecRegistry().get(value.getClass());
        encoderContext.encodeWithChildContext(codec, writer, value);
    }
}
