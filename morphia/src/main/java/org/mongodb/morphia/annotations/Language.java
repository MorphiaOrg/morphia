package org.mongodb.morphia.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates the field to use to determine the language to use while indexing.  If this annotation is not used, then any field named
 * "language" will be used to determine the language to use when indexing.  If there is no such field, the default language will be used.
 * 
 * @see TextIndexed#language() 
 */
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface Language {
}
