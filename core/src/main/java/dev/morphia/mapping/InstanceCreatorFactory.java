package dev.morphia.mapping;

import dev.morphia.mapping.codec.MorphiaInstanceCreator;

/**
 * The factory for creating {@link MorphiaInstanceCreator} instances
 */
public interface InstanceCreatorFactory {

    /**
     * @return a new ClassAccessor instance
     */
    MorphiaInstanceCreator create();
}
