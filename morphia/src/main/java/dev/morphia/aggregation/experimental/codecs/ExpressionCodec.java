package dev.morphia.aggregation.experimental.codecs;

import dev.morphia.aggregation.experimental.stages.Expression;
import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecRegistry;

public class ExpressionCodec implements Codec<Expression> {
    private CodecRegistry codecRegistry;

    public ExpressionCodec(final CodecRegistry codecRegistry) {
        this.codecRegistry = codecRegistry;
    }

    @Override
    public Expression decode(final BsonReader reader, final DecoderContext decoderContext) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void encode(final BsonWriter writer, final Expression expression, final EncoderContext encoderContext) {
        writer.writeStartDocument(expression.getName());
        writer.writeName(expression.getOperation());
        Object value = expression.getValue();
        Codec codec = codecRegistry.get(value.getClass());
        encoderContext.encodeWithChildContext(codec, writer, value);
        writer.writeEndDocument();
    }

    @Override
    public Class<Expression> getEncoderClass() {
        return Expression.class;
    }
}
