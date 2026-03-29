package dev.morphia.rewrite.refaster;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a field in a Refaster template class as the "after" replacement template.
 */
@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.SOURCE)
public @interface RefasterAfterTemplate {
}
