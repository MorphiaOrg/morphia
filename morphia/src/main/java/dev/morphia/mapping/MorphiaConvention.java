package dev.morphia.mapping;

import dev.morphia.Datastore;
import dev.morphia.mapping.codec.pojo.MorphiaModelBuilder;
import org.bson.codecs.pojo.ClassModelBuilder;
import org.bson.codecs.pojo.Convention;

public interface MorphiaConvention extends Convention {
    @Override
    default void apply(ClassModelBuilder<?> classModelBuilder) {
        if(classModelBuilder instanceof MorphiaModelBuilder) {
            throw new MappingException("call #apply(Datastore, MorphiaModelBuilder) instead");
        }
    }

    /**
     * This method applies this Convention to the given builder
     *
     * @param builder the builder to apply the convention to
     */
    void apply(Datastore datastore, MorphiaModelBuilder<?> builder);

}
