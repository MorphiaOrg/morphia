package dev.morphia.mapping.codec;

import org.bson.codecs.Codec;
import org.bson.codecs.pojo.PropertyCodecProvider;
import org.bson.codecs.pojo.PropertyCodecRegistry;
import org.bson.codecs.pojo.TypeWithTypeParameters;

import java.util.Collection;

final class CollectionPropertyCodecProvider implements PropertyCodecProvider {
    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public <T> Codec<T> get(TypeWithTypeParameters<T> type, PropertyCodecRegistry registry) {
        if (Collection.class.isAssignableFrom(type.getType()) && type.getTypeParameters().size() == 1) {
            return new CollectionCodec(type.getType(), registry.get(type.getTypeParameters().get(0)));
        } else {
            return null;
        }
    }
}
