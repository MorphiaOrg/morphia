package dev.morphia.mapping.codec.pojo.experimental;

import dev.morphia.mapping.codec.MorphiaCodecProvider;
import dev.morphia.mapping.codec.pojo.EntityModel;

import java.util.List;

/**
 * @morphia.internal
 * @since 2.3
 */
public interface EntityModelImporter {
    MorphiaCodecProvider getCodecProvider();

    List<EntityModel> importModels();
}
