package dev.morphia.mapping;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import dev.morphia.Datastore;
import dev.morphia.mapping.cache.EntityCache;
import dev.morphia.utils.IterHelper;
import dev.morphia.utils.IterHelper.MapIterCallback;
import dev.morphia.utils.ReflectionUtils;

import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @morphia.internal
 * @deprecated
 */
class EmbeddedMapper implements CustomMapper {
    static boolean shouldSaveClassName(final Object rawVal, final Object convertedVal, final MappedField mf) {
        if (rawVal == null || mf == null) {
            return true;
        }
        if (mf.isSingleValue()) {
            return !(mf.getType().equals(rawVal.getClass()) && !(convertedVal instanceof BasicDBList));
        }
        boolean isDBObject = convertedVal instanceof DBObject;
        boolean anInterface = mf.getSubClass().isInterface();
        boolean anAbstract = Modifier.isAbstract(mf.getSubClass().getModifiers());
        boolean equals = mf.getSubClass().equals(rawVal.getClass());
        return convertedVal == null || !isDBObject || anInterface || anAbstract || !equals;
    }

    private static boolean isMapOrCollection(final MappedField mf) {
        return Map.class.isAssignableFrom(mf.getSubClass()) || Iterable.class.isAssignableFrom(mf.getSubClass());
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public void fromDBObject(final Datastore datastore, final DBObject dbObject, final MappedField mf, final Object entity,
                             final EntityCache cache, final Mapper mapper) {
        try {
            if (mf.isMap()) {
                readMap(datastore, mapper, entity, cache, mf, dbObject);
            } else if (mf.isMultipleValues()) {
                readCollection(datastore, mapper, entity, cache, mf, dbObject);
            } else {
                // single element
                final Object dbVal = mf.getDbObjectValue(dbObject);
                if (dbVal != null) {
                    final boolean isDBObject = dbVal instanceof DBObject;

                    //run converters
                    if (isDBObject && !mapper.isMapped(mf.getConcreteType()) && (mapper.getConverters().hasDbObjectConverter(mf)
                                                                                 || mapper.getConverters()
                                                                                          .hasDbObjectConverter(mf.getType()))) {
                        mapper.getConverters().fromDBObject(dbObject, mf, entity);
                    } else {
                        Object refObj;
                        if (mapper.getConverters().hasSimpleValueConverter(mf) || mapper.getConverters()
                                                                                        .hasSimpleValueConverter(mf.getType())) {
                            refObj = mapper.getConverters().decode(mf.getType(), dbVal, mf);
                        } else if (mapper.getConverters().hasSimpleValueConverter(dbVal.getClass())) {
                            refObj = mapper.getConverters().decode(dbVal.getClass(), dbVal, mf);
                        } else if (isDBObject) {
                            DBObject value = (DBObject) dbVal;
                            refObj = mapper.getOptions().getObjectFactory().createInstance(mapper, mf, value);
                            refObj = mapper.fromDb(datastore, value, refObj, cache);
                        } else {
                            refObj = dbVal;
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

    @Override
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
            if (mapper.getConverters().hasDbObjectConverter(mf) || mapper.getConverters().hasDbObjectConverter(entity.getClass())) {
                mapper.getConverters().toDBObject(entity, mf, dbObject, mapper.getOptions());
                return;
            }

            final DBObject dbObj = fieldValue == null ? null : mapper.toDBObject(fieldValue, involvedObjects);
            if (dbObj != null) {
                if (!shouldSaveClassName(fieldValue, dbObj, mf)) {
                    dbObj.removeField(mapper.getOptions().getDiscriminatorField());
                }

                if (!dbObj.keySet().isEmpty() || mapper.getOptions().isStoreEmpties()) {
                    dbObject.put(name, dbObj);
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void readCollection(final Datastore datastore, final Mapper mapper, final Object entity, final EntityCache cache,
                                final MappedField mf, final DBObject dbObject) {
        Collection values;

        final Object dbVal = mf.getDbObjectValue(dbObject);
        if (dbVal != null) {
            // multiple documents in a List
            values = mf.isSet() ? mapper.getOptions().getObjectFactory().createSet(mf)
                                : mapper.getOptions().getObjectFactory().createList(mf);

            final List dbValues;
            if (dbVal instanceof List) {
                dbValues = (List) dbVal;
            } else {
                dbValues = new BasicDBList();
                dbValues.add(dbVal);
            }

            EphemeralMappedField ephemeralMappedField = !mapper.isMapped(mf.getType()) && isMapOrCollection(mf)
                                                            && (mf.getSubType() instanceof ParameterizedType)
                                                        ? new EphemeralMappedField((ParameterizedType) mf.getSubType(), mf, mapper)
                                                        : null;
            for (final Object o : dbValues) {

                Object newEntity = null;

                if (o != null) {
                    //run converters
                    if (mapper.getConverters().hasSimpleValueConverter(mf) || mapper.getConverters()
                                                                                    .hasSimpleValueConverter(mf.getSubClass())) {
                        newEntity = mapper.getConverters().decode(mf.getSubClass(), o, mf);
                    } else {
                        newEntity = readMapOrCollectionOrEntity(datastore, mapper, cache, mf, ephemeralMappedField, (DBObject) o);
                    }
                }

                values.add(newEntity);
            }
            if (!values.isEmpty() || mapper.getOptions().isStoreEmpties()) {
                if (mf.getType().isArray()) {
                    mf.setFieldValue(entity, ReflectionUtils.convertToArray(mf.getSubClass(), ReflectionUtils.iterToList(values)));
                } else {
                    mf.setFieldValue(entity, values);
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void readMap(final Datastore datastore, final Mapper mapper, final Object entity, final EntityCache cache,
                         final MappedField mf, final DBObject dbObject) {
        final DBObject dbObj = (DBObject) mf.getDbObjectValue(dbObject);

        if (dbObj != null) {
            final Map map = mapper.getOptions().getObjectFactory().createMap(mf);

            final EphemeralMappedField ephemeralMappedField = isMapOrCollection(mf)
                                                              ? new EphemeralMappedField((ParameterizedType) mf.getSubType(), mf, mapper)
                                                              : null;
            new IterHelper<Object, Object>().loopMap(dbObj, new MapIterCallback<Object, Object>() {
                @Override
                public void eval(final Object k, final Object val) {
                    Object newEntity = null;

                    //run converters
                    if (val != null) {
                        if (mapper.getConverters().hasSimpleValueConverter(mf)
                            || mapper.getConverters().hasSimpleValueConverter(mf.getSubClass())) {
                            newEntity = mapper.getConverters().decode(mf.getSubClass(), val, mf);
                        } else {
                            if (val instanceof DBObject) {
                                newEntity = readMapOrCollectionOrEntity(datastore, mapper, cache, mf, ephemeralMappedField, (DBObject) val);
                            } else {
                                newEntity = val;
                            }

                        }
                    }

                    final Object objKey = mapper.getConverters().decode(mf.getMapKeyClass(), k, mf);
                    map.put(objKey, newEntity);
                }
            });

            if (!map.isEmpty() || mapper.getOptions().isStoreEmpties()) {
                mf.setFieldValue(entity, map);
            }
        }
    }

    private Object readMapOrCollectionOrEntity(final Datastore datastore, final Mapper mapper, final EntityCache cache,
                                               final MappedField mf, final EphemeralMappedField ephemeralMappedField,
                                               final DBObject dbObj) {
        if (ephemeralMappedField != null) {
            mapper.fromDb(datastore, dbObj, ephemeralMappedField, cache);
            return ephemeralMappedField.getValue();
        } else {
            final Object newEntity = mapper.getOptions().getObjectFactory().createInstance(mapper, mf, dbObj);
            return mapper.fromDb(datastore, dbObj, newEntity, cache);
        }
    }

    private void writeCollection(final MappedField mf, final DBObject dbObject, final Map<Object, DBObject> involvedObjects,
                                 final String name, final Object fieldValue, final Mapper mapper) {
        Iterable coll = null;

        if (fieldValue != null) {
            if (mf.isArray()) {
                coll = Arrays.asList((Object[]) fieldValue);
            } else {
                coll = (Iterable) fieldValue;
            }
        }

        if (coll != null) {
            final List<Object> values = new ArrayList<Object>();
            for (final Object o : coll) {
                if (null == o) {
                    values.add(null);
                } else if (mapper.getConverters().hasSimpleValueConverter(mf) || mapper.getConverters()
                                                                                       .hasSimpleValueConverter(o.getClass())) {
                    values.add(mapper.getConverters().encode(o));
                } else {
                    final Object val;
                    if (Collection.class.isAssignableFrom(o.getClass()) || Map.class.isAssignableFrom(o.getClass())) {
                        val = mapper.toMongoObject(o, true);
                    } else {
                        val = mapper.toDBObject(o, involvedObjects);
                    }

                    if (!shouldSaveClassName(o, val, mf)) {
                        ((DBObject) val).removeField(mapper.getOptions().getDiscriminatorField());
                    }

                    values.add(val);
                }
            }
            if (!values.isEmpty() || mapper.getOptions().isStoreEmpties()) {
                dbObject.put(name, values);
            }
        }
    }

    @SuppressWarnings("unchecked")
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
                } else if (mapper.getConverters().hasSimpleValueConverter(mf)
                    || mapper.getConverters().hasSimpleValueConverter(entryVal.getClass())) {
                    val = mapper.getConverters().encode(entryVal);
                } else {
                    if (Map.class.isAssignableFrom(entryVal.getClass()) || Collection.class.isAssignableFrom(entryVal.getClass())) {
                        val = mapper.toMongoObject(entryVal, true);
                    } else {
                        val = mapper.toDBObject(entryVal, involvedObjects);
                    }

                    if (!shouldSaveClassName(entryVal, val, mf)) {
                        if (val instanceof List) {
                            if (((List) val).get(0) instanceof DBObject) {
                                List<DBObject> list = (List<DBObject>) val;
                                for (DBObject o : list) {
                                    o.removeField(mapper.getOptions().getDiscriminatorField());
                                }
                            }
                        } else {
                            ((DBObject) val).removeField(mapper.getOptions().getDiscriminatorField());
                        }
                    }
                }

                final String strKey = mapper.getConverters().encode(entry.getKey()).toString();
                values.put(strKey, val);
            }

            if (!values.isEmpty() || mapper.getOptions().isStoreEmpties()) {
                dbObject.put(name, values);
            }
        }
    }

}
