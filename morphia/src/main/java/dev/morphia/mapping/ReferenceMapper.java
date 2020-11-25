package dev.morphia.mapping;


import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.DBRef;
import dev.morphia.Datastore;
import dev.morphia.Key;
import dev.morphia.annotations.Reference;
import dev.morphia.mapping.cache.EntityCache;
import dev.morphia.mapping.experimental.CollectionReference;
import dev.morphia.mapping.experimental.MapReference;
import dev.morphia.mapping.experimental.MorphiaReference;
import dev.morphia.mapping.experimental.SingleReference;
import dev.morphia.mapping.lazy.LazyFeatureDependencies;
import dev.morphia.mapping.lazy.proxy.ProxiedEntityReference;
import dev.morphia.mapping.lazy.proxy.ProxiedEntityReferenceList;
import dev.morphia.mapping.lazy.proxy.ProxiedEntityReferenceMap;
import dev.morphia.mapping.lazy.proxy.ProxyHelper;
import dev.morphia.utils.IterHelper;
import dev.morphia.utils.IterHelper.IterCallback;
import dev.morphia.utils.IterHelper.MapIterCallback;
import dev.morphia.utils.ReflectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @morphia.internal
 * @deprecated
 */
@Deprecated
@SuppressWarnings({"unchecked", "rawtypes"})
class ReferenceMapper implements CustomMapper {
    public static final Logger LOG = LoggerFactory.getLogger(ReferenceMapper.class);

    @Override
    public void fromDBObject(Datastore datastore, DBObject dbObject, MappedField mf, Object entity,
                             EntityCache cache, Mapper mapper) {
        final Class fieldType = mf.getType();

        if (mf.getType().equals(MorphiaReference.class) && !mf.getTypeParameters().isEmpty()) {
            readMorphiaReferenceValues(mapper, datastore, mf, dbObject, entity);
        } else {
            final Reference refAnn = mf.getAnnotation(Reference.class);
            if (mf.isMap()) {
                readMap(datastore, mapper, entity, refAnn, cache, mf, dbObject);
            } else if (mf.isMultipleValues()) {
                readCollection(datastore, mapper, dbObject, mf, entity, refAnn, cache);
            } else {
                readSingle(datastore, mapper, entity, fieldType, refAnn, cache, mf, dbObject);
            }
        }

    }

    @Override
    public void toDBObject(Object entity, MappedField mf, DBObject dbObject, Map<Object, DBObject> involvedObjects,
                           Mapper mapper) {
        final String name = mf.getNameToStore();

        final Object fieldValue = mf.getFieldValue(entity);

        if (fieldValue == null && !mapper.getOptions().isStoreNulls()) {
            return;
        }

        if (fieldValue instanceof MorphiaReference && !mf.getTypeParameters().isEmpty()) {
            writeMorphiaReferenceValues(dbObject, mf, fieldValue, name, mapper);
        } else {
            final Reference refAnn = mf.getAnnotation(Reference.class);
            if (mf.isMap()) {
                writeMap(mf, dbObject, name, fieldValue, refAnn, mapper);
            } else if (mf.isMultipleValues()) {
                writeCollection(mf, dbObject, name, fieldValue, refAnn, mapper);
            } else {
                writeSingle(mf, dbObject, name, fieldValue, refAnn, mapper);
            }
        }

    }

    private void addValue(List values, Object o, Mapper mapper, boolean idOnly) {
        if (o == null && mapper.getOptions().isStoreNulls()) {
            values.add(null);
            return;
        }

        final Key key = o instanceof Key
                        ? (Key) o
                        : getKey(o, mapper);
        values.add(idOnly
                   ? mapper.keyToId(key)
                   : mapper.keyToDBRef(key));
    }

    private Object createOrReuseProxy(Datastore datastore, Mapper mapper, Class referenceObjClass, Object ref,
                                      EntityCache cache, Reference anntotation) {
        final Key key = anntotation.idOnly() ? mapper.manualRefToKey(referenceObjClass, ref) : mapper.refToKey((DBRef) ref);
        final Object proxyAlreadyCreated = cache.getProxy(key);
        if (proxyAlreadyCreated != null) {
            return proxyAlreadyCreated;
        }
        final Object newProxy = mapper.getProxyFactory().createProxy(datastore, referenceObjClass, key, anntotation.ignoreMissing());
        cache.putProxy(key, newProxy);
        return newProxy;
    }

    private Key<?> getKey(Object entity, Mapper mapper) {
        try {
            if (entity instanceof ProxiedEntityReference) {
                final ProxiedEntityReference proxy = (ProxiedEntityReference) entity;
                return proxy.__getKey();
            }
            final MappedClass mappedClass = mapper.getMappedClass(entity);
            Object id = mappedClass.getIdField().get(entity);
            if (id == null) {
                throw new MappingException("@Id field cannot be null!");
            }
            Class<?> idType = mappedClass.getIdField().getType();
            id = mapper.getConverters()
                       .encode(idType, id);
            return new Key(mappedClass.getClazz(), mappedClass.getCollectionName(), id);
        } catch (IllegalAccessException iae) {
            throw new RuntimeException(iae);
        }
    }

    private void readCollection(Datastore datastore, Mapper mapper, DBObject dbObject, MappedField mf,
                                Object entity,
                                Reference refAnn,
                                EntityCache cache) {
        // multiple references in a List
        final Class referenceObjClass = mf.getSubClass();
        // load reference class.  this "fixes" #816
        mapper.getMappedClass(referenceObjClass);
        Collection references = mf.isSet() ? mapper.getOptions().getObjectFactory().createSet(mf)
                                           : mapper.getOptions().getObjectFactory().createList(mf);

        if (refAnn.lazy() && LazyFeatureDependencies.assertDependencyFullFilled()) {
            final Object dbVal = mf.getDbObjectValue(dbObject);
            if (dbVal != null) {
                references = mapper.getProxyFactory()
                                   .createListProxy(datastore, references, referenceObjClass, refAnn.ignoreMissing());
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
                public void eval(Object val) {
                    final Object ent = resolveObject(datastore, mapper, cache, mf, refAnn.idOnly(), val);
                    if (ent == null) {
                        LOG.warn("Null reference found when retrieving value for " + mf.getFullName());
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

    private void readMap(Datastore datastore, Mapper mapper, Object entity, Reference refAnn,
                         EntityCache cache, MappedField mf, DBObject dbObject) {
        final Class referenceObjClass = mf.getSubClass();
        Map m = mapper.getOptions().getObjectFactory().createMap(mf);

        final DBObject dbVal = (DBObject) mf.getDbObjectValue(dbObject);
        if (dbVal != null) {
            if (refAnn.lazy() && LazyFeatureDependencies.assertDependencyFullFilled()) {
                // replace map by proxy to it.
                m = mapper.getProxyFactory().createMapProxy(datastore, m, referenceObjClass, refAnn.ignoreMissing());
            }

            final Map map = m;
            new IterHelper<Object, Object>().loopMap(dbVal, new MapIterCallback<Object, Object>() {
                @Override
                public void eval(Object k, Object val) {

                    final Object objKey = mapper.getConverters().decode(mf.getMapKeyClass(), k, mf);

                    if (refAnn.lazy() && LazyFeatureDependencies.assertDependencyFullFilled()) {
                        final ProxiedEntityReferenceMap proxiedMap = (ProxiedEntityReferenceMap) map;
                        proxiedMap.__put(objKey, refAnn.idOnly()
                                                 ? mapper.manualRefToKey(referenceObjClass, val)
                                                 : mapper.refToKey((DBRef) val));
                    } else {
                        map.put(objKey, resolveObject(datastore, mapper, cache, mf, refAnn.idOnly(), val));
                    }
                }
            });
        }
        mf.setFieldValue(entity, m);
    }

    private void readSingle(Datastore datastore, Mapper mapper, Object entity, Class fieldType,
                            Reference annotation, EntityCache cache, MappedField mf, DBObject dbObject) {

        final Object ref = mf.getDbObjectValue(dbObject);
        if (ref != null) {
            Object resolvedObject;
            if (annotation.lazy() && LazyFeatureDependencies.assertDependencyFullFilled()) {
                resolvedObject = createOrReuseProxy(datastore, mapper, fieldType, ref, cache, annotation);
            } else {
                resolvedObject = resolveObject(datastore, mapper, cache, mf, annotation.idOnly(), ref);
            }

            if (resolvedObject != null) {
                mf.setFieldValue(entity, resolvedObject);
            }

        }
    }

    private void writeCollection(MappedField mf, DBObject dbObject, String name, Object fieldValue,
                                 Reference refAnn, Mapper mapper) {
        if (fieldValue != null) {
            final List values = new ArrayList();

            if (ProxyHelper.isProxy(fieldValue) && ProxyHelper.isUnFetched(fieldValue)) {
                final ProxiedEntityReferenceList p = (ProxiedEntityReferenceList) fieldValue;
                final List<Key<?>> getKeysAsList = p.__getKeysAsList();
                for (Key<?> key : getKeysAsList) {
                    addValue(values, key, mapper, refAnn.idOnly());
                }
            } else {

                if (mf.getType().isArray()) {
                    for (Object o : (Object[]) fieldValue) {
                        addValue(values, o, mapper, refAnn.idOnly());
                    }
                } else {
                    for (Object o : (Iterable) fieldValue) {
                        addValue(values, o, mapper, refAnn.idOnly());
                    }
                }
            }
            if (!values.isEmpty() || mapper.getOptions().isStoreEmpties()) {
                dbObject.put(name, values);
            }
        }
    }

    private void writeMap(MappedField mf, DBObject dbObject, String name, Object fieldValue,
                          Reference refAnn, Mapper mapper) {
        final Map<Object, Object> map = (Map<Object, Object>) fieldValue;
        if (map != null) {
            final Map values = mapper.getOptions().getObjectFactory().createMap(mf);

            if (ProxyHelper.isProxy(map) && ProxyHelper.isUnFetched(map)) {
                final ProxiedEntityReferenceMap proxy = (ProxiedEntityReferenceMap) map;

                final Map<Object, Key<?>> refMap = proxy.__getReferenceMap();
                for (Map.Entry<Object, Key<?>> entry : refMap.entrySet()) {
                    final Object key = entry.getKey();
                    values.put(key, refAnn.idOnly()
                                    ? mapper.keyToId(entry.getValue())
                                    : mapper.keyToDBRef(entry.getValue()));
                }
            } else {
                for (Map.Entry<Object, Object> entry : map.entrySet()) {
                    final String strKey = mapper.getConverters().encode(entry.getKey()).toString();
                    values.put(strKey, refAnn.idOnly()
                                       ? mapper.keyToId(getKey(entry.getValue(), mapper))
                                       : mapper.keyToDBRef(getKey(entry.getValue(), mapper)));
                }
            }
            if (!values.isEmpty() || mapper.getOptions().isStoreEmpties()) {
                dbObject.put(name, values);
            }
        }
    }

    private void writeSingle(MappedField mf, DBObject dbObject, String name, Object fieldValue,
                             Reference refAnn, Mapper mapper) {
        if (fieldValue == null) {
            if (mapper.getOptions().isStoreNulls()) {
                dbObject.put(name, null);
            }
        } else {
            Key<?> key = getKey(fieldValue, mapper);
            if (refAnn.idOnly()) {
                Object id = mapper.keyToId(key);
                if (id != null && mapper.isMapped(id.getClass())) {
                    id = mapper.toMongoObject(id, true);
                }
                MappedClass refEntityType = mapper.getMappedClass(mf.getConcreteType());
                Class<?> idType = refEntityType.getIdField().getType();
                id = mapper.getConverters()
                           .encode(idType, id);


                dbObject.put(name, id);
            } else {
                dbObject.put(name, mapper.keyToDBRef(key));
            }
        }
    }

    void readMorphiaReferenceValues(Mapper mapper, Datastore datastore, MappedField mappedField,
                                    DBObject dbObject, Object entity) {
        final Class paramType = mappedField.getTypeParameters().get(0).getType();
        MorphiaReference<?> reference;
        if (Map.class.isAssignableFrom(paramType)) {
            reference = MapReference.decode(datastore, mapper, mappedField, dbObject);
        } else if (Collection.class.isAssignableFrom(paramType)) {
            reference = CollectionReference.decode(datastore, mapper, mappedField, paramType, dbObject);
        } else {
            reference = SingleReference.decode(datastore, mapper, mappedField, paramType, dbObject);
        }
        mappedField.setFieldValue(entity, reference);
    }

    Object resolveObject(Datastore datastore, Mapper mapper, EntityCache cache, MappedField mf,
                         boolean idOnly, Object ref) {
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

        final DBObject refDbObject;
        DBCollection collection;
        Object id;

        if (idOnly) {
            collection = datastore.getCollection(key.getType());
            id = ref;
        } else {
            collection = datastore.getDB().getCollection(dbRef.getCollectionName());
            id = dbRef.getId();
        }
        if (id instanceof DBObject) {
            ((DBObject) id).removeField(mapper.getOptions().getDiscriminatorField());
        }
        refDbObject = collection.findOne(id);

        if (refDbObject != null) {
            Object refObj = mapper.getOptions().getObjectFactory().createInstance(mapper, mf, refDbObject);
            refObj = mapper.fromDb(datastore, refDbObject, refObj, cache);
            cache.putEntity(key, refObj);
            return refObj;
        }

        final boolean ignoreMissing = mf.getAnnotation(Reference.class) != null && mf.getAnnotation(Reference.class).ignoreMissing();
        if (!ignoreMissing) {
            throw new MappingException(String.format("The reference (%s) could not be fetched for %s", ref, mf.getFullName()));
        } else {
            return null;
        }
    }

    void writeMorphiaReferenceValues(DBObject dbObject, MappedField mf, Object fieldValue, String name,
                                     Mapper mapper) {
        final Class paramType = mf.getTypeParameters().get(0).getType();

        boolean notEmpty = true;
        final Object value = ((MorphiaReference) fieldValue).encode(mapper, fieldValue, mf);
        final boolean notNull = value != null;

        if (Map.class.isAssignableFrom(paramType)) {
            notEmpty = notNull && !((Map) value).isEmpty();
        } else if (Collection.class.isAssignableFrom(paramType)) {
            notEmpty = notNull && !((Collection) value).isEmpty();
        }

        if ((notNull || mapper.getOptions().isStoreNulls())
            && (notEmpty || mapper.getOptions().isStoreEmpties())) {
            dbObject.put(name, value);
        }
    }
}
