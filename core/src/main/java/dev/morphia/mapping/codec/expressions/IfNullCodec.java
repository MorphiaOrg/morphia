package dev.morphia.mapping.codec.expressions;

import dev.morphia.aggregation.expressions.impls.IfNull;
import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecRegistry;


public class IfNullCodec extends BaseExpressionCodec<IfNull> {
    private final CodecRegistry codecRegistry;

    public IfNullCodec(CodecRegistry codecRegistry) {
        this.codecRegistry = codecRegistry;
    }

    @Override
    public void encode(BsonWriter writer, IfNull value, EncoderContext encoderContext) {
        array(writer, value.operation(), () -> {
            encodeIfNotNull(codecRegistry, writer, value.target(), encoderContext);
            encodeIfNotNull(codecRegistry, writer, value.replacement(), encoderContext);
            encodeIfNotNull(codecRegistry, writer, value.document(), encoderContext);
        });

    }

    @Override
    public Class<IfNull> getEncoderClass() {
        return IfNull.class;
    }
}
