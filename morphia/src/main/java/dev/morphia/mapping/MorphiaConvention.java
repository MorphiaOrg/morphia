package dev.morphia.mapping;

import dev.morphia.Datastore;
import dev.morphia.mapping.codec.pojo.EntityModelBuilder;

/**
 * Applies certain conventions specific for Morphia
 */
public interface MorphiaConvention {
    /**
     * This method applies this Convention to the given builder
     *
     * @param datastore the datastore to use
     * @param builder   the builder to apply the convention to
     */
    void apply(Datastore datastore, EntityModelBuilder<?> builder);
}
