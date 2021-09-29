package dev.morphia.mapping.codec.pojo.experimental;

import dev.morphia.Datastore;
import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.codec.MorphiaCodecProvider;
import dev.morphia.mapping.codec.pojo.EntityModel;

import java.util.List;

/**
 * @morphia.internal
 * @since 2.3
 */
public interface EntityModelImporter {
    List<EntityModel> importModels(Datastore datastore);

    default void importCodecProvider(Datastore datastore) {
        Mapper mapper = datastore.getMapper();
        mapper.register(new MorphiaCodecProvider(mapper, datastore));
        datastore.updateDatabaseWithRegistry();
    }
}
