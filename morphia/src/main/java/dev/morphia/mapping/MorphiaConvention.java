package dev.morphia.mapping;

import dev.morphia.Datastore;
import dev.morphia.mapping.codec.pojo.EntityModelBuilder;
import org.bson.codecs.pojo.ClassModelBuilder;
import org.bson.codecs.pojo.Convention;

/**
 * Applies certain conventions specific for Morphia
 */
public interface MorphiaConvention extends Convention {
    @Override
    default void apply(final ClassModelBuilder<?> classModelBuilder) {
        if (classModelBuilder instanceof EntityModelBuilder) {
            throw new MappingException("call #apply(Datastore, MorphiaModelBuilder) instead");
        }
    }

    /**
     * This method applies this Convention to the given builder
     *
     * @param datastore the datastore to use
     * @param builder   the builder to apply the convention to
     */
    void apply(Datastore datastore, EntityModelBuilder<?> builder);
}
