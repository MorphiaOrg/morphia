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
@Target({ ElementType.ANNOTATION_TYPE })
@MorphiaExperimental
public @interface ShardOptions {
    /**
     * Optional. Specifies the minimum number of chunks to create initially when sharding an empty collection with a hashed shard key.
     *
     * @return the number of chunks
     */
    int numInitialChunks() default -1;

    /**
     * Optional. Specify true to perform initial chunk creation and distribution for an empty or non-existing collection based on the
     * defined zones and zone ranges for the collection. For hashed sharding only.
     *
     * @return true for presplits
     * @see dev.morphia.mapping.ShardKeyType#HASHED
     */
    boolean presplitHashedZones() default false;

    /**
     * Optional. Specify true to ensure that the underlying index enforces a unique constraint.
     *
     * @return true for unique constraints
     */
    boolean unique() default false;
}
