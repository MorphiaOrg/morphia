package dev.morphia.config.converters;

import dev.morphia.annotations.internal.MorphiaInternal;
import dev.morphia.mapping.MappingException;
import dev.morphia.mapping.NamingStrategy;

import org.eclipse.microprofile.config.spi.Converter;

import static dev.morphia.mapping.NamingStrategy.camelCase;
import static dev.morphia.mapping.NamingStrategy.identity;
import static dev.morphia.mapping.NamingStrategy.kebabCase;
import static dev.morphia.mapping.NamingStrategy.lowerCase;
import static dev.morphia.mapping.NamingStrategy.snakeCase;

/**
 * @hidden
 * @morphia.internal
 */
@MorphiaInternal
public class NamingStrategyConverter implements Converter<NamingStrategy> {
    @Override
    public NamingStrategy convert(String value) throws IllegalArgumentException, NullPointerException {
        try {
            switch (value) {
                case "camelCase":
                    return camelCase();
                case "identity":
                    return identity();
                case "kebabCase":
                    return kebabCase();
                case "lowerCase":
                    return lowerCase();
                case "snakeCase":
                    return snakeCase();
                default:
                    ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
                    return (NamingStrategy) Class.forName(value, true, classLoader)
                            .getDeclaredConstructor()
                            .newInstance();
            }
        } catch (ReflectiveOperationException e) {
            throw new MappingException(e.getMessage(), e);
        }
    }
}
