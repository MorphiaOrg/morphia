package dev.morphia.mapping.discriminator;

import dev.morphia.mapping.DiscriminatorFunction;
import dev.morphia.mapping.Mapper;

public class LowerSimpleNameDiscriminator extends DiscriminatorFunction {
    @Override
    public String compute(String type) {
        return Mapper.simpleName(type).toLowerCase();
    }
}
