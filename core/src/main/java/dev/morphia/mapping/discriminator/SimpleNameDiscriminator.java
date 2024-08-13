package dev.morphia.mapping.discriminator;

import dev.morphia.mapping.DiscriminatorFunction;
import dev.morphia.mapping.Mapper;

public class SimpleNameDiscriminator extends DiscriminatorFunction {
    @Override
    public String compute(String type) {
        return Mapper.simpleName(type);
    }
}
