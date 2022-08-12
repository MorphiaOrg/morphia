package dev.morphia.annotations;

import dev.morphia.annotations.internal.MorphiaExperimental;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specifies options to be applied when sharding.
 *
 * @morphia.experimental
 * @since 2.3
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.ANNOTATION_TYPE})
@MorphiaExperimental
public @interface ShardOptions {
    int numInitialChunks() default -1;

    boolean presplitHashedZones() default false;

    boolean unique() default false;
}
