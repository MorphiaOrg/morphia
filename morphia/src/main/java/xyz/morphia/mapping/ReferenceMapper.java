package xyz.morphia.mapping;


import com.mongodb.BasicDBList;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.DBRef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.morphia.Datastore;
import xyz.morphia.Key;
import xyz.morphia.annotations.Reference;
import xyz.morphia.mapping.cache.EntityCache;
import xyz.morphia.mapping.experimental.ListReference;
import xyz.morphia.mapping.experimental.MapReference;
import xyz.morphia.mapping.experimental.MorphiaReference;
import xyz.morphia.mapping.experimental.SetReference;
import xyz.morphia.mapping.experimental.SingleReference;
import xyz.morphia.mapping.lazy.LazyFeatureDependencies;
import xyz.morphia.mapping.lazy.proxy.ProxiedEntityReference;
import xyz.morphia.mapping.lazy.proxy.ProxiedEntityReferenceList;
import xyz.morphia.mapping.lazy.proxy.ProxiedEntityReferenceMap;
import xyz.morphia.mapping.lazy.proxy.ProxyHelper;
import xyz.morphia.utils.IterHelper;
import xyz.morphia.utils.IterHelper.IterCallback;
import xyz.morphia.utils.IterHelper.MapIterCallback;
import xyz.morphia.utils.ReflectionUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @morphia.internal
 */
@SuppressWarnings({"unchecked", "rawtypes"})
class ReferenceMapper implements CustomMapper {
    public static final Logger LOG = LoggerFactory.getLogger(ReferenceMapper.class);

    @Override
    public void fromDBObject(final Datastore datastore, final DBObject dbObject, final MappedField mf, final Object entity,
                             final EntityCache cache, final Mapper mapper) {
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
    public void toDBObject(final Object entity, final MappedField mf, final DBObject dbObject, final Map<Object, DBObject> involvedObjects,
                           final Mapper mapper) {
        final String name = mf.getNameToStore();

        final Object fieldValue = mf.getFieldValue(entity);

        if (fieldValue == null && !mapper.getOptions().isStoreNulls()) {
            return;
        }

        if(fieldValue instanceof MorphiaReference && !mf.getTypeParameters().isEmpty()) {
            writeMorphiaReferenceValues(mf, dbObject, mapper, name, fieldValue);
        } else {
            final Reference refAnn = mf.getAnnotation(Reference.class);
            if (mf.isMap()) {
                writeMap(mf, dbObject, name, fieldValue, refAnn, mapper);
            } else if (mf.isMultipleValues()) {
                writeCollection(mf, dbObject, name, fieldValue, refAnn, mapper);
            } else {
                writeSingle(dbObject, name, fieldValue, refAnn, mapper);
            }
        }
    }

    public void readMorphiaReferenceValues(Mapper mapper, Datastore datastore, MappedField mappedField, DBObject dbObject, Object entity) {
        final Class paramType = mappedField.getTypeParameters().get(0).getType();
        MorphiaReference<?> reference = null;
        if (Map.class.isAssignableFrom(paramType)) {
            final Class subType = mappedField.getTypeParameters().get(0).getSubClass();

            final Map<String, Object> ids = (Map<String, Object>) mappedField.getDbObjectValue(dbObject);
            if(ids != null) {
                final Collection<Object> values = ids.values();
                final Object first = values.iterator().next();
                String collection = null;
                if (first instanceof DBRef) {
                    collection = ((DBRef) first).getCollectionName();
                }

                reference = new MapReference(datastore, mapper.getMappedClass(subType), collection, ids);
            }
        } else if (Collection.class.isAssignableFrom(paramType)) {
            final BasicDBList dbVal = (BasicDBList) mappedField.getDbObjectValue(dbObject);
            if (dbVal != null) {
                final Class subType = mappedField.getTypeParameters().get(0).getSubClass();
                final MappedClass mappedClass = mapper.getMappedClass(subType);
                String collection = null;
                if(!dbVal.isEmpty() && dbVal.get(0) instanceof DBRef) {
                    collection = ((DBRef) dbVal.get(0)).getCollectionName();
                }
                if (Set.class.isAssignableFrom(paramType)) {
                    reference = new SetReference(datastore, mappedClass, collection, dbVal);
                } else {
                    reference = new ListReference(datastore, mappedClass, collection, dbVal);
                }
            }
        } else {
            final MappedClass mappedClass = mapper.getMappedClass(paramType);
            final MappedField idField = mappedClass.getMappedIdField();
            Object id = dbObject.get(mappedField.getMappedFieldName());
            String collection = null;
            if (id instanceof DBRef) {
                collection = ((DBRef) id).getCollectionName();
                id = mapper.getConverters().decode(idField.getConcreteType(), ((DBRef) id).getId(), mappedField);
            }
            reference = new SingleReference(datastore, mappedClass, collection, id);
        }
        mappedField.setFieldValue(entity, reference);
    }

    public void writeMorphiaReferenceValues(final MappedField mf, final DBObject dbObject, final Mapper mapper, final String name,
                                            final Object fieldValue) {
        final Class paramType = mf.getTypeParameters().get(0).getType();
        boolean notNull;
        boolean notEmpty;
        Object value;
        if (Map.class.isAssignableFrom(paramType)) {
            final Map map = (Map) ((MapReference) fieldValue).encode(mapper, fieldValue, mf);
            value = map;
            notNull = map != null;
            notEmpty = notNull && !map.isEmpty();
        } else if (Collection.class.isAssignableFrom(paramType)) {
            final Collection collection = (Collection) ((MorphiaReference) fieldValue).encode(mapper, fieldValue, mf);
            value = collection;
            notNull = collection != null;
            notEmpty = notNull && !collection.isEmpty();
        } else {
            value = mapper.getConverters().encode(mf.getConcreteType(), fieldValue);
            notNull = value != null;
            notEmpty = true;
        }
        if ((notNull || mapper.getOptions().isStoreNulls()) &&
             (notEmpty || mapper.getOptions().isStoreEmpties())) {
            dbObject.put(name, value);
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
                   ? mapper.keyToId(key)
                   : mapper.keyToDBRef(key));
    }

    private Object createOrReuseProxy(final Datastore datastore, final Mapper mapper, final Class referenceObjClass, final Object ref,
                                      final EntityCache cache, final Reference anntotation) {
        final Key key = anntotation.idOnly() ? mapper.manualRefToKey(referenceObjClass, ref) : mapper.refToKey((DBRef) ref);
        final Object proxyAlreadyCreated = cache.getProxy(key);
        if (proxyAlreadyCreated != null) {
            return proxyAlreadyCreated;
        }
        final Object newProxy = mapper.getProxyFactory().createProxy(datastore, referenceObjClass, key, anntotation.ignoreMissing());
        cache.putProxy(key, newProxy);
        return newProxy;
    }

    private Key<?> getKey(final Object entity, final Mapper mapper) {
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
            return new Key(mappedClass.getClazz(), mappedClass.getCollectionName(), id);
        } catch (IllegalAccessException iae) {
            throw new RuntimeException(iae);
        }
    }

    private void readCollection(final Datastore datastore, final Mapper mapper, final DBObject dbObject, final MappedField mf,
                                final Object entity, final Reference refAnn, final EntityCache cache) {
        Object value = null;
        Collection references = mf.isSet() ? mapper.getOptions().getObjectFactory().createSet(mf)
                                           : mapper.getOptions().getObjectFactory().createList(mf);
        value = references;
        // multiple references in a List
        final Class referenceObjClass = mf.getSubClass();
        // load reference class.  this "fixes" #816
        mapper.getMappedClass(referenceObjClass);

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
                public void eval(final Object val) {
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
            mf.setFieldValue(entity, value);
        }
    }

    private void readMap(final Datastore datastore, final Mapper mapper, final Object entity, final Reference refAnn,
                         final EntityCache cache, final MappedField mf, final DBObject dbObject) {
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
                public void eval(final Object k, final Object val) {
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

    private void readSingle(final Datastore datastore, final Mapper mapper, final Object entity, final Class fieldType,
                            final Reference annotation, final EntityCache cache, final MappedField mf, final DBObject dbObject) {

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

    private void writeMap(final MappedField mf, final DBObject dbObject, final String name, final Object fieldValue,
                          final Reference refAnn, final Mapper mapper) {
        final Map<Object, Object> map = (Map<Object, Object>) fieldValue;
        if (map != null) {
            final Map values = mapper.getOptions().getObjectFactory().createMap(mf);

            if (ProxyHelper.isProxy(map) && ProxyHelper.isUnFetched(map)) {
                final ProxiedEntityReferenceMap proxy = (ProxiedEntityReferenceMap) map;

                final Map<Object, Key<?>> refMap = proxy.__getReferenceMap();
                for (final Map.Entry<Object, Key<?>> entry : refMap.entrySet()) {
                    final Object key = entry.getKey();
                    values.put(key, refAnn.idOnly()
                                    ? mapper.keyToId(entry.getValue())
                                    : mapper.keyToDBRef(entry.getValue()));
                }
            } else {
                for (final Map.Entry<Object, Object> entry : map.entrySet()) {
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

    private void writeSingle(final DBObject dbObject, final String name, final Object fieldValue, final Reference refAnn,
                             final Mapper mapper) {
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

                dbObject.put(name, id);
            } else {
                dbObject.put(name, mapper.keyToDBRef(key));
            }
        }
    }

    Object resolveObject(final Datastore datastore, final Mapper mapper, final EntityCache cache, final MappedField mf,
                         final boolean idOnly, final Object ref) {
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
            throw new MappingException("The reference(" + ref.toString() + ") could not be fetched for " + mf.getFullName());
        } else {
            return null;
        }
    }
}
