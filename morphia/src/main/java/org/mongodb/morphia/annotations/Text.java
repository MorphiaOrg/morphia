package org.mongodb.morphia.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks this field for inclusion in text indexing.
 * 
 * @see TextIndexed
 */
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface Text {
    /**
     * Weight of the field. If a weight is omitted from this item, the weight is assumed to the database default.
     */
    int value() default -1;
}
