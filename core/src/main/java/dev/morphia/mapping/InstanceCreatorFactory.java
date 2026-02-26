package dev.morphia.mapping;

import dev.morphia.mapping.codec.Conversions;
import dev.morphia.mapping.codec.MorphiaInstanceCreator;

/**
 * The factory for creating {@link MorphiaInstanceCreator} instances
 */
public interface InstanceCreatorFactory {

    /**
     * @param conversions the Conversions instance to use
     * @return a new ClassAccessor instance
     */
    MorphiaInstanceCreator create(Conversions conversions);
}
