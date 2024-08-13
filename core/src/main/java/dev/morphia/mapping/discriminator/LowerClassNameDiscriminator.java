package dev.morphia.mapping.discriminator;

import dev.morphia.mapping.DiscriminatorFunction;

public class LowerClassNameDiscriminator extends DiscriminatorFunction {
    @Override
    public String compute(String type) {
        return type.toLowerCase();
    }
}
