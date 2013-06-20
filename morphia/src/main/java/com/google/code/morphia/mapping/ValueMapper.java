package com.google.code.morphia.mapping;


import java.util.Map;

import com.google.code.morphia.mapping.cache.EntityCache;
import com.mongodb.DBObject;


/**
 * Simple mapper that just uses the Mapper.getOptions().converts
 *
 * @author Scott Hernnadez
 */
class ValueMapper implements CustomMapper {
  public void toDBObject(final Object entity, final MappedField mf, final DBObject dbObject, final Map<Object, DBObject> involvedObjects,
    final Mapper mapper) {
    try {
      mapper.converters.toDBObject(entity, mf, dbObject, mapper.getOptions());
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public void fromDBObject(final DBObject dbObject, final MappedField mf, final Object entity, final EntityCache cache,
    final Mapper mapper) {
    try {
      mapper.converters.fromDBObject(dbObject, mf, entity);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

  }
}
