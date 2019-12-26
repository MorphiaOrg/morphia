package dev.morphia.aggregation.experimental.codecs.expressions;

import dev.morphia.aggregation.experimental.stages.Expression;
import dev.morphia.mapping.Mapper;
import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecRegistry;

public class ExpressionCodec<T extends Expression> implements Codec<T> {
    private final Mapper mapper;
    private final Class<T> encoderClass;

    public ExpressionCodec(final Mapper mapper, final Class<T> encoderClass) {
        this.mapper = mapper;
        this.encoderClass = encoderClass;
    }

    @Override
    public final T decode(final BsonReader reader, final DecoderContext decoderContext) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void encode(final BsonWriter writer, final T expression, final EncoderContext encoderContext) {
        writer.writeStartDocument(expression.getName());
        writer.writeName(expression.getOperation());
        Object value = expression.getValue();
        Codec codec = getCodecRegistry().get(value.getClass());
        encoderContext.encodeWithChildContext(codec, writer, value);
        writer.writeEndDocument();
    }

    @Override
    public final Class<T> getEncoderClass() {
        return encoderClass;
    }

    protected CodecRegistry getCodecRegistry() {
        return mapper.getCodecRegistry();
    }

    protected Mapper getMapper() {
        return mapper;
    }
}
