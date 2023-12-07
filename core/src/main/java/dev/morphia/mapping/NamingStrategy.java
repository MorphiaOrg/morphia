package dev.morphia.mapping;

import dev.morphia.mapping.strategy.CamelCase;
import dev.morphia.mapping.strategy.Identity;
import dev.morphia.mapping.strategy.KebabCase;
import dev.morphia.mapping.strategy.LowerCase;
import dev.morphia.mapping.strategy.SnakeCase;
import dev.morphia.mapping.strategy.Title;

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
        return new Identity();
    }

    /**
     * Defines a naming strategy that returns the lowercase form of the value passed
     *
     * @return the updated value.
     */
    public static NamingStrategy lowerCase() {
        return new LowerCase();
    }

    /**
     * Defines a naming strategy that returns snake case of the value passed
     *
     * @return the new value.
     * @see <a href="https://en.wikipedia.org/wiki/Snake_case">Snake case</a>
     */
    public static NamingStrategy snakeCase() {
        return new SnakeCase();
    }

    /**
     * Defines a naming strategy that returns camel case of the value passed
     *
     * @return the new value.
     * @see <a href="https://en.wikipedia.org/wiki/Camel_case">Camel case</a>
     */
    public static NamingStrategy camelCase() {
        return new CamelCase();
    }

    /**
     * Defines a naming strategy that returns kebab case of the value passed
     *
     * @return the new value.
     * @see <a href="https://en.wikipedia.org/wiki/Kebab_case">Kebab case</a>
     */
    public static NamingStrategy kebabCase() {
        return new KebabCase();
    }

    /**
     * @hidden
     * @return
     */
    public static NamingStrategy title() {
        return new Title();
    }

    /**
     * Applies this naming strategy to the given value
     *
     * @param value the value to process
     * @return the updated value
     */
    public abstract String apply(String value);
}
