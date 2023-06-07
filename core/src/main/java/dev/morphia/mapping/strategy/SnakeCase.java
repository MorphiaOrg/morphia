package dev.morphia.mapping.strategy;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

import dev.morphia.mapping.NamingStrategy;

public class SnakeCase extends NamingStrategy {
    @Override
    public String apply(String value) {
        List<String> groups = groupByCapitals(value);

        StringJoiner joiner = new StringJoiner("_");
        for (String group : groups) {
            joiner.add(group.toLowerCase());
        }
        return joiner.toString();
    }

    public static List<String> groupByCapitals(String value) {
        List<String> groups = new ArrayList<>();
        StringBuilder builder = null;
        int index = 0;
        int length = value.length();

        while (index < length) {
            char current = value.charAt(index);
            if (index == 0 || Character.isUpperCase(current)) {
                if (builder != null) {
                    groups.add(builder.toString());
                }
                builder = new StringBuilder();
            }
            builder.append(current);
            index++;
        }
        groups.add(builder.toString());

        return groups;
    }

}
