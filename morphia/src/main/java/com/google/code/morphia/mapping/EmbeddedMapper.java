package com.google.code.morphia.mapping;


import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.google.code.morphia.mapping.cache.DisposeEntityCache;
import com.google.code.morphia.mapping.cache.EntityCache;
import com.google.code.morphia.utils.IterHelper;
import com.google.code.morphia.utils.IterHelper.MapIterCallback;
import com.google.code.morphia.utils.ReflectionUtils;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;


@SuppressWarnings({"unchecked", "rawtypes"})
class EmbeddedMapper implements CustomMapper {
	protected EntityCache disposedCache = new DisposeEntityCache();
    public void toDBObject(final Object entity, final MappedField mf, final DBObject dbObject, final Map<Object, DBObject> involvedObjects,
    final Mapper mapper) {
    final String name = mf.getNameToStore();

    final Object fieldValue = mf.getFieldValue(entity);

    if (mf.isMap()) {
      writeMap(mf, dbObject, involvedObjects, name, fieldValue, mapper);
    } else if (mf.isMultipleValues()) {
      writeCollection(mf, dbObject, involvedObjects, name, fieldValue, mapper);
    } else {
      //run converters
      if (mapper.converters.hasDbObjectConverter(mf) || mapper.converters.hasDbObjectConverter(entity.getClass())) {
        mapper.converters.toDBObject(entity, mf, dbObject, mapper.getOptions());
        return;
      }

      final DBObject dbObj = fieldValue == null ? null : mapper.toDBObject(fieldValue, involvedObjects);
      if (dbObj != null) {
        if (!shouldSaveClassName(fieldValue, dbObj, mf)) {
          dbObj.removeField(Mapper.CLASS_NAME_FIELDNAME);
        }

        if (!dbObj.keySet().isEmpty() || mapper.getOptions().storeEmpties) {
          dbObject.put(name, dbObj);
        }
      }
    }
  }

  private void writeCollection(final MappedField mf, final DBObject dbObject, final Map<Object, DBObject> involvedObjects,
    final String name, final Object fieldValue, final Mapper mapper) {
    Iterable coll = null;

    if (fieldValue != null) {
      if (mf.isArray) {
        coll = Arrays.asList((Object[]) fieldValue);
      } else {
        coll = (Iterable) fieldValue;
      }
    }

    if (coll != null) {
      final List values = new ArrayList();
      for (final Object o : coll) {
        if (null == o) {
          values.add(null);
        } else if (mapper.converters.hasSimpleValueConverter(mf) || mapper.converters.hasSimpleValueConverter(o.getClass())) {
          values.add(mapper.converters.encode(o));
        } else {
          final Object val;
          if (Collection.class.isAssignableFrom(o.getClass()) || Map.class.isAssignableFrom(o.getClass())) {
            val = mapper.toMongoObject(o, true);
          } else {
            val = mapper.toDBObject(o, involvedObjects);
          }

          if (!shouldSaveClassName(o, val, mf)) {
            ((DBObject) val).removeField(Mapper.CLASS_NAME_FIELDNAME);
          }

          values.add(val);
        }
      }
      if (!values.isEmpty() || mapper.getOptions().storeEmpties) {
        dbObject.put(name, values);
      }
    }
  }

  private void writeMap(final MappedField mf, final DBObject dbObject, final Map<Object, DBObject> involvedObjects, final String name,
    final Object fieldValue, final Mapper mapper) {
    final Map<String, Object> map = (Map<String, Object>) fieldValue;
    if (map != null) {
      final BasicDBObject values = new BasicDBObject();

      for (final Map.Entry<String, Object> entry : map.entrySet()) {
        final Object entryVal = entry.getValue();
        final Object val;

        if (entryVal == null) {
          val = null;
        } else if (mapper.converters.hasSimpleValueConverter(mf) || mapper.converters.hasSimpleValueConverter(entryVal.getClass())) {
          val = mapper.converters.encode(entryVal);
        } else {
          if (Map.class.isAssignableFrom(entryVal.getClass()) || Collection.class.isAssignableFrom(entryVal.getClass())) {
            val = mapper.toMongoObject(entryVal, true);
          } else {
            val = mapper.toDBObject(entryVal, involvedObjects);
          }

          if (!shouldSaveClassName(entryVal, val, mf)) {
            ((DBObject) val).removeField(Mapper.CLASS_NAME_FIELDNAME);
          }
        }

        final String strKey = mapper.converters.encode(entry.getKey()).toString();
        values.put(strKey, val);
      }

      if (!values.isEmpty() || mapper.getOptions().storeEmpties) {
        dbObject.put(name, values);
      }
    }
  }

  public void fromDBObject(final DBObject dbObject, final MappedField mf, final Object entity, final EntityCache cache,
    final Mapper mapper) {
    try {
      if (mf.isMap()) {
        readMap(dbObject, mf, entity, cache, mapper);
      } else if (mf.isMultipleValues()) {
        readCollection(dbObject, mf, entity, cache, mapper);
      } else {
        // single element

        final Object dbVal = mf.getDbObjectValue(dbObject);
        if (dbVal != null) {
          final boolean isDBObject = dbVal instanceof DBObject && !(dbVal instanceof BasicDBList);

          //run converters
          if (isDBObject && (mapper.converters.hasDbObjectConverter(mf) || mapper.converters.hasDbObjectConverter(mf.getType()))) {
            mapper.converters.fromDBObject(((DBObject) dbVal), mf, entity);
          } else {
            Object refObj;
            if (mapper.converters.hasSimpleValueConverter(mf) || mapper.converters.hasSimpleValueConverter(mf.getType())) {
              refObj = mapper.converters.decode(mf.getType(), dbVal, mf);
            } else {
              refObj = mapper.getOptions().objectFactory.createInstance(mapper, mf, ((DBObject) dbVal));
              refObj = mapper.fromDb(((DBObject) dbVal), refObj, disposedCache);
            }
            if (refObj != null) {
              mf.setFieldValue(entity, refObj);
            }
          }
        }
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private void readCollection(final DBObject dbObject, final MappedField mf, final Object entity, final EntityCache cache,
    final Mapper mapper) {
    // multiple documents in a List
    final Collection values = mf.isSet() ? mapper.getOptions().objectFactory.createSet(mf) : mapper.getOptions().objectFactory.createList(
      mf);

    final Object dbVal = mf.getDbObjectValue(dbObject);
    if (dbVal != null) {

      final List dbValues;
      if (dbVal instanceof List) {
        dbValues = (List) dbVal;
      } else {
        dbValues = new BasicDBList();
        dbValues.add(dbVal);
      }

      for (final Object o : dbValues) {

        Object newEntity = null;

        if (o != null) {
          //run converters
          if (mapper.converters.hasSimpleValueConverter(mf) || mapper.converters.hasSimpleValueConverter(mf.getSubClass())) {
            newEntity = mapper.converters.decode(mf.getSubClass(), o, mf);
          } else {
            newEntity = readMapOrCollectionOrEntity((DBObject) o, mf, cache, mapper);
          }
        }

        values.add(newEntity);
      }
    }
    if (!values.isEmpty()) {
      if (mf.getType().isArray()) {
        mf.setFieldValue(entity, ReflectionUtils.convertToArray(mf.getSubClass(), ReflectionUtils.iterToList(values)));
      } else {
        mf.setFieldValue(entity, values);
      }
    }
  }

  private void readMap(final DBObject dbObject, final MappedField mf, final Object entity, final EntityCache cache, final Mapper mapper) {
    final Map map = mapper.getOptions().objectFactory.createMap(mf);

    final DBObject dbObj = (DBObject) mf.getDbObjectValue(dbObject);
    new IterHelper<Object, Object>().loopMap(dbObj, new MapIterCallback<Object, Object>() {
      @Override
      public void eval(final Object key, final Object val) {
        Object newEntity = null;

        //run converters
        if (val != null) {
          if (mapper.converters.hasSimpleValueConverter(mf) || mapper.converters.hasSimpleValueConverter(mf.getSubClass())) {
            newEntity = mapper.converters.decode(mf.getSubClass(), val, mf);
          } else {
            if (val instanceof DBObject) {
              newEntity = readMapOrCollectionOrEntity((DBObject) val, mf, cache, mapper);
            } else {
              throw new MappingException("Embedded element isn't a DBObject! How can it be that is a " + val.getClass());
            }

          }
        }

        final Object objKey = mapper.converters.decode(mf.getMapKeyClass(), key);
        map.put(objKey, newEntity);
      }
    });

    if (!map.isEmpty()) {
      mf.setFieldValue(entity, map);
    }
  }

  private Object readMapOrCollectionOrEntity(final DBObject dbObj, final MappedField mf, final EntityCache cache, final Mapper mapper) {
    if (Map.class.isAssignableFrom(mf.getSubClass()) || Iterable.class.isAssignableFrom(mf.getSubClass())) {
      final MapOrCollectionMF mocMF = new MapOrCollectionMF((ParameterizedType) mf.getSubType());
      mapper.fromDb(dbObj, mocMF, cache);
      return mocMF.getValue();
    } else {
      final Object newEntity = mapper.getOptions().objectFactory.createInstance(mapper, mf, dbObj);
      return mapper.fromDb(dbObj, newEntity, cache);
    }
  }

  public static boolean shouldSaveClassName(final Object rawVal, final Object convertedVal, final MappedField mf) {
    if (rawVal == null || mf == null) {
      return true;
    }
    if (mf.isSingleValue()) {
      return !(mf.getType().equals(rawVal.getClass()) && !(convertedVal instanceof BasicDBList));
    }
    return !(convertedVal != null && convertedVal instanceof DBObject && !mf.getSubClass().isInterface() && !Modifier.isAbstract(
      mf.getSubClass().getModifiers()) && mf.getSubClass().equals(rawVal.getClass()));
  }

}
