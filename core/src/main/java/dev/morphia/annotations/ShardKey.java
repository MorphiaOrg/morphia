package dev.morphia.annotations;

import dev.morphia.annotations.internal.MorphiaExperimental;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Defines a shard key of a particular type
 *
 * @morphia.experimental
 * @since 2.3
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.ANNOTATION_TYPE})
@MorphiaExperimental
public @interface ShardKey {
    ShardKeyType type() default ShardKeyType.RANGED;

    String value();
}
