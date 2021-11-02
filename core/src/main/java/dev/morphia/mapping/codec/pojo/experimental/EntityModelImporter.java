package dev.morphia.mapping.codec.pojo.experimental;

import dev.morphia.Datastore;
import dev.morphia.annotations.internal.MorphiaExperimental;
import dev.morphia.annotations.internal.MorphiaInternal;
import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.codec.MorphiaCodecProvider;
import dev.morphia.mapping.codec.pojo.EntityModel;

import java.util.List;

/**
 * Defines an import to allow for the external definition and import of model and codec definitions.
 * <p>
 * NOTE:  This interface is marked as an internal interface but is intended for use in more advanced scenarios.  While this API should
 * remain relatively stable over time, breaking changes might happen occasionally.
 *
 * @morphia.internal
 * @since 2.3
 */
@MorphiaInternal
@MorphiaExperimental
public interface EntityModelImporter {
    /**
     * Returns the codec provider responsible for creating the codecs for the {@link EntityModel}s returned by this importer.
     *
     * @param datastore The datastore to use
     * @return the provider
     */
    MorphiaCodecProvider getCodecProvider(Datastore datastore);

    /**
     * Returns the models to be introduced by this importer.
     *
     * @param mapper the Mapper to use while building the {@link EntityModel}s
     * @return the models
     */
    List<EntityModel> getModels(Mapper mapper);
}
