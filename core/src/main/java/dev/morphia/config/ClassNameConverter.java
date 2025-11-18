package dev.morphia.config;

import dev.morphia.annotations.internal.MorphiaInternal;
import dev.morphia.mapping.MappingException;

import org.eclipse.microprofile.config.spi.Converter;

/**
 * @hidden
 * @morphia.internal
 */
@MorphiaInternal
public class ClassNameConverter<T> implements Converter<Object> {
    @Override
    public Object convert(String value) throws IllegalArgumentException, NullPointerException {
        return loadClass(value);
    }

    static Object loadClass(String value) {
        if (value == null || value.trim().equals("")) {
            return null;
        }
        try {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            return Class.forName(value, true, classLoader).getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new MappingException(e.getMessage(), e);
        }
    }
}
