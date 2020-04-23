package dev.morphia.annotations;


import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * Indicates that this field can be constructed from the stored fields; it doesn't require a no-args constructor. Please list the names of
 * args/fields, in order.
 *
 * @deprecated incomplete.  will be reworked in the 2.x timeframe
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@Deprecated
public @interface ConstructorArgs {
    /**
     * @return The fields to use
     */
    String[] value();
}
