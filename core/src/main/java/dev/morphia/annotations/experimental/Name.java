package dev.morphia.annotations.experimental;

import dev.morphia.annotations.Property;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Defines a name for a constructor parameter.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface Name {
    /**
     * @return the field name for the parameter
     * @see Property
     */
    String value();
}
