package dev.morphia.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import dev.morphia.annotations.internal.MorphiaExperimental;
import dev.morphia.mapping.ShardKeyType;

/**
 * Defines a shard key of a particular type
 *
 * @morphia.experimental
 * @since 2.3
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.ANNOTATION_TYPE })
@MorphiaExperimental
public @interface ShardKey {
    /**
     * The type of sharding to use.
     *
     * @return the type
     */
    ShardKeyType type() default ShardKeyType.RANGED;

    /**
     * The shard key value
     *
     * @return the shard key
     */
    String value();
}
