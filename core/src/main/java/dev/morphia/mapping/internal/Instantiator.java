package dev.morphia.mapping.internal;

import dev.morphia.annotations.internal.MorphiaInternal;

/**
 * Calls one of an entity's constructors. Critter generates one for each constructor that decoding may use, so that decoding
 * an entity doesn't go through {@link java.lang.reflect.Constructor#newInstance}.
 *
 * @morphia.internal
 * @hidden
 */
@MorphiaInternal
public interface Instantiator {
    /**
     * @param arguments the constructor's arguments, a primitive parameter's as its exact wrapper type
     * @return the new instance
     * @throws Exception whatever the constructor throws
     */
    Object newInstance(Object[] arguments) throws Exception;
}
