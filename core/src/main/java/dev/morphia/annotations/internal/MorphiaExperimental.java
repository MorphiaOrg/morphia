package dev.morphia.annotations.internal;

import java.lang.annotation.Documented;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Marks an element as experimental and subject to change.
 */
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface MorphiaExperimental {
}
