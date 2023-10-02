package dev.morphia.mapping;

import dev.morphia.annotations.internal.MorphiaInternal;
import dev.morphia.mapping.codec.MorphiaInstanceCreator;

/**
 * The factory for creating {@link MorphiaInstanceCreator} instances
 * 
 * @hidden
 * @morphia.internal
 */
@MorphiaInternal
public interface InstanceCreatorFactory {

    /**
     * @return a new ClassAccessor instance
     */
    MorphiaInstanceCreator create();
}
