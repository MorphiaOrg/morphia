package dev.morphia.mapping.codec

import com.mongodb.lang.Nullable
import dev.morphia.mapping.codec.pojo.TypeData
import org.bson.codecs.Codec
import org.bson.codecs.configuration.CodecConfigurationException
import org.bson.codecs.pojo.PropertyCodecRegistry
import org.bson.codecs.pojo.TypeWithTypeParameters
import kotlin.properties.ReadWriteProperty

/**
 * @morphia.internal
 * @since 2.2
 */
@Suppress("UNCHECKED_CAST")
class ReadWritePropertyCodecProvider : MorphiaPropertyCodecProvider() {
    @Nullable
    override fun <T> get(type: TypeWithTypeParameters<T>, registry: PropertyCodecRegistry): Codec<T>? {
        if (ReadWriteProperty::class.java.isAssignableFrom(type.type)) {
            val typeParameters = type.typeParameters
            val valueType = getType(typeParameters, 0)
            return try {
                val codec: Codec<Any> = registry[valueType] as Codec<Any>
                ReadWritePropertyCodec<Any>(codec) as Codec<T>
            } catch (e: CodecConfigurationException) {
                if (valueType.type == Any::class.java) {
                    try {
                        return registry.get(TypeData.builder(Collection::class.java).build()) as Codec<T>
                    } catch (_: CodecConfigurationException) {
                        // Ignore and return original exception
                    }
                }
                throw e
            }
        }
        return null
    }
}
