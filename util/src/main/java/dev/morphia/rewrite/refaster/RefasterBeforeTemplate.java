package dev.morphia.rewrite.refaster;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a field in a Refaster template class as the "before" pattern template to be replaced.
 */
@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.SOURCE)
public @interface RefasterBeforeTemplate {
}
