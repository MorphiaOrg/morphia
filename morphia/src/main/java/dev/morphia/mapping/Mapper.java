/**
 * Copyright (C) 2010 Olafur Gauti Gudmundsson Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless
 * required
 * by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
 * OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations
 * under the License.
 */


package dev.morphia.mapping;


import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.DBRef;
import dev.morphia.Datastore;
import dev.morphia.EntityInterceptor;
import dev.morphia.Key;
import dev.morphia.annotations.Converters;
import dev.morphia.annotations.Embedded;
import dev.morphia.annotations.NotSaved;
import dev.morphia.annotations.PostLoad;
import dev.morphia.annotations.PreLoad;
import dev.morphia.annotations.PrePersist;
import dev.morphia.annotations.PreSave;
import dev.morphia.annotations.Property;
import dev.morphia.annotations.Reference;
import dev.morphia.annotations.Serialized;
import dev.morphia.converters.CustomConverters;
import dev.morphia.converters.TypeConverter;
import dev.morphia.mapping.cache.DefaultEntityCache;
import dev.morphia.mapping.cache.EntityCache;
import dev.morphia.mapping.experimental.MorphiaReference;
import dev.morphia.mapping.lazy.LazyFeatureDependencies;
import dev.morphia.mapping.lazy.LazyProxyFactory;
import dev.morphia.mapping.lazy.proxy.ProxiedEntityReference;
import dev.morphia.mapping.lazy.proxy.ProxyHelper;
import dev.morphia.query.Query;
import dev.morphia.query.QueryImpl;
import dev.morphia.query.ValidationException;
import org.bson.BSONEncoder;
import org.bson.BasicBSONEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

import static dev.morphia.utils.ReflectionUtils.getParameterizedClass;
import static dev.morphia.utils.ReflectionUtils.implementsInterface;
import static dev.morphia.utils.ReflectionUtils.isPropertyType;
import static java.lang.String.format;


/**
 * @morphia.internal
 * @deprecated this class will be internalized in 2.0
 */
@SuppressWarnings({"unchecked", "rawtypes"})
@Deprecated
public class Mapper {
    /**
     * The @{@link dev.morphia.annotations.Id} field name that is stored with mongodb.
     *
     * @deprecated use "_id" directly
     */
    @Deprecated
    public static final String ID_KEY = "_id";

    /**
     * Special name that can never be used. Used as default for some fields to indicate default state.
     *
     * @morphia.internal
     */
    public static final String IGNORED_FIELDNAME = ".";

    /**
     * Special field used by morphia to support various possibly loading issues; will be replaced when discriminators are implemented to
     * support polymorphism
     *
     * @deprecated
     */
    @Deprecated
    public static final String CLASS_NAME_FIELDNAME = "className";

    private static final Logger LOG = LoggerFactory.getLogger(Mapper.class);
    /**
     * Set of classes that registered by this mapper
     */
    private final Map<String, MappedClass> mappedClasses = new ConcurrentHashMap<String, MappedClass>();
    private final ConcurrentHashMap<String, Set<MappedClass>> mappedClassesByCollection = new ConcurrentHashMap<String, Set<MappedClass>>();

    //EntityInterceptors; these are called after EntityListeners and lifecycle methods on an Entity, for all Entities
    private final List<EntityInterceptor> interceptors = new LinkedList<EntityInterceptor>();

    //A general cache of instances of classes; used by MappedClass for EntityListener(s)
    private final Map<Class, Object> instanceCache = new ConcurrentHashMap();
    // TODO: make these configurable
    private final LazyProxyFactory proxyFactory = LazyFeatureDependencies.createDefaultProxyFactory();
    private final dev.morphia.converters.Converters converters;
    private MapperOptions opts = MapperOptions.builder().build();

    /**
     * Creates a Mapper with the given options.
     *
     * @param opts the options to use
     */
    public Mapper(final MapperOptions opts) {
        this();
        this.opts = opts;
    }

    /**
     * Creates a Mapper with default options
     *
     * @see MapperOptions
     */
    public Mapper() {
        converters = new CustomConverters(this);
    }

    /**
     * Creates a new Mapper with the given options and a Mapper to copy.  This is effectively a copy constructor that allows a developer
     * to override the options.
     *
     * @param options the options to use
     * @param mapper  the collection of MappedClasses to add
     */
    public Mapper(final MapperOptions options, final Mapper mapper) {
        this(options);
        for (final MappedClass mappedClass : mapper.getMappedClasses()) {
            addMappedClass(mappedClass, false);
        }
    }

    /**
     * Adds an {@link EntityInterceptor}
     *
     * @param ei the interceptor to add
     */
    public void addInterceptor(final EntityInterceptor ei) {
        interceptors.add(ei);
    }

    /**
     * Creates a MappedClass and validates it.
     *
     * @param c the Class to map
     * @return the MappedClass for the given Class
     */
    public MappedClass addMappedClass(final Class c) {

        MappedClass mappedClass = mappedClasses.get(c.getName());
        if (mappedClass == null) {
            mappedClass = new MappedClass(c, this);
            return addMappedClass(mappedClass, true);
        }
        return mappedClass;
    }

    /**
     * Creates a cache for tracking entities seen during processing
     *
     * @return the cache
     */
    public EntityCache createEntityCache() {
        return getOptions().isCachingEnabled()
               ? getOptions().getCacheFactory().createCache()
               : new DefaultEntityCache() {
                   @Override
                   public <T> void putEntity(final Key<T> k, final T t) {
                       LOG.info("entity caching disabled");
                   }

                   @Override
                   public <T> void putProxy(final Key<T> k, final T t) {
                       LOG.info("proxy caching disabled");
                   }
               };
    }

    /**
     * Converts a DBObject back to a type-safe java object (POJO)
     *
     * @param <T>         the type of the entity
     * @param datastore   the Datastore to use when fetching this reference
     * @param entityClass The type to return, or use; can be overridden by the @see Mapper.CLASS_NAME_FIELDNAME in the DBObject
     * @param dbObject    the DBObject containing the document from mongodb
     * @param cache       the EntityCache to use
     * @return the new entity
     * @morphia.internal
     * @deprecated no replacement is planned
     */
    @Deprecated
    public <T> T fromDBObject(final Datastore datastore, final Class<T> entityClass, final DBObject dbObject, final EntityCache cache) {
        if (dbObject == null) {
            return null;
        }

        return fromDb(datastore, dbObject, opts.getObjectFactory().createInstance(entityClass, dbObject), cache);
    }

    /**
     * Finds any subtypes for the given MappedClass.
     *
     * @param mc the parent type
     * @return the list of subtypes
     * @since 1.3
     */
    public List<MappedClass> getSubTypes(final MappedClass mc) {
        List<MappedClass> subtypes = new ArrayList<MappedClass>();
        for (MappedClass mappedClass : getMappedClasses()) {
            if (mappedClass.isSubType(mc)) {
                subtypes.add(mappedClass);
            }
        }

        return subtypes;
    }

    /**
     * Converts an entity (POJO) to a DBObject.  A special field will be added to keep track of the class type.
     *
     * @param datastore the Datastore to use when fetching this reference
     * @param dbObject  the DBObject
     * @param <T>       the type of the referenced entity
     * @return the entity
     * @morphia.internal
     * @deprecated no replacement is planned
     */
    @Deprecated
    <T> T fromDBObject(final Datastore datastore, final DBObject dbObject) {
        if (dbObject.containsField(opts.getDiscriminatorField())) {
            T entity = opts.getObjectFactory().createInstance(null, dbObject);
            entity = fromDb(datastore, dbObject, entity, createEntityCache());

            return entity;
        } else {
            throw new MappingException(format("The DBObject does not contain a %s key.  Determining entity type is impossible.",
                opts.getDiscriminatorField()));
        }
    }

    /**
     * Converts a DBObject back to a type-safe java object (POJO)
     *
     * @param <T>       the type of the entity
     * @param datastore the Datastore to use when fetching this reference
     * @param dbObject  the DBObject containing the document from mongodb
     * @param entity    the instance to populate
     * @param cache     the EntityCache to use
     * @return the entity
     * @morphia.internal
     * @deprecated no replacement is planned
     */
    @Deprecated
    public <T> T fromDb(final Datastore datastore, final DBObject dbObject, final T entity, final EntityCache cache) {
        //hack to bypass things and just read the value.
        if (entity instanceof MappedField) {
            readMappedField(datastore, (MappedField) entity, entity, cache, dbObject);
            return entity;
        }

        // check the history key (a key is the namespace + id)

        if (dbObject.containsField("_id") && getMappedClass(entity).getIdField() != null
            && getMappedClass(entity).getEntityAnnotation() != null) {
            final Key<T> key = new Key(entity.getClass(), getCollectionName(entity.getClass()), dbObject.get("_id"));
            final T cachedInstance = cache.getEntity(key);
            if (cachedInstance != null) {
                return cachedInstance;
            } else {
                cache.putEntity(key, entity); // to avoid stackOverflow in recursive refs
            }
        }

        if (entity instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) entity;
            for (String key : dbObject.keySet()) {
                Object o = dbObject.get(key);
                map.put(key, (o instanceof DBObject) ? fromDBObject(datastore, (DBObject) o) : o);
            }
        } else if (entity instanceof Collection) {
            Collection<Object> collection = (Collection<Object>) entity;
            for (Object o : ((List) dbObject)) {
                collection.add((o instanceof DBObject) ? fromDBObject(datastore, (DBObject) o) : o);
            }
        } else {
            final MappedClass mc = getMappedClass(entity);
            final DBObject updated = mc.callLifecycleMethods(PreLoad.class, entity, dbObject, this);
            try {
                for (final MappedField mf : mc.getPersistenceFields()) {
                    readMappedField(datastore, mf, entity, cache, updated);
                }
            } catch (final MappingException e) {
                Object id = dbObject.get("_id");
                String entityName = entity.getClass().getName();
                throw new MappingException(format("Could not map %s with ID: %s in database '%s'", entityName, id,
                    datastore.getDB().getName()), e);
            }

            if (updated.containsField("_id") && getMappedClass(entity).getIdField() != null) {
                final Key key = new Key(entity.getClass(), getCollectionName(entity.getClass()), updated.get("_id"));
                cache.putEntity(key, entity);
            }
            mc.callLifecycleMethods(PostLoad.class, entity, updated, this);
        }
        return entity;
    }

    /**
     * Looks up the class mapped to a named collection.
     *
     * @param collection the collection name
     * @return the Class mapped to this collection name
     * @morphia.internal
     * @deprecated no replacement is planned
     */
    @Deprecated
    public Class<?> getClassFromCollection(final String collection) {
        final Set<MappedClass> mcs = mappedClassesByCollection.get(collection);
        if (mcs == null || mcs.isEmpty()) {
            throw new MappingException(format("The collection '%s' is not mapped to a java class.", collection));
        }
        if (mcs.size() > 1) {
            if (LOG.isWarnEnabled()) {
                List<String> classes = new ArrayList<String>();
                for (final MappedClass mc : mcs) {
                    classes.add(mc.getClazz().getName());
                }
                LOG.warn(format("Found more than one class mapped to collection '%s': %s", collection, classes));
            }
        }
        return mcs.iterator().next().getClazz();
    }

    /**
     * Gets the mapped collection for an object instance or Class reference.
     *
     * @param object the object to process
     * @return the collection name
     * @morphia.internal
     * @deprecated no replacement is planned
     */
    @Deprecated
    public String getCollectionName(final Object object) {
        if (object == null) {
            throw new IllegalArgumentException();
        }

        final MappedClass mc = getMappedClass(object);
        return mc.getCollectionName();
    }

    /**
     * @return the Converters bundle this Mapper uses
     */
    public dev.morphia.converters.Converters getConverters() {
        return converters;
    }

    /**
     * Gets the ID value for an entity
     *
     * @param entity the entity to process
     * @return the ID value
     */
    public Object getId(final Object entity) {
        Object unwrapped = entity;
        if (unwrapped == null) {
            return null;
        }
        unwrapped = ProxyHelper.unwrap(unwrapped);
        try {
            final MappedClass mappedClass = getMappedClass(unwrapped.getClass());
            if (mappedClass != null) {
                final Field idField = mappedClass.getIdField();
                if (idField != null) {
                    return idField.get(unwrapped);
                }
            }
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
        return null;
    }

    /**
     * @return the cache of instances
     * @morphia.internal
     * @deprecated no replacement is planned
     */
    public Map<Class, Object> getInstanceCache() {
        return instanceCache;
    }

    /**
     * Gets list of {@link EntityInterceptor}s
     *
     * @return the Interceptors
     */
    public Collection<EntityInterceptor> getInterceptors() {
        return interceptors;
    }

    /**
     * Gets the Key for an entity
     *
     * @param entity the entity to process
     * @param <T>    the type of the entity
     * @return the Key
     */
    public <T> Key<T> getKey(final T entity) {
        T unwrapped = entity;
        if (unwrapped instanceof ProxiedEntityReference) {
            final ProxiedEntityReference proxy = (ProxiedEntityReference) unwrapped;
            return (Key<T>) proxy.__getKey();
        }

        unwrapped = ProxyHelper.unwrap(unwrapped);
        if (unwrapped instanceof Key) {
            return (Key<T>) unwrapped;
        }

        final Object id = getId(unwrapped);
        final Class<T> aClass = (Class<T>) unwrapped.getClass();
        return id == null ? null : new Key<T>(aClass, getCollectionName(aClass), id);
    }

    /**
     * Gets the Key for an entity and a specific collection
     *
     * @param entity     the entity to process
     * @param collection the collection to use in the Key rather than the mapped collection as defined on the entity's class
     * @param <T>        the type of the entity
     * @return the Key
     */
    public <T> Key<T> getKey(final T entity, final String collection) {
        T unwrapped = entity;
        if (unwrapped instanceof ProxiedEntityReference) {
            final ProxiedEntityReference proxy = (ProxiedEntityReference) unwrapped;
            return (Key<T>) proxy.__getKey();
        }

        unwrapped = ProxyHelper.unwrap(unwrapped);
        if (unwrapped instanceof Key) {
            return (Key<T>) unwrapped;
        }

        final Object id = getId(unwrapped);
        final Class<T> aClass = (Class<T>) unwrapped.getClass();
        return id == null ? null : new Key<T>(aClass, collection, id);
    }

    /**
     * Gets the Keys for a list of objects
     *
     * @param clazz the Class of the objects
     * @param refs  the objects to fetch the keys for
     * @param <T>   the type of the entity
     * @return the list of Keys
     */
    public <T> List<Key<T>> getKeysByManualRefs(final Class<T> clazz, final List<Object> refs) {
        final String collection = getCollectionName(clazz);
        final List<Key<T>> keys = new ArrayList<Key<T>>(refs.size());
        for (final Object ref : refs) {
            keys.add(this.<T>manualRefToKey(collection, ref));
        }

        return keys;
    }

    /**
     * Gets the Keys for a list of objects
     *
     * @param refs the objects to process
     * @param <T>  the type of the objects
     * @return the list of Keys
     */
    public <T> List<Key<T>> getKeysByRefs(final List<DBRef> refs) {
        final List<Key<T>> keys = new ArrayList<Key<T>>(refs.size());
        for (final DBRef ref : refs) {
            final Key<T> testKey = refToKey(ref);
            keys.add(testKey);
        }
        return keys;
    }

    /**
     * @return map of MappedClasses by class name
     */
    public Map<String, MappedClass> getMCMap() {
        return Collections.unmodifiableMap(mappedClasses);
    }

    /**
     * Gets the {@link MappedClass} for the object (type). If it isn't mapped, create a new class and cache it (without validating).
     *
     * @param obj the object to process
     * @return the MappedClass for the object given
     */
    public MappedClass getMappedClass(final Object obj) {
        if (obj == null) {
            return null;
        }

        Class type = (obj instanceof Class) ? (Class) obj : obj.getClass();
        if (ProxyHelper.isProxy(obj)) {
            type = ProxyHelper.getReferentClass(obj);
        }

        MappedClass mc = mappedClasses.get(type.getName());
        if (mc == null) {
            mc = new MappedClass(type, this);
            // no validation
            addMappedClass(mc, false);
        }
        return mc;
    }

    /**
     * @return collection of MappedClasses
     */
    public Collection<MappedClass> getMappedClasses() {
        return new ArrayList<MappedClass>(mappedClasses.values());
    }

    /**
     * @return the options used by this Mapper
     */
    public MapperOptions getOptions() {
        return opts;
    }

    /**
     * Sets the options this Mapper should use
     *
     * @param options the options to use
     */
    public void setOptions(final MapperOptions options) {
        opts = options;
    }

    /**
     * Checks to see if a Class has been mapped.
     *
     * @param c the Class to check
     * @return true if the Class has been mapped
     */
    public boolean isMapped(final Class c) {
        return mappedClasses.containsKey(c.getName());
    }

    /**
     * Converts a Key to a DBRef
     *
     * @param key the Key to convert
     * @return the DBRef
     */
    public DBRef keyToDBRef(final Key key) {
        if (key == null) {
            return null;
        }
        if (key.getType() == null && key.getCollection() == null) {
            throw new IllegalStateException("How can it be missing both?");
        }
        if (key.getCollection() == null) {
            key.setCollection(getCollectionName(key.getType()));
        }

        Object id = key.getId();
        if (isMapped(id.getClass())) {
            id = toMongoObject(id, true);
        }
        return new DBRef(key.getCollection(), id);
    }

    /**
     * Creates a Key for a type and an ID value
     *
     * @param type the Class of the entity
     * @param id   the ID value
     * @param <T>  the type of the entity
     * @return the Key
     */
    public <T> Key<T> manualRefToKey(final Class<T> type, final Object id) {
        return id == null ? null : new Key<T>(type, getCollectionName(type), id);
    }

    /**
     * Converts a DBRef to a Key
     *
     * @param ref the DBRef to convert
     * @param <T> the type of the referenced entity
     * @return the Key
     */
    public <T> Key<T> refToKey(final DBRef ref) {
        return ref == null ? null : new Key<T>((Class<? extends T>) getClassFromCollection(ref.getCollectionName()),
            ref.getCollectionName(), ref.getId());
    }

    /**
     * /**
     * Converts an entity (POJO) to a DBObject.  A special field will be added to keep track of the class type.
     *
     * @param entity The POJO
     * @return the DBObject
     */
    public DBObject toDBObject(final Object entity) {
        return toDBObject(entity, null);
    }

    /**
     * Converts an entity (POJO) to a DBObject.  A special field will be added to keep track of the class type.
     *
     * @param entity          The POJO
     * @param involvedObjects A Map of (already converted) POJOs
     * @return the DBObject
     */
    public DBObject toDBObject(final Object entity, final Map<Object, DBObject> involvedObjects) {
        return toDBObject(entity, involvedObjects, true);
    }

    /**
     * Converts a java object to a mongo-compatible object (possibly a DBObject for complex mappings).  Very similar to {@link
     * Mapper#toDBObject}.  Used (mainly) by query/update operations.
     *
     * @param mf    the MappedField for this value
     * @param mc    the MappedClass for this value
     * @param value the value to convert
     * @return the MongoDB compatible object
     */
    @SuppressWarnings("deprecation")
    public Object toMongoObject(final MappedField mf, final MappedClass mc, final Object value) {
        if (value == null) {
            return null;
        }

        Object mappedValue = value;

        if (value instanceof Query) {
            mappedValue = ((QueryImpl) value).getQueryObject();
        } else if (isAssignable(mf, value) || isEntity(mc)) {
            //convert the value to Key (DBRef) if the field is @Reference or type is Key/DBRef, or if the destination class is an @Entity
            try {
                if (value instanceof Iterable) {
                    MappedClass mapped = getMappedClass(mf.getSubClass());
                    if (mapped != null && (Key.class.isAssignableFrom(mapped.getClazz()) || mapped.getEntityAnnotation() != null)) {
                        mappedValue = getDBRefs(mf, (Iterable) value);
                    } else {
                        if (mf.hasAnnotation(Reference.class)) {
                            mappedValue = getDBRefs(mf, (Iterable) value);
                        } else {
                            mappedValue = toMongoObject(value, false);
                        }
                    }
                } else {
                    if (mf != null) {
                        Class<?> idType = null;
                        if (!mf.getType().equals(Key.class) && isMapped(mf.getType())) {
                            final MappedField idField = getMappedClass(mf.getType())
                                                            .getMappedIdField();
                            idType = idField != null ? idField.getType() : null;
                        }
                        boolean valueIsIdType = mappedValue.getClass().equals(idType);
                        Reference refAnn = mf.getAnnotation(Reference.class);
                        if (refAnn != null) {
                            if (!valueIsIdType) {
                                Key<?> key = value instanceof Key ? (Key<?>) value : getKey(value);
                                if (key != null) {
                                    mappedValue = refAnn.idOnly()
                                                  ? keyToId(key)
                                                  : keyToDBRef(key);
                                }
                            }
                        } else if (mf.getType().isAssignableFrom(MorphiaReference.class)) {
                            if (!valueIsIdType) {
                                Key<?> key = value instanceof Key ? (Key<?>) value : getKey(value);
                                if (key != null) {
                                    mappedValue = keyToId(key);
                                }
                            }

                        } else if (mf.getType().equals(Key.class)) {
                            mappedValue = keyToDBRef(valueIsIdType
                                                     ? createKey(mf.getSubClass(), value)
                                                     : value instanceof Key ? (Key<?>) value : getKey(value));
                            if (mappedValue == value) {
                                throw new ValidationException("cannot map to Key<T> field: " + value);
                            }
                        }
                    }

                    if (mappedValue == value) {
                        mappedValue = toMongoObject(value, false);
                    }
                }
            } catch (Exception e) {
                LOG.error("Error converting value(" + value + ") to reference.", e);
                mappedValue = toMongoObject(value, false);
            }
        } else if (mf != null && mf.hasAnnotation(Serialized.class)) { //serialized
            try {
                mappedValue = Serializer.serialize(value, !mf.getAnnotation(Serialized.class).disableCompression());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else if (value instanceof DBObject) {  //pass-through
            mappedValue = value;
        } else {
            mappedValue = toMongoObject(value, EmbeddedMapper.shouldSaveClassName(value, mappedValue, mf));
            if (mappedValue instanceof BasicDBList) {
                final BasicDBList list = (BasicDBList) mappedValue;
                if (list.size() != 0) {
                    if (!EmbeddedMapper.shouldSaveClassName(extractFirstElement(value), list.get(0), mf)) {
                        for (Object o : list) {
                            if (o instanceof DBObject) {
                                ((DBObject) o).removeField(opts.getDiscriminatorField());
                            }
                        }
                    }
                }
            } else if (mappedValue instanceof DBObject && !EmbeddedMapper.shouldSaveClassName(value, mappedValue, mf)) {
                ((DBObject) mappedValue).removeField(opts.getDiscriminatorField());
            }
        }

        return mappedValue;
    }

    /**
     * Updates the collection value on a Key with the mapped value on the Key's type Class
     *
     * @param key the Key to update
     * @return the collection name on the Key
     */
    public String updateCollection(final Key key) {
        if (key.getCollection() == null && key.getType() == null) {
            throw new IllegalStateException("Key is invalid! " + toString());
        } else if (key.getCollection() == null) {
            key.setCollection(getMappedClass(key.getType()).getCollectionName());
        }

        return key.getCollection();
    }

    /**
     * Updates the @{@link dev.morphia.annotations.Id} and @{@link dev.morphia.annotations.Version} fields.
     *
     * @param datastore the Datastore to use when fetching this reference
     * @param dbObj     Value to update with; null means skip
     * @param cache     the EntityCache
     * @param entity    The object to update
     */
    public void updateKeyAndVersionInfo(final Datastore datastore, final DBObject dbObj, final EntityCache cache, final Object entity) {
        final MappedClass mc = getMappedClass(entity);

        // update id field, if there.
        if ((mc.getIdField() != null) && (dbObj != null) && (dbObj.get("_id") != null)) {
            try {
                final MappedField mf = mc.getMappedIdField();
                final Object oldIdValue = mc.getIdField().get(entity);
                readMappedField(datastore, mf, entity, cache, dbObj);
                if (oldIdValue != null) {
                    // The entity already had an id set. Check to make sure it hasn't changed. That would be unexpected, and could
                    // indicate a bad state.
                    final Object dbIdValue = mf.getFieldValue(entity);
                    if (!dbIdValue.equals(oldIdValue)) {
                        mf.setFieldValue(entity, oldIdValue); //put the value back...
                        throw new RuntimeException(format("@Id mismatch: %s != %s for %s", oldIdValue, dbIdValue,
                            entity.getClass().getName()));
                    }
                }
            } catch (Exception e) {
                if (e instanceof RuntimeException) {
                    throw (RuntimeException) e;
                }

                throw new RuntimeException("Error setting @Id field after save/insert.", e);
            }
        }
        if (mc.getMappedVersionField() != null && (dbObj != null)) {
            readMappedField(datastore, mc.getMappedVersionField(), entity, cache, dbObj);
        }
    }

    protected LazyProxyFactory getProxyFactory() {
        return proxyFactory;
    }

    private void addConverters(final MappedClass mc) {
        final List<Annotation> convertersList = mc.getAnnotations(Converters.class);
        if (convertersList != null) {
            for (Annotation a : convertersList) {
                final Converters c = (Converters) a;
                if (c != null) {
                    for (final Class<? extends TypeConverter> clazz : c.value()) {
                        if (!getConverters().isRegistered(clazz)) {
                            getConverters().addConverter(clazz);
                        }
                    }
                }
            }

        }
    }

    /**
     * Add MappedClass to internal cache, possibly validating first.
     */
    private MappedClass addMappedClass(final MappedClass mc, final boolean validate) {
        addConverters(mc);

        if (validate && !mc.isInterface()) {
            mc.validate(this);
        }

        mappedClasses.put(mc.getClazz().getName(), mc);

        Set<MappedClass> mcs = mappedClassesByCollection.get(mc.getCollectionName());
        if (mcs == null) {
            mcs = new CopyOnWriteArraySet<MappedClass>();
            final Set<MappedClass> temp = mappedClassesByCollection.putIfAbsent(mc.getCollectionName(), mcs);
            if (temp != null) {
                mcs = temp;
            }
        }

        mcs.add(mc);

        return mc;
    }

    private Object extractFirstElement(final Object value) {
        return value.getClass().isArray() ? Array.get(value, 0) : ((Iterable) value).iterator().next();
    }

    private Object getDBRefs(final MappedField field, final Iterable value) {
        final List<Object> refs = new ArrayList<Object>();
        Reference annotation = field.getAnnotation(Reference.class);
        boolean idOnly = annotation != null && annotation.idOnly();
        for (final Object o : value) {
            Key<?> key = (o instanceof Key) ? (Key<?>) o : getKey(o);
            refs.add(idOnly ? key.getId() : keyToDBRef(key));
        }
        return refs;
    }

    private Class<? extends Annotation> getFieldAnnotation(final MappedField mf) {
        Class<? extends Annotation> annType = null;
        for (final Class<? extends Annotation> testType : new Class[]{Property.class, Embedded.class, Serialized.class, Reference.class}) {
            if (mf.hasAnnotation(testType)) {
                annType = testType;
                break;
            }
        }
        return annType;
    }

    private boolean isAssignable(final MappedField mf, final Object value) {
        return mf != null
               && (mf.hasAnnotation(Reference.class) || Key.class.isAssignableFrom(mf.getType())
                   || DBRef.class.isAssignableFrom(mf.getType()) || isMultiValued(mf, value));

    }

    private boolean isEntity(final MappedClass mc) {
        return (mc != null && mc.getEntityAnnotation() != null);
    }

    private boolean isMultiValued(final MappedField mf, final Object value) {
        final Class subClass = mf.getSubClass();
        return value instanceof Iterable
               && mf.isMultipleValues()
               && (Key.class.isAssignableFrom(subClass) || DBRef.class.isAssignableFrom(subClass));
    }

    private void readMappedField(final Datastore datastore, final MappedField mf, final Object entity, final EntityCache cache,
                                 final DBObject dbObject) {
        CustomMapper mapper;
        if (mf.hasAnnotation(Property.class) || mf.hasAnnotation(Serialized.class)
            || mf.isTypeMongoCompatible() || getConverters().hasSimpleValueConverter(mf)) {
            mapper = opts.getValueMapper();
        } else if (mf.hasAnnotation(Embedded.class)) {
            mapper = opts.getEmbeddedMapper();
        } else if (mf.hasAnnotation(Reference.class) || MorphiaReference.class == mf.getConcreteType()) {
            mapper = opts.getReferenceMapper();
        } else {
            mapper = opts.getDefaultMapper();
        }
        mapper.fromDBObject(datastore, dbObject, mf, entity, cache, this);
    }

    private void writeMappedField(final DBObject dbObject, final MappedField mf, final Object entity,
                                  final Map<Object, DBObject> involvedObjects) {

        //skip not saved fields.
        if (mf.hasAnnotation(NotSaved.class)) {
            return;
        }

        // get the annotation from the field.
        Class<? extends Annotation> annType = getFieldAnnotation(mf);

        if (Property.class.equals(annType) || Serialized.class.equals(annType) || mf.isTypeMongoCompatible()
            || (getConverters().hasSimpleValueConverter(mf) || (getConverters().hasSimpleValueConverter(mf.getFieldValue(entity))))) {
            opts.getValueMapper().toDBObject(entity, mf, dbObject, involvedObjects, this);
        } else if (Reference.class.equals(annType) || MorphiaReference.class == mf.getConcreteType()) {
            opts.getReferenceMapper().toDBObject(entity, mf, dbObject, involvedObjects, this);
        } else if (Embedded.class.equals(annType)) {
            opts.getEmbeddedMapper().toDBObject(entity, mf, dbObject, involvedObjects, this);
        } else {
            if (LOG.isDebugEnabled()) {
                LOG.debug("No annotation was found, using default mapper " + opts.getDefaultMapper() + " for " + mf);
            }
            opts.getDefaultMapper().toDBObject(entity, mf, dbObject, involvedObjects, this);
        }

    }

    <T> Key<T> manualRefToKey(final String collection, final Object id) {
        return id == null ? null : new Key<T>((Class<? extends T>) getClassFromCollection(collection), collection, id);
    }

    Object keyToId(final Key key) {
        return key == null ? null : key.getId();
    }

    /**
     * <p> Converts a java object to a mongo-compatible object (possibly a DBObject for complex mappings). Very similar to {@link
     * Mapper#toDBObject} </p> <p> Used (mainly) by query/update operations </p>
     */
    Object toMongoObject(final Object javaObj, final boolean includeClassName) {
        if (javaObj == null) {
            return null;
        }
        Class origClass = javaObj.getClass();

        if (origClass.isAnonymousClass() && origClass.getSuperclass().isEnum()) {
            origClass = origClass.getSuperclass();
        }

        final Object newObj = getConverters().encode(origClass, javaObj);
        if (newObj == null) {
            LOG.warn("converted " + javaObj + " to null");
            return null;
        }
        final Class type = newObj.getClass();
        final boolean bSameType = origClass.equals(type);

        //TODO: think about this logic a bit more.
        //Even if the converter changed it, should it still be processed?
        if (!bSameType && !(Map.class.isAssignableFrom(type) || Iterable.class.isAssignableFrom(type))) {
            return newObj;
        } else { //The converter ran, and produced another type, or it is a list/map

            boolean isSingleValue = true;
            boolean isMap = false;
            Class subType = null;

            if (type.isArray() || Map.class.isAssignableFrom(type) || Iterable.class.isAssignableFrom(type)) {
                isSingleValue = false;
                isMap = implementsInterface(type, Map.class);
                // subtype of Long[], List<Long> is Long
                subType = (type.isArray()) ? type.getComponentType() : getParameterizedClass(type, (isMap) ? 1 : 0);
            }

            if (isSingleValue && !isPropertyType(type)) {
                final DBObject dbObj = toDBObject(newObj);
                if (!includeClassName) {
                    dbObj.removeField(opts.getDiscriminatorField());
                }
                return dbObj;
            } else if (newObj instanceof DBObject) {
                return newObj;
            } else if (isMap) {
                if (isPropertyType(subType)) {
                    return toDBObject(newObj);
                } else {
                    final LinkedHashMap m = new LinkedHashMap();
                    for (final Map.Entry e : (Iterable<Map.Entry>) ((Map) newObj).entrySet()) {
                        m.put(e.getKey(), toMongoObject(e.getValue(), includeClassName));
                    }

                    return m;
                }
                //Set/List but needs elements converted
            } else if (!isSingleValue && !isPropertyType(subType)) {
                final List<Object> values = new BasicDBList();
                if (type.isArray()) {
                    for (final Object obj : (Object[]) newObj) {
                        values.add(toMongoObject(obj, includeClassName));
                    }
                } else {
                    for (final Object obj : (Iterable) newObj) {
                        values.add(toMongoObject(obj, includeClassName));
                    }
                }

                return values;
            } else {
                return newObj;
            }
        }
    }

    DBObject toDBObject(final Object entity, final Map<Object, DBObject> involvedObjects, final boolean lifecycle) {

        DBObject dbObject = new BasicDBObject();
        final MappedClass mc = getMappedClass(entity);

        if (mc.getEntityAnnotation() == null || !mc.getEntityAnnotation().noClassnameStored()) {
            dbObject.put(opts.getDiscriminatorField(), entity.getClass().getName());
        }

        if (lifecycle) {
            dbObject = mc.callLifecycleMethods(PrePersist.class, entity, dbObject, this);
        }

        for (final MappedField mf : mc.getPersistenceFields()) {
            try {
                writeMappedField(dbObject, mf, entity, involvedObjects);
            } catch (Exception e) {
                throw new MappingException("Error mapping field:" + mf.getFullName(), e);
            }
        }
        if (involvedObjects != null) {
            involvedObjects.put(entity, dbObject);
        }

        if (lifecycle) {
            mc.callLifecycleMethods(PreSave.class, entity, dbObject, this);
        }

        return dbObject;
    }

    <T> Key<T> createKey(final Class<T> clazz, final Serializable id) {
        return new Key<T>(clazz, getCollectionName(clazz), id);
    }

    <T> Key<T> createKey(final Class<T> clazz, final Object id) {
        if (id instanceof Serializable) {
            return createKey(clazz, (Serializable) id);
        }

        //TODO: cache the encoders, maybe use the pool version of the buffer that the driver does.
        final BSONEncoder enc = new BasicBSONEncoder();
        return new Key<T>(clazz, getCollectionName(clazz), enc.encode(toDBObject(id)));
    }
}
