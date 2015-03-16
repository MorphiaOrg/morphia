package org.mongodb.morphia.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that this entity has a text index define.  With this annotation one can define the name and the default language for the 
 * index.  To specify the specific fields or the language override fields, one would annotate those fields directly.
 * 
 * @see Text
 * @see Language
 */
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface TextIndexed {
    /**
     * Name of the index
     */
    String value() default "";

    /**
     * Default language for the index.
     */
    String language() default "";
}
