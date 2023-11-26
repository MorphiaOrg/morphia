package dev.morphia.mapping.discriminator;

import dev.morphia.mapping.DiscriminatorFunction;

public class SimpleNameDiscriminator extends DiscriminatorFunction {
    @Override
    public String compute(Class<?> type) {
        return type.getSimpleName();
    }
}
