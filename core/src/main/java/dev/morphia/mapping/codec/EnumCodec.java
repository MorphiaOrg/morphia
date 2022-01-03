package dev.morphia.mapping.codec;

import dev.morphia.annotations.Property;
import org.bson.BsonReader;
import org.bson.BsonType;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * An enum codec
 *
 * @param <T> the type of the enum
 * @morphia.internal
 * @since 2.0
 */
public class EnumCodec<T extends Enum<T>> implements Codec<T> {
    private final Class<T> type;
    private final Map<String, T> enumByMappedValue;
    private final Map<T, String> mappedValueByEnum;

    /**
     * Creates a codec for the given type
     *
     * @param type the type
     */
    public EnumCodec(Class<T> type) {
        this.type = type;
        this.enumByMappedValue = new HashMap<>();
        this.mappedValueByEnum = new HashMap<>();
        Arrays.asList(type.getEnumConstants())
            .forEach(
                enumConst -> {
                    final String dbValue = getPropertyValue(type, enumConst.name())
                        .map(Property::value)
                        .orElse(enumConst.name());
                    enumByMappedValue.put(dbValue, enumConst);
                    mappedValueByEnum.put(enumConst, dbValue);
                }
            );
    }

    @Override
    public void encode(BsonWriter writer, T value, EncoderContext encoderContext) {
        writer.writeString(
            Optional.ofNullable(mappedValueByEnum.get(value))
                .orElseThrow(() -> new IllegalStateException("Failed to get mapped value of " + value.name()))
        );
    }

    @Override
    public T decode(BsonReader reader, DecoderContext decoderContext) {
        if (reader.getCurrentBsonType() == BsonType.NULL) {
            return null;
        }
        if (reader.getCurrentBsonType() == BsonType.STRING) {
            final String mappedValue = reader.readString();
            return Optional.ofNullable(enumByMappedValue.get(mappedValue))
                .orElseThrow(() -> new IllegalStateException(String.format("Failed to get enum %s by mapped value %s", type.getName(), mappedValue)));
        }
        throw new IllegalStateException(String.format("Failed to read enum %s. Unexpected bson type %s", type.getName(), reader.getCurrentBsonType().name()));
    }

    @Override
    public Class<T> getEncoderClass() {
        return type;
    }

    private Optional<Property> getPropertyValue(final Class<T> enumClass, final String constName) {
        try {
            return Optional.ofNullable(enumClass.getDeclaredField(constName).getAnnotation(Property.class));
        } catch (NoSuchFieldException e) {
            throw new IllegalStateException("Failed to get property annotation of " + constName, e);
        }
    }
}
