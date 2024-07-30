package dev.morphia.config.converters;

import java.util.ArrayList;
import java.util.List;

import dev.morphia.annotations.internal.MorphiaInternal;
import dev.morphia.config.MorphiaPropertyAnnotationProvider;
import dev.morphia.config.PropertyAnnotationProvider;
import dev.morphia.mapping.MappingException;

import org.eclipse.microprofile.config.spi.Converter;

import static java.util.Arrays.asList;

/**
 * @hidden
 * @since 3.0
 * @morphia.internal
 */
@MorphiaInternal
@SuppressWarnings("unchecked")
public class PropertyAnnotationProviderConverter implements Converter<List<PropertyAnnotationProvider<?>>> {
    @Override
    public List<PropertyAnnotationProvider<?>> convert(String value) {
        List<String> list = new ArrayList<>(List.of(MorphiaPropertyAnnotationProvider.class.getName()));
        list.addAll(asList(value.split(",")));
        return (List<PropertyAnnotationProvider<?>>) list.stream().distinct()
                .map(s -> {
                    try {
                        return Class.forName(s.trim()).getConstructor().newInstance();
                    } catch (ReflectiveOperationException e) {
                        throw new MappingException(e.getMessage(), e);
                    }
                });
    }
}
