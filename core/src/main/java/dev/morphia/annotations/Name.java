package dev.morphia.annotations;

import dev.morphia.annotations.internal.MorphiaExperimental;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Defines a name for a constructor parameter.
 *
 * @morphia.experimental
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@MorphiaExperimental
public @interface Name {
    /**
     * @return the field name for the parameter
     * @see Property
     */
    String value();
}
