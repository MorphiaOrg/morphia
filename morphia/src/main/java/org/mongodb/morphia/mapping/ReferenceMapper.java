package org.mongodb.morphia.mapping;


import com.mongodb.DBObject;
import com.mongodb.DBRef;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.Key;
import org.mongodb.morphia.annotations.Reference;
import org.mongodb.morphia.logging.Logger;
import org.mongodb.morphia.logging.MorphiaLoggerFactory;
import org.mongodb.morphia.mapping.cache.EntityCache;
import org.mongodb.morphia.mapping.lazy.LazyFeatureDependencies;
import org.mongodb.morphia.mapping.lazy.proxy.ProxiedEntityReference;
import org.mongodb.morphia.mapping.lazy.proxy.ProxiedEntityReferenceList;
import org.mongodb.morphia.mapping.lazy.proxy.ProxiedEntityReferenceMap;
import org.mongodb.morphia.mapping.lazy.proxy.ProxyHelper;
import org.mongodb.morphia.utils.IterHelper;
import org.mongodb.morphia.utils.IterHelper.IterCallback;
import org.mongodb.morphia.utils.IterHelper.MapIterCallback;
import org.mongodb.morphia.utils.ReflectionUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;


@SuppressWarnings({"unchecked", "rawtypes"})
class ReferenceMapper implements CustomMapper {
    public static final Logger LOG = MorphiaLoggerFactory.get(ReferenceMapper.class);

    public void toDBObject(final Object entity, final MappedField mf, final DBObject dbObject, final Map<Object, DBObject> involvedObjects,
                           final Mapper mapper) {
        final String name = mf.getNameToStore();

        final Object fieldValue = mf.getFieldValue(entity);

        if (fieldValue == null && !mapper.getOptions().isStoreNulls()) {
            return;
        }

        final Reference refAnn = mf.getAnnotation(Reference.class);
        if (mf.isMap()) {
            writeMap(mf, dbObject, name, fieldValue, refAnn, mapper);
        } else if (mf.isMultipleValues()) {
            writeCollection(mf, dbObject, name, fieldValue, refAnn, mapper);
        } else {
            writeSingle(dbObject, name, fieldValue, refAnn, mapper);
        }

    }

    private void writeSingle(final DBObject dbObject, final String name, final Object fieldValue, final Reference refAnn,
                             final Mapper mapper) {
        if (fieldValue == null) {
            if (mapper.getOptions().isStoreNulls()) {
                dbObject.put(name, null);
            }
        } else {
            dbObject.put(name, refAnn.idOnly()
                               ? mapper.keyToManualRef(getKey(fieldValue, mapper))
                               : mapper.keyToRef(getKey(fieldValue, mapper)));
        }
    }

    private void writeCollection(final MappedField mf, final DBObject dbObject, final String name, final Object fieldValue,
                                 final Reference refAnn, final Mapper mapper) {
        if (fieldValue != null) {
            final List values = new ArrayList();

            if (ProxyHelper.isProxy(fieldValue) && ProxyHelper.isUnFetched(fieldValue)) {
                final ProxiedEntityReferenceList p = (ProxiedEntityReferenceList) fieldValue;
                final List<Key<?>> getKeysAsList = p.__getKeysAsList();
                for (final Key<?> key : getKeysAsList) {
                    addValue(values, key, mapper, refAnn.idOnly());
                }
            } else {

                if (mf.getType().isArray()) {
                    for (final Object o : (Object[]) fieldValue) {
                        addValue(values, o, mapper, refAnn.idOnly());
                    }
                } else {
                    for (final Object o : (Iterable) fieldValue) {
                        addValue(values, o, mapper, refAnn.idOnly());
                    }
                }
            }
            if (!values.isEmpty() || mapper.getOptions().isStoreEmpties()) {
                dbObject.put(name, values);
            }
        }
    }

    private void addValue(final List values, final Object o, final Mapper mapper, final boolean idOnly) {
        if (o == null && mapper.getOptions().isStoreNulls()) {
            values.add(null);
            return;
        }

        final Key key = o instanceof Key
                        ? (Key) o
                        : getKey(o, mapper);
        values.add(idOnly
                   ? mapper.keyToManualRef(key)
                   : mapper.keyToRef(key));
    }

    private void writeMap(final MappedField mf, final DBObject dbObject, final String name, final Object fieldValue,
                          final Reference refAnn, final Mapper mapper) {
        final Map<Object, Object> map = (Map<Object, Object>) fieldValue;
        if ((map != null)) {
            final Map values = mapper.getOptions().getObjectFactory().createMap(mf);

            if (ProxyHelper.isProxy(map) && ProxyHelper.isUnFetched(map)) {
                final ProxiedEntityReferenceMap proxy = (ProxiedEntityReferenceMap) map;

                final Map<Object, Key<?>> refMap = proxy.__getReferenceMap();
                for (final Map.Entry<Object, Key<?>> entry : refMap.entrySet()) {
                    final Object key = entry.getKey();
                    values.put(key, refAnn.idOnly()
                                    ? mapper.keyToManualRef(entry.getValue())
                                    : mapper.keyToRef(entry.getValue()));
                }
            } else {
                for (final Map.Entry<Object, Object> entry : map.entrySet()) {
                    final String strKey = mapper.getConverters().encode(entry.getKey()).toString();
                    values.put(strKey, refAnn.idOnly()
                                       ? mapper.keyToManualRef(getKey(entry.getValue(), mapper))
                                       : mapper.keyToRef(getKey(entry.getValue(), mapper)));
                }
            }
            if (!values.isEmpty() || mapper.getOptions().isStoreEmpties()) {
                dbObject.put(name, values);
            }
        }
    }

    private Key<?> getKey(final Object entity, final Mapper mapper) {
        try {
            if (entity instanceof ProxiedEntityReference) {
                final ProxiedEntityReference proxy = (ProxiedEntityReference) entity;
                return proxy.__getKey();
            }
            final MappedClass mappedClass = mapper.getMappedClass(entity);
            final Object id = mappedClass.getIdField().get(entity);
            if (id == null) {
                throw new MappingException("@Id field cannot be null!");
            }
            return new Key(mappedClass.getClazz(), mappedClass.getCollectionName(), id);
        } catch (IllegalAccessException iae) {
            throw new RuntimeException(iae);
        }
    }

    /**
     * @deprecated use void fromDBObject(final DBObject dbObject, final MappedField mf, final Object entity, EntityCache cache) instead.
     */
    @Deprecated
    void fromDBObject(final DBObject dbObject, final MappedField mf, final Object entity, final Mapper mapper) {
        fromDBObject(dbObject, mf, entity, mapper.createEntityCache(), mapper);
    }

    public void fromDBObject(final DBObject dbObject, final MappedField mf, final Object entity, final EntityCache cache,
                             final Mapper mapper) {
        final Class fieldType = mf.getType();

        final Reference refAnn = mf.getAnnotation(Reference.class);
        if (mf.isMap()) {
            readMap(dbObject, mf, entity, refAnn, cache, mapper);
        } else if (mf.isMultipleValues()) {
            readCollection(dbObject, mf, entity, refAnn, cache, mapper);
        } else {
            readSingle(dbObject, mf, entity, fieldType, refAnn, cache, mapper);
        }

    }

    private void readSingle(final DBObject dbObject, final MappedField mf, final Object entity, final Class fieldType,
                            final Reference refAnn, final EntityCache cache, final Mapper mapper) {

        final Object ref = mf.getDbObjectValue(dbObject);
        if (ref != null) {
            Object resolvedObject;
            if (refAnn.lazy() && LazyFeatureDependencies.assertDependencyFullFilled()) {
                resolvedObject = createOrReuseProxy(fieldType, ref, cache, mapper, refAnn.idOnly());
            } else {
                resolvedObject = resolveObject(ref, mf, cache, mapper, refAnn.idOnly());
            }

            if (resolvedObject != null) {
                mf.setFieldValue(entity, resolvedObject);
            }

        }
    }

    private void readCollection(final DBObject dbObject, final MappedField mf, final Object entity, final Reference refAnn,
                                final EntityCache cache, final Mapper mapper) {
        // multiple references in a List
        final Class referenceObjClass = mf.getSubClass();
        Collection references = mf.isSet() ? mapper.getOptions().getObjectFactory().createSet(mf)
                                           : mapper.getOptions().getObjectFactory().createList(mf);

        if (refAnn.lazy() && LazyFeatureDependencies.assertDependencyFullFilled()) {
            final Object dbVal = mf.getDbObjectValue(dbObject);
            if (dbVal != null) {
                references = mapper.getProxyFactory()
                                   .createListProxy(references, referenceObjClass, refAnn.ignoreMissing(), mapper.getDatastoreProvider());
                final ProxiedEntityReferenceList referencesAsProxy = (ProxiedEntityReferenceList) references;

                if (dbVal instanceof List) {
                    referencesAsProxy.__addAll(refAnn.idOnly()
                                                   ? mapper.getKeysByManualRefs(referenceObjClass, (List) dbVal)
                                                   : mapper.getKeysByRefs((List) dbVal));
                } else {
                    referencesAsProxy.__add(refAnn.idOnly()
                                            ? mapper.manualRefToKey(referenceObjClass, dbVal)
                                            : mapper.refToKey((DBRef) dbVal));
                }
            }
        } else {
            final Object dbVal = mf.getDbObjectValue(dbObject);
            final Collection refs = references;
            new IterHelper<String, Object>().loopOrSingle(dbVal, new IterCallback<Object>() {
                @Override
                public void eval(final Object val) {
                    final Object ent = resolveObject(val, mf, cache, mapper, refAnn.idOnly());
                    if (ent == null) {
                        LOG.warning("Null reference found when retrieving value for " + mf.getFullName());
                    } else {
                        refs.add(ent);
                    }
                }
            });
        }

        if (mf.getType().isArray()) {
            mf.setFieldValue(entity, ReflectionUtils.convertToArray(mf.getSubClass(), ReflectionUtils.iterToList(references)));
        } else {
            mf.setFieldValue(entity, references);
        }
    }

    Object resolveObject(final Object ref, final MappedField mf, final EntityCache cache, final Mapper mapper,
                         final boolean idOnly) {
        if (ref == null) {
            return null;
        }

        final DBRef dbRef = idOnly ? null : (DBRef) ref;
        final Key key = mapper.createKey(mf.isSingleValue() ? mf.getType() : mf.getSubClass(),
                                         idOnly ? ref : dbRef.getId());

        final Object cached = cache.getEntity(key);
        if (cached != null) {
            return cached;
        }

        final Datastore ds = mapper.getDatastoreProvider().get();

        final DBObject refDbObject = idOnly ? ds.getCollection(key.getType()).findOne(ref)
                                            : ds.getDB().getCollection(dbRef.getCollectionName()).findOne(dbRef.getId());

        if (refDbObject != null) {
            Object refObj = mapper.getOptions().getObjectFactory().createInstance(mapper, mf, refDbObject);
            refObj = mapper.fromDb(refDbObject, refObj, cache);
            cache.putEntity(key, refObj);
            return refObj;
        }

        final boolean ignoreMissing = mf.getAnnotation(Reference.class) != null && mf.getAnnotation(Reference.class).ignoreMissing();
        if (!ignoreMissing) {
            throw new MappingException("The reference(" + ref.toString() + ") could not be fetched for " + mf.getFullName());
        } else {
            return null;
        }
    }

    private void readMap(final DBObject dbObject, final MappedField mf, final Object entity, final Reference refAnn,
                         final EntityCache cache,
                         final Mapper mapper) {
        final Class referenceObjClass = mf.getSubClass();
        Map m = mapper.getOptions().getObjectFactory().createMap(mf);

        final DBObject dbVal = (DBObject) mf.getDbObjectValue(dbObject);
        if (dbVal != null) {
            if (refAnn.lazy() && LazyFeatureDependencies.assertDependencyFullFilled()) {
                // replace map by proxy to it.
                m = mapper.getProxyFactory().createMapProxy(m, referenceObjClass, refAnn.ignoreMissing(), mapper.getDatastoreProvider());
            }

            final Map map = m;
            new IterHelper<Object, Object>().loopMap(dbVal, new MapIterCallback<Object, Object>() {
                @Override
                public void eval(final Object key, final Object val) {

                    final Object objKey = mapper.getConverters().decode(mf.getMapKeyClass(), key, mf);

                    if (refAnn.lazy() && LazyFeatureDependencies.assertDependencyFullFilled()) {
                        final ProxiedEntityReferenceMap proxiedMap = (ProxiedEntityReferenceMap) map;
                        proxiedMap.__put(objKey, refAnn.idOnly()
                                                 ? mapper.manualRefToKey(referenceObjClass, val)
                                                 : mapper.refToKey((DBRef) val));
                    } else {
                        final Object resolvedObject = resolveObject(val, mf, cache, mapper, refAnn.idOnly());
                        map.put(objKey, resolvedObject);
                    }
                }
            });
        }
        mf.setFieldValue(entity, m);
    }

    private Object createOrReuseProxy(final Class referenceObjClass, final Object ref, final EntityCache cache, final Mapper mapper,
                                      final boolean idOnly) {
        final Key key = idOnly ? mapper.manualRefToKey(referenceObjClass, ref) : mapper.refToKey((DBRef) ref);
        final Object proxyAlreadyCreated = cache.getProxy(key);
        if (proxyAlreadyCreated != null) {
            return proxyAlreadyCreated;
        }
        final Object newProxy = mapper.getProxyFactory().createProxy(referenceObjClass, key, mapper.getDatastoreProvider());
        cache.putProxy(key, newProxy);
        return newProxy;
    }
}
