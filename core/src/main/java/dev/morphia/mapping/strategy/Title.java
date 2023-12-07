package dev.morphia.mapping.strategy;

import dev.morphia.mapping.NamingStrategy;

public class Title extends NamingStrategy {
    @Override
    public String apply(String value) {
        return value.substring(0, 1).toUpperCase() + value.substring(1);
    }

}
