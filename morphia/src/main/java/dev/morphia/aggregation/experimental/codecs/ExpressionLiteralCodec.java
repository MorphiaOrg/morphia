package dev.morphia.aggregation.experimental.codecs;

import dev.morphia.aggregation.experimental.stages.Expression.Literal;
import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecRegistry;

public class ExpressionLiteralCodec implements Codec<Literal> {
    private CodecRegistry codecRegistry;

    public ExpressionLiteralCodec(final CodecRegistry codecRegistry) {
        this.codecRegistry = codecRegistry;
    }

    @Override
    public Literal decode(final BsonReader reader, final DecoderContext decoderContext) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void encode(final BsonWriter writer, final Literal expression, final EncoderContext encoderContext) {
        Object value = expression.getValue();
        Codec codec = codecRegistry.get(value.getClass());
        encoderContext.encodeWithChildContext(codec, writer, value);
    }

    @Override
    public Class<Literal> getEncoderClass() {
        return Literal.class;
    }
}
