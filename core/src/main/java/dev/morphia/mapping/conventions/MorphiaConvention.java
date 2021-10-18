package dev.morphia.mapping.conventions;

import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.codec.pojo.EntityModelBuilder;

/**
 * Applies certain conventions specific for Morphia
 */
public interface MorphiaConvention {
    /**
     * This method applies this Convention to the given builder
     *
     * @param mapper
     * @param builder the builder to apply the convention to
     */
    void apply(Mapper mapper, EntityModelBuilder builder);
}
