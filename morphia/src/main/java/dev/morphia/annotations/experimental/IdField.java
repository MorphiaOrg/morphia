package dev.morphia.annotations.experimental;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Denotes the ID field on an entity.  This field is typically on a parent type outside the current source base and can't be directly
 * annotated with {@link dev.morphia.annotations.Id}
 */
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface IdField {
    /**
     * @return the name of the field to use as the ID field
     */
    String value();
}
