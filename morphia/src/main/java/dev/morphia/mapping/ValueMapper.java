package dev.morphia.mapping;


import dev.morphia.Datastore;
import dev.morphia.mapping.cache.EntityCache;
import org.bson.Document;

import java.util.Map;


/**
 * @morphia.internal
 * @deprecated
 */
class ValueMapper implements CustomMapper {
    @Override
    public void fromDocument(final Datastore datastore, final Document document, final MappedField mf, final Object entity,
                             final EntityCache cache, final Mapper mapper) {
        mapper.getConverters().fromDocument(document, mf, entity);
    }

    @Override
    public void toDocument(final Object entity, final MappedField mf, final Document document, final Map<Object, Document> involvedObjects,
                           final Mapper mapper) {
        try {
            mapper.getConverters().toDocument(entity, mf, document, mapper.getOptions());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
