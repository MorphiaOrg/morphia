package org.mongodb.morphia.mapping;


import com.mongodb.DBObject;
import org.mongodb.morphia.mapping.cache.EntityCache;

import java.util.Map;


/**
 * Simple mapper that just uses the Mapper.getOptions().converts
 *
 * @author Scott Hernnadez
 */
class ValueMapper implements CustomMapper {
    public void fromDBObject(final DBObject dbObject, final MappedField mf, final Object entity, final EntityCache cache,
                             final Mapper mapper) {
        mapper.getConverters().fromDBObject(dbObject, mf, entity);
    }

    public void toDBObject(final Object entity, final MappedField mf, final DBObject dbObject, final Map<Object, DBObject> involvedObjects,
                           final Mapper mapper) {
        try {
            mapper.getConverters().toDBObject(entity, mf, dbObject, mapper.getOptions());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
