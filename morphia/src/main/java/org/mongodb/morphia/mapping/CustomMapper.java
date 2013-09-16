package org.mongodb.morphia.mapping;


import java.util.Map;

import org.mongodb.morphia.mapping.cache.EntityCache;
import com.mongodb.DBObject;


/**
 * A CustomMapper if one that implements the methods needed to map to/from POJO/DBObject
 *
 * @author skot
 */
public interface CustomMapper {
  void toDBObject(Object entity, MappedField mf, DBObject dbObject, Map<Object, DBObject> involvedObjects, Mapper mapper);

  void fromDBObject(DBObject dbObject, MappedField mf, Object entity, EntityCache cache, Mapper mapper);
}
