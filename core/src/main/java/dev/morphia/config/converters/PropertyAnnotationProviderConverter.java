package dev.morphia.config.converters;

import dev.morphia.annotations.internal.MorphiaInternal;
import dev.morphia.config.PropertyAnnotationProvider;
import dev.morphia.mapping.MappingException;

import org.eclipse.microprofile.config.spi.Converter;

/**
 * @hidden
 * @since 3.0
 * @morphia.internal
 */
@MorphiaInternal
@SuppressWarnings("unchecked")
public class PropertyAnnotationProviderConverter implements Converter<PropertyAnnotationProvider<?>> {
    @Override
    public PropertyAnnotationProvider<?> convert(String value) {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        try {
            return (PropertyAnnotationProvider<?>) Class.forName(value.trim(), true, classLoader)
                    .getConstructor()
                    .newInstance();
        } catch (ReflectiveOperationException e) {
            throw new MappingException(e.getMessage(), e);
        }
    }
}
