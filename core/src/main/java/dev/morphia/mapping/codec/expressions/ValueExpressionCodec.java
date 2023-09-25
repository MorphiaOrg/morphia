package dev.morphia.mapping.codec.expressions;

import dev.morphia.aggregation.expressions.impls.ValueExpression;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecRegistry;

public class ValueExpressionCodec extends BaseExpressionCodec<ValueExpression> {
    private CodecRegistry codecRegistry;

    public ValueExpressionCodec(CodecRegistry codecRegistry) {
        this.codecRegistry = codecRegistry;
    }

    @Override
    public void encode(BsonWriter writer, ValueExpression value, EncoderContext encoderContext) {
        Object object = value.object();
        if (object != null) {
            Codec codec = codecRegistry.get(object.getClass());
            encoderContext.encodeWithChildContext(codec, writer, object);
        } else {
            writer.writeNull();
        }

    }

    @Override
    public Class<ValueExpression> getEncoderClass() {
        return ValueExpression.class;
    }
}
