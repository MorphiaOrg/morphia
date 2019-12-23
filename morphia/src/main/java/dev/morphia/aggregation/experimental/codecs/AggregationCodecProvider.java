package dev.morphia.aggregation.experimental.codecs;

import dev.morphia.aggregation.experimental.stages.Expression;
import dev.morphia.aggregation.experimental.stages.Expression.Literal;
import dev.morphia.aggregation.experimental.stages.Group;
import org.bson.codecs.Codec;
import org.bson.codecs.configuration.CodecProvider;
import org.bson.codecs.configuration.CodecRegistry;

public class AggregationCodecProvider implements CodecProvider {
    @Override
    public <T> Codec<T> get(final Class<T> clazz, final CodecRegistry registry) {
        Codec codec = null;

        if(Group.class.isAssignableFrom(clazz)) {
            codec = new GroupCodec(registry);
        } else if(Literal.class.isAssignableFrom(clazz)) {
            codec = new ExpressionLiteralCodec(registry);
        } else if(Expression.class.isAssignableFrom(clazz)) {
            codec = new ExpressionCodec(registry);
        }

        return codec;
    }
}
