package dev.morphia.config.converters;

import dev.morphia.annotations.internal.MorphiaInternal;
import dev.morphia.mapping.DiscriminatorFunction;
import dev.morphia.mapping.MappingException;

import org.eclipse.microprofile.config.spi.Converter;

import static dev.morphia.mapping.DiscriminatorFunction.className;
import static dev.morphia.mapping.DiscriminatorFunction.lowerClassName;
import static dev.morphia.mapping.DiscriminatorFunction.lowerSimpleName;
import static dev.morphia.mapping.DiscriminatorFunction.simpleName;

/**
 * @hidden
 * @morphia.internal
 */
@MorphiaInternal
public class DiscriminatorFunctionConverter implements Converter<DiscriminatorFunction> {
    @Override
    public DiscriminatorFunction convert(String value) throws IllegalArgumentException, NullPointerException {
        try {
            return switch (value) {
                case "className" -> className();
                case "lowerClassName" -> lowerClassName();
                case "lowerSimpleName" -> lowerSimpleName();
                case "simpleName" -> simpleName();
                default -> (DiscriminatorFunction) Class.forName(value, true, Thread.currentThread().getContextClassLoader()).getDeclaredConstructor().newInstance();
            };
        } catch (ReflectiveOperationException e) {
            throw new MappingException(e.getMessage(), e);
        }
    }
}
