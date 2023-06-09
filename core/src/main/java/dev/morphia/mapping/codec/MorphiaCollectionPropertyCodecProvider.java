package dev.morphia.mapping.codec;

import java.util.Collection;
import java.util.List;

import com.mongodb.lang.Nullable;

import dev.morphia.annotations.internal.MorphiaInternal;
import dev.morphia.mapping.codec.pojo.TypeData;

import org.bson.codecs.Codec;
import org.bson.codecs.configuration.CodecConfigurationException;
import org.bson.codecs.pojo.PropertyCodecRegistry;
import org.bson.codecs.pojo.TypeWithTypeParameters;

/**
 * A provider for collection types
 * 
 * @hidden
 * @morphia.internal
 */
@MorphiaInternal
@SuppressWarnings("unchecked")
public class MorphiaCollectionPropertyCodecProvider extends MorphiaPropertyCodecProvider {
    @Nullable
    @Override
    public <T> Codec<T> get(TypeWithTypeParameters<T> type, PropertyCodecRegistry registry) {
        if (Collection.class.isAssignableFrom(type.getType())) {
            final List<? extends TypeWithTypeParameters<?>> typeParameters = type.getTypeParameters();
            TypeWithTypeParameters<?> valueType = getType(typeParameters, 0);

            try {
                return new MorphiaCollectionCodec(registry.get(valueType), type.getType());
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
