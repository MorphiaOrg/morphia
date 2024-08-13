package dev.morphia.mapping.discriminator;

import dev.morphia.mapping.DiscriminatorFunction;

public class ClassNameDiscriminator extends DiscriminatorFunction {
    @Override
    public String compute(String type) {
        return type;
    }
}
