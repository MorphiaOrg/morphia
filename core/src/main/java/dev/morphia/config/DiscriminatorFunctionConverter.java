package dev.morphia.config;

import dev.morphia.mapping.DiscriminatorFunction;

import org.eclipse.microprofile.config.spi.Converter;

import static dev.morphia.mapping.DiscriminatorFunction.className;
import static dev.morphia.mapping.DiscriminatorFunction.lowerClassName;
import static dev.morphia.mapping.DiscriminatorFunction.lowerSimpleName;
import static dev.morphia.mapping.DiscriminatorFunction.simpleName;

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
                    return (DiscriminatorFunction) Class.forName(value).getDeclaredConstructor().newInstance();
            }
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }
}
