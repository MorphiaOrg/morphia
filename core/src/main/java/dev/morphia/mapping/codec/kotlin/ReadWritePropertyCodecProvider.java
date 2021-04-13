package dev.morphia.mapping.codec.kotlin;

import com.mongodb.lang.Nullable;
import dev.morphia.mapping.codec.MorphiaPropertyCodecProvider;
import dev.morphia.mapping.codec.pojo.TypeData;
import kotlin.properties.ReadWriteProperty;
import org.bson.codecs.Codec;
import org.bson.codecs.configuration.CodecConfigurationException;
import org.bson.codecs.pojo.PropertyCodecRegistry;
import org.bson.codecs.pojo.TypeWithTypeParameters;

import java.util.Collection;
import java.util.List;

/**
 * @morphia.internal
 * @since 2.2
 */
@SuppressWarnings("unchecked")
public class ReadWritePropertyCodecProvider extends MorphiaPropertyCodecProvider {
    @Override
    @Nullable
    @SuppressWarnings("rawtypes")
    public <T> Codec<T> get(TypeWithTypeParameters<T> type, PropertyCodecRegistry registry) {
        if (ReadWriteProperty.class.isAssignableFrom(type.getType())) {
            final List<? extends TypeWithTypeParameters<?>> typeParameters = type.getTypeParameters();
            TypeWithTypeParameters<?> valueType = getType(typeParameters, 0);

            try {
                Codec codec = registry.get(valueType);
                return (Codec<T>) new ReadWritePropertyCodec(codec);
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
