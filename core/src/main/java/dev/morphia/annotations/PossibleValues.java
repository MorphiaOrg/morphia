package dev.morphia.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import dev.morphia.annotations.internal.MorphiaExperimental;

/**
 * Denotes the possible values for a configuration option. Depending on the config property, this list may not be exhausted. Consult
 * the documentation for that property for details.
 *
 * @since 2.4
 * @morphia.experimental
 */
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@MorphiaExperimental
public @interface PossibleValues {
    /**
     * @return the possible values
     */
    String[] value();

    /**
     * Indicates that a fully qualified class name maybe listed as well.
     *
     * @return true if a class name maybe be listed.
     */
    boolean fqcn() default true;
}
