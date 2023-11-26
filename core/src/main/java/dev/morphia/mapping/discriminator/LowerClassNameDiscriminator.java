package dev.morphia.mapping.discriminator;

import dev.morphia.mapping.DiscriminatorFunction;

public class LowerClassNameDiscriminator extends DiscriminatorFunction {
    @Override
    public String compute(Class<?> type) {
        return type.getName().toLowerCase();
    }
}
