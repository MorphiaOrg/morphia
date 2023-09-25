package dev.morphia.mapping.codec.expressions;


import dev.morphia.aggregation.expressions.impls.Expression;
import org.bson.codecs.Codec;
import org.bson.codecs.configuration.CodecProvider;
import org.bson.codecs.configuration.CodecRegistry;

import java.util.HashMap;
import java.util.Map;

/**
 * @hidden
 * @morphia.internal
 * @since 3.0
 */
public class MorphiaExpressionCodecProvider implements CodecProvider {
    private final Map<Class<?>, Codec<?>> codecs = new HashMap<>();

    public MorphiaExpressionCodecProvider(CodecRegistry codecRegistry) {
        addCodec(new ValueExpressionCodec(codecRegistry));
        addCodec(new IfNullCodec(codecRegistry));
    }

    @Override
    public <T> Codec<T> get(Class<T> clazz, CodecRegistry registry) {
        Codec<T> codec = (Codec<T>) codecs.get(clazz);

        if (codec == null && Expression.class.isAssignableFrom(clazz)) {
            throw new UnsupportedOperationException(clazz.getName() + " needs a codec");
        }
        return codec;
    }

    protected <T> void addCodec(BaseExpressionCodec<T> codec) {
        codecs.put(codec.getEncoderClass(), codec);
    }

}
