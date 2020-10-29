package dev.morphia.mapping.codec;

import dev.morphia.mapping.codec.pojo.TypeData;
import org.bson.codecs.Codec;
import org.bson.codecs.configuration.CodecConfigurationException;
import org.bson.codecs.pojo.PropertyCodecRegistry;
import org.bson.codecs.pojo.TypeWithTypeParameters;

import java.util.Collection;
import java.util.List;

/**
 * A provider for collection types
 */
@SuppressWarnings("unchecked")
public class MorphiaCollectionPropertyCodecProvider extends MorphiaPropertyCodecProvider {
    @Override
    public <T> Codec<T> get(TypeWithTypeParameters<T> type, PropertyCodecRegistry registry) {
        if (Collection.class.isAssignableFrom(type.getType())) {
            final List<? extends TypeWithTypeParameters<?>> typeParameters = type.getTypeParameters();
            TypeWithTypeParameters<?> valueType = getType(typeParameters, 0);

            try {
                return new MorphiaCollectionCodec(type, registry, valueType);
            } catch (CodecConfigurationException e) {
                if (valueType.getType().equals(Object.class)) {
                    try {
                        return (Codec<T>) registry.get(TypeData.builder(Collection.class).build());
                    } catch (CodecConfigurationException e1) {
                        // Ignore and return original exception
                    }
                }
                throw e;
            }
        }

        return null;
    }

}
