package dev.morphia.config;

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
            switch (value) {
                case "className":
                    return className();
                case "lowerClassName":
                    return lowerClassName();
                case "lowerSimpleName":
                    return lowerSimpleName();
                case "simpleName":
                    return simpleName();
                default:
                    ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
                    return (DiscriminatorFunction) Class.forName(value, true, classLoader).getDeclaredConstructor()
                            .newInstance();
            }
        } catch (ReflectiveOperationException e) {
            throw new MappingException(e.getMessage(), e);
        }
    }
}
