package dev.morphia.mapping;

import com.mongodb.BasicDBList;
import dev.morphia.Datastore;
import dev.morphia.mapping.cache.EntityCache;
import dev.morphia.utils.IterHelper;
import dev.morphia.utils.IterHelper.MapIterCallback;
import dev.morphia.utils.ReflectionUtils;
import org.bson.Document;

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
        boolean isDocument = convertedVal instanceof Document;
        boolean anInterface = mf.getSubClass().isInterface();
        boolean anAbstract = Modifier.isAbstract(mf.getSubClass().getModifiers());
        boolean equals = mf.getSubClass().equals(rawVal.getClass());
        return convertedVal == null || !isDocument || anInterface || anAbstract || !equals;
    }

    private static boolean isMapOrCollection(final MappedField mf) {
        return Map.class.isAssignableFrom(mf.getSubClass()) || Iterable.class.isAssignableFrom(mf.getSubClass());
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public void fromDocument(final Datastore datastore, final Document document, final MappedField mf, final Object entity,
                             final EntityCache cache, final Mapper mapper) {
        try {
            if (mf.isMap()) {
                readMap(datastore, mapper, entity, cache, mf, document);
            } else if (mf.isMultipleValues()) {
                readCollection(datastore, mapper, entity, cache, mf, document);
            } else {
                // single element
                final Object dbVal = mf.getDocumentValue(document);
                if (dbVal != null) {
                    final boolean isDocument = dbVal instanceof Document;

                    //run converters
                    if (isDocument && !mapper.isMapped(mf.getConcreteType()) && (mapper.getConverters().hasDocumentConverter(mf)
                                                                                 || mapper.getConverters()
                                                                                          .hasDocumentConverter(mf.getType()))) {
                        mapper.getConverters().fromDocument(document, mf, entity);
                    } else {
                        Object refObj;
                        if (mapper.getConverters().hasSimpleValueConverter(mf) || mapper.getConverters()
                                                                                        .hasSimpleValueConverter(mf.getType())) {
                            refObj = mapper.getConverters().decode(mf.getType(), dbVal, mf);
                        } else {
                            Document value = (Document) dbVal;
                            refObj = mapper.getOptions().getObjectFactory().createInstance(mapper, mf, value);
                            refObj = mapper.fromDb(datastore, value, refObj, cache);
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
    public void toDocument(final Object entity, final MappedField mf, final Document document, final Map<Object, Document> involvedObjects,
                           final Mapper mapper) {
        final String name = mf.getNameToStore();

        final Object fieldValue = mf.getFieldValue(entity);

        if (mf.isMap()) {
            writeMap(mf, document, involvedObjects, name, fieldValue, mapper);
        } else if (mf.isMultipleValues()) {
            writeCollection(mf, document, involvedObjects, name, fieldValue, mapper);
        } else {
            //run converters
            if (mapper.getConverters().hasDocumentConverter(mf) || mapper.getConverters().hasDocumentConverter(entity.getClass())) {
                mapper.getConverters().toDocument(entity, mf, document, mapper.getOptions());
                return;
            }

            final Document dbObj = fieldValue == null ? null : mapper.toDocument(fieldValue, involvedObjects);
            if (dbObj != null) {
                if (!shouldSaveClassName(fieldValue, dbObj, mf)) {
                    dbObj.remove(mapper.getOptions().getDiscriminatorField());
                }

                if (!dbObj.keySet().isEmpty() || mapper.getOptions().isStoreEmpties()) {
                    document.put(name, dbObj);
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void readCollection(final Datastore datastore, final Mapper mapper, final Object entity, final EntityCache cache,
                                final MappedField mf, final Document document) {
        Collection values;

        final Object dbVal = mf.getDocumentValue(document);
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
                        newEntity = readMapOrCollectionOrEntity(datastore, mapper, cache, mf, ephemeralMappedField, (Document) o);
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
                         final MappedField mf, final Document document) {
        final Document dbObj = (Document) mf.getDocumentValue(document);

        if (dbObj != null) {
            final Map map = mapper.getOptions().getObjectFactory().createMap(mf);

            final EphemeralMappedField ephemeralMappedField = isMapOrCollection(mf)
                                                              ? new EphemeralMappedField((ParameterizedType) mf.getSubType(), mf, mapper)
                                                              : null;
            new IterHelper<>().loopMap(dbObj, new MapIterCallback<>() {
                @Override
                public void eval(final Object k, final Object val) {
                    Object newEntity = null;

                    //run converters
                    if (val != null) {
                        if (mapper.getConverters().hasSimpleValueConverter(mf)
                            || mapper.getConverters().hasSimpleValueConverter(mf.getSubClass())) {
                            newEntity = mapper.getConverters().decode(mf.getSubClass(), val, mf);
                        } else {
                            if (val instanceof Document) {
                                newEntity = readMapOrCollectionOrEntity(datastore, mapper, cache, mf, ephemeralMappedField, (Document) val);
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
                                               final Document dbObj) {
        if (ephemeralMappedField != null) {
            mapper.fromDb(datastore, dbObj, ephemeralMappedField, cache);
            return ephemeralMappedField.getValue();
        } else {
            final Object newEntity = mapper.getOptions().getObjectFactory().createInstance(mapper, mf, dbObj);
            return mapper.fromDb(datastore, dbObj, newEntity, cache);
        }
    }

    private void writeCollection(final MappedField mf, final Document document, final Map<Object, Document> involvedObjects,
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
            final List<Object> values = new ArrayList<>();
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
                        val = mapper.toDocument(o, involvedObjects);
                    }

                    if (!shouldSaveClassName(o, val, mf)) {
                        ((Document) val).remove(mapper.getOptions().getDiscriminatorField());
                    }

                    values.add(val);
                }
            }
            if (!values.isEmpty() || mapper.getOptions().isStoreEmpties()) {
                document.put(name, values);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void writeMap(final MappedField mf, final Document document, final Map<Object, Document> involvedObjects, final String name,
                          final Object fieldValue, final Mapper mapper) {
        final Map<String, Object> map = (Map<String, Object>) fieldValue;
        if (map != null) {
            final Document values = new Document();

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
                        val = mapper.toDocument(entryVal, involvedObjects);
                    }

                    if (!shouldSaveClassName(entryVal, val, mf)) {
                        if (val instanceof List) {
                            if (((List) val).get(0) instanceof Document) {
                                List<Document> list = (List<Document>) val;
                                for (Document o : list) {
                                    o.remove(mapper.getOptions().getDiscriminatorField());
                                }
                            }
                        } else {
                            ((Document) val).remove(mapper.getOptions().getDiscriminatorField());
                        }
                    }
                }

                final String strKey = mapper.getConverters().encode(entry.getKey()).toString();
                values.put(strKey, val);
            }

            if (!values.isEmpty() || mapper.getOptions().isStoreEmpties()) {
                document.put(name, values);
            }
        }
    }

}
