package dev.morphia.annotations;

import dev.morphia.annotations.internal.MorphiaExperimental;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Defines the properties used in the shard key.
 *
 * @morphia.experimental
 * @since 2.3
 */
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@MorphiaExperimental
public @interface ShardKeys {
    /**
     * The sharding options to apply.
     *
     * @return the options
     */
    ShardOptions options() default @ShardOptions();

    /**
     * The shard keys
     *
     * @return the keys
     */
    ShardKey[] value();
}

