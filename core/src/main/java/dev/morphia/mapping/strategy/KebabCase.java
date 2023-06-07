package dev.morphia.mapping.strategy;

import java.util.List;
import java.util.StringJoiner;

import dev.morphia.mapping.NamingStrategy;

import static dev.morphia.mapping.strategy.SnakeCase.groupByCapitals;

public class KebabCase extends NamingStrategy {
    @Override
    public String apply(String value) {
        List<String> groups = groupByCapitals(value);

        StringJoiner joiner = new StringJoiner("-");
        joiner.add(groups.get(0).toLowerCase());
        for (int i = 1; i < groups.size(); i++) {
            joiner.add(groups.get(i).toLowerCase());
        }
        return joiner.toString();
    }
}
