package dev.morphia.mapping;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

/**
 * Defines a naming strategy for use, e.g., in naming collections and fields
 *
 * @since 2.0
 */
public abstract class NamingStrategy {
    /**
     * Defines a naming strategy that returns the value passed
     *
     * @return the unchanged value.
     */
    public static NamingStrategy identity() {
        return new NamingStrategy() {
            @Override
            public String apply(String value) {
                return value;
            }
        };
    }

    /**
     * Defines a naming strategy that returns the lowercase form of the value passed
     *
     * @return the updated value.
     */
    public static NamingStrategy lowerCase() {
        return new NamingStrategy() {
            @Override
            public String apply(String value) {
                return value.toLowerCase();
            }
        };
    }

    /**
     * Defines a naming strategy that returns snake case of the value passed
     *
     * @return the new value.
     * @see <a href="https://en.wikipedia.org/wiki/Snake_case">Snake case</a>
     */
    public static NamingStrategy snakeCase() {
        return new NamingStrategy() {
            @Override
            public String apply(String value) {
                List<String> groups = groupByCapitals(value);

                StringJoiner joiner = new StringJoiner("_");
                for (String group : groups) {
                    joiner.add(group.toLowerCase());
                }
                return joiner.toString();
            }
        };
    }

    /**
     * Defines a naming strategy that returns camel case of the value passed
     *
     * @return the new value.
     * @see <a href="https://en.wikipedia.org/wiki/Camel_case">Camel case</a>
     */
    public static NamingStrategy camelCase() {
        return new NamingStrategy() {
            @Override
            public String apply(String value) {
                List<String> groups = groupByCapitals(value);

                StringJoiner joiner = new StringJoiner("");
                joiner.add(groups.get(0).toLowerCase());
                for (int i = 1; i < groups.size(); i++) {
                    joiner.add(groups.get(i));
                }
                return joiner.toString();
            }
        };
    }

    /**
     * Defines a naming strategy that returns kebab case of the value passed
     *
     * @return the new value.
     * @see <a href="https://en.wikipedia.org/wiki/Kebab_case">Kebab case</a>
     */
    public static NamingStrategy kebabCase() {
        return new NamingStrategy() {
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
        };
    }

    /**
     * Applies this naming strategy to the given value
     *
     * @param value the value to process
     * @return the updated value
     */
    public abstract String apply(String value);

    private static List<String> groupByCapitals(String value) {
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
