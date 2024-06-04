package dev.morphia.mapping.conventions;

import dev.morphia.mapping.Mapper;

public interface MappingConvention<M> {
    /**
     * This method applies this Convention to the given builder
     *
     * @param mapper the mapper to use
     * @param model  the model to apply the convention to
     */
    void apply(Mapper mapper, M model);
}
