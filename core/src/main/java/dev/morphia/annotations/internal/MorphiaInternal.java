package dev.morphia.annotations.internal;

import java.lang.annotation.Documented;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Marks an item as an internal implementation detail.  No guarantee is made to maintain compatibility with prior versions.
 */
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface MorphiaInternal {
}
