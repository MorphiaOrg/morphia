package dev.morphia.mapping.strategy;

import dev.morphia.mapping.NamingStrategy;

public class Identity extends NamingStrategy {
    @Override
    public String apply(String value) {
        return value;
    }

}
