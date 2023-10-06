package dev.morphia.mapping;

/**
 * Defines the kind of sharding to perform.
 */
public enum ShardKeyType {
    /**
     * hashed sharding
     */
    HASHED,
    /**
     * ranged sharding
     */
    RANGED
}
