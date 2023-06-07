package dev.morphia.mapping.strategy;

import dev.morphia.mapping.NamingStrategy;

public class LowerCase extends NamingStrategy {
    @Override
    public String apply(String value) {
        return value.toLowerCase();
    }

}
