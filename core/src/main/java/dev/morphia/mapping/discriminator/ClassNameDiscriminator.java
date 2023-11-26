package dev.morphia.mapping.discriminator;

import dev.morphia.mapping.DiscriminatorFunction;

public class ClassNameDiscriminator extends DiscriminatorFunction {
    @Override
    public String compute(Class<?> type) {
        return type.getName();
    }
}
