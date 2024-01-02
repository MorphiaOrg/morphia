package dev.morphia.config;

import com.mongodb.lang.Nullable;

import dev.morphia.annotations.internal.MorphiaInternal;
import dev.morphia.mapping.MappingException;

import org.eclipse.microprofile.config.spi.Converter;

/**
 * @hidden
 * @morphia.internal
 */
@MorphiaInternal
@SuppressWarnings("unchecked")
public class ClassNameConverter<T> implements Converter<T> {
    @Override
    @Nullable
    public T convert(String value) throws IllegalArgumentException, NullPointerException {
        return loadClass(value);
    }

    @Nullable
    private T loadClass(@Nullable String value) {
        try {
            return value == null || value.trim().isEmpty()
                    ? null
                    : ((Class<T>) Class.forName(value)).getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new MappingException(e.getMessage(), e);
        }
    }
}
