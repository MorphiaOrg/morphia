/**
 * Copyright (C) 2010 Olafur Gauti Gudmundsson Licensed under the Apache
 * License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law
 * or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */


package org.mongodb.morphia.mapping;


import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.DBRef;
import org.bson.BSONEncoder;
import org.bson.BasicBSONEncoder;
import org.mongodb.morphia.EntityInterceptor;
import org.mongodb.morphia.Key;
import org.mongodb.morphia.annotations.Converters;
import org.mongodb.morphia.annotations.Embedded;
import org.mongodb.morphia.annotations.NotSaved;
import org.mongodb.morphia.annotations.PostLoad;
import org.mongodb.morphia.annotations.PreLoad;
import org.mongodb.morphia.annotations.PrePersist;
import org.mongodb.morphia.annotations.PreSave;
import org.mongodb.morphia.annotations.Property;
import org.mongodb.morphia.annotations.Reference;
import org.mongodb.morphia.annotations.Serialized;
import org.mongodb.morphia.converters.CustomConverters;
import org.mongodb.morphia.converters.TypeConverter;
import org.mongodb.morphia.logging.Logger;
import org.mongodb.morphia.logging.MorphiaLoggerFactory;
import org.mongodb.morphia.mapping.cache.DefaultEntityCache;
import org.mongodb.morphia.mapping.cache.EntityCache;
import org.mongodb.morphia.mapping.lazy.DatastoreProvider;
import org.mongodb.morphia.mapping.lazy.DefaultDatastoreProvider;
import org.mongodb.morphia.mapping.lazy.LazyFeatureDependencies;
import org.mongodb.morphia.mapping.lazy.LazyProxyFactory;
import org.mongodb.morphia.mapping.lazy.proxy.ProxiedEntityReference;
import org.mongodb.morphia.mapping.lazy.proxy.ProxyHelper;
import org.mongodb.morphia.query.ValidationException;

import java.io.IOException;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

import static java.lang.String.format;
import static org.mongodb.morphia.utils.ReflectionUtils.getParameterizedClass;
import static org.mongodb.morphia.utils.ReflectionUtils.implementsInterface;
import static org.mongodb.morphia.utils.ReflectionUtils.isPropertyType;


/**
 * <p>This is the heart of Morphia and takes care of mapping from/to POJOs/DBObjects<p> <p>This class is thread-safe and keeps various
 * "cached" data which should speed up processing.</p>
 *
 * @author Olafur Gauti Gudmundsson
 * @author Scott Hernandez
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class Mapper {
    private static final Logger LOG = MorphiaLoggerFactory.get(Mapper.class);

    /**
     * The @{@link org.mongodb.morphia.annotations.Id} field name that is stored with mongodb.
     */
    public static final String ID_KEY = "_id";
    /**
     * Special name that can never be used. Used as default for some fields to indicate default state.
     */
    public static final String IGNORED_FIELDNAME = ".";
    /**
     * Special field used by morphia to support various possibly loading issues; will be replaced when discriminators are implemented to
     * support polymorphism
     */
    public static final String CLASS_NAME_FIELDNAME = "className";

    /**
     * Set of classes that registered by this mapper
     */
    private final Map<String, MappedClass> mappedClasses = new ConcurrentHashMap<String, MappedClass>();
    private final ConcurrentHashMap<String, Set<MappedClass>> mappedClassesByCollection = new ConcurrentHashMap<String, Set<MappedClass>>();

    //EntityInterceptors; these are called before EntityListeners and lifecycle methods on an Entity, for all Entities
    private final List<EntityInterceptor> interceptors = new LinkedList<EntityInterceptor>();

    //A general cache of instances of classes; used by MappedClass for EntityListener(s)
    private final Map<Class, Object> instanceCache = new ConcurrentHashMap();

    private MapperOptions opts = new MapperOptions();

    // TODO: make these configurable
    private final LazyProxyFactory proxyFactory = LazyFeatureDependencies.createDefaultProxyFactory();
    private DatastoreProvider datastoreProvider = new DefaultDatastoreProvider();
    private final org.mongodb.morphia.converters.Converters converters;

    public Mapper() {
        converters = new CustomConverters(this);
    }

    public Mapper(final MapperOptions opts) {
        this();
        this.opts = opts;
    }
    
    public Mapper(final DatastoreProvider datastoreProvider) {
        this();
        this.datastoreProvider = datastoreProvider;
    }

    /**
     * <p> Adds an {@link EntityInterceptor} </p>
     */
    public void addInterceptor(final EntityInterceptor ei) {
        interceptors.add(ei);
    }

    /**
     * <p> Gets list of {@link EntityInterceptor}s </p>
     */
    public Collection<EntityInterceptor> getInterceptors() {
        return interceptors;
    }

    public MapperOptions getOptions() {
        return opts;
    }

    public void setOptions(final MapperOptions options) {
        opts = options;
    }

    public boolean isMapped(final Class c) {
        return mappedClasses.containsKey(c.getName());
    }

    /**
     * Creates a MappedClass and validates it.
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
     * Add MappedClass to internal cache, possibly validating first.
     */
    private MappedClass addMappedClass(final MappedClass mc, final boolean validate) {
        addConverters(mc);

        if (validate) {
            mc.validate();
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
     * Returns collection of MappedClasses
     */
    public Collection<MappedClass> getMappedClasses() {
        return new ArrayList<MappedClass>(mappedClasses.values());
    }

    /**
     * Returns map of MappedClasses by class name
     */
    public Map<String, MappedClass> getMCMap() {
        return Collections.unmodifiableMap(mappedClasses);
    }

    /**
     * <p> Gets the {@link MappedClass} for the object (type). If it isn't mapped, create a new class and cache it (without validating).
     * </p>
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
//        if (mc == null && !isPropertyType(type) && !isPrimitiveLike(type)) {
        if (mc == null) {
            mc = new MappedClass(type, this);
            // no validation
            addMappedClass(mc, false);
        }
        return mc;
    }

    public String getCollectionName(final Object object) {
        if (object == null) {
            throw new IllegalArgumentException();
        }

        final MappedClass mc = getMappedClass(object);
        return mc.getCollectionName();
    }

    /**
     * <p> Updates the @{@link org.mongodb.morphia.annotations.Id} fields. </p>
     *
     * @param entity The object to update
     * @param dbObj  Value to update with; null means skip
     */
    public void updateKeyInfo(final Object entity, final DBObject dbObj, final EntityCache cache) {
        final MappedClass mc = getMappedClass(entity);

        // update id field, if there.
        if ((mc.getIdField() != null) && (dbObj != null) && (dbObj.get(ID_KEY) != null)) {
            try {
                final MappedField mf = mc.getMappedIdField();
                final Object oldIdValue = mc.getIdField().get(entity);
                readMappedField(dbObj, mf, entity, cache);
                final Object dbIdValue = mc.getIdField().get(entity);
                if (oldIdValue != null) {
                    // The entity already had an id set. Check to make sure it
                    // hasn't changed. That would be unexpected, and could
                    // indicate a bad state.
                    if (!dbIdValue.equals(oldIdValue)) {
                        mf.setFieldValue(entity, oldIdValue); //put the value back...
                        throw new RuntimeException(format("@Id mismatch: %s != %s for %s", oldIdValue, dbIdValue,
                                                          entity.getClass().getName()));
                    }
                } else {
                    mc.getIdField().set(entity, dbIdValue);
                }
            } catch (Exception e) {
                if (e instanceof RuntimeException) {
                    throw (RuntimeException) e;
                }

                throw new RuntimeException("Error setting @Id field after save/insert.", e);
            }
        }
    }

    /**
     * Converts a DBObject back to a type-safe java object (POJO)
     *
     * @param entityClass The type to return, or use; can be overridden by the @see Mapper.CLASS_NAME_FIELDNAME in the DBObject
     */
    public <T> T fromDBObject(final Class<T> entityClass, final DBObject dbObject, final EntityCache cache) {
        if (dbObject == null) {
            final Throwable t = new Throwable();
            LOG.error("Somebody passed in a null dbObject; bad client!", t);
            return null;
        }

        T entity;
        entity = opts.getObjectFactory().createInstance(entityClass, dbObject);
        entity = fromDb(dbObject, entity, cache);
        return entity;
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
            LOG.warning("converted " + javaObj + " to null");
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
                    dbObj.removeField(CLASS_NAME_FIELDNAME);
                }
                return dbObj;
            } else if (newObj instanceof DBObject) {
                return newObj;
            } else if (isMap) {
                if (isPropertyType(subType)) {
                    return toDBObject(newObj);
                } else {
                    final HashMap m = new HashMap();
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


    /**
     * <p> Converts a java object to a mongo-compatible object (possibly a DBObject for complex mappings). Very similar to {@link
     * Mapper#toDBObject} </p> <p> Used (mainly) by query/update operations </p>
     */
    public Object toMongoObject(final MappedField mf, final MappedClass mc, final Object value) {
        Object mappedValue = value;

        //convert the value to Key (DBRef) if the field is @Reference or type is Key/DBRef, or if the destination class is an @Entity
        if (isAssignable(mf, value) || isEntity(mc)) {
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
                    final Key<?> key = (value instanceof Key) ? (Key<?>) value : getKey(value);
                    if (key == null) {
                        mappedValue = toMongoObject(value, false);
                    } else {
                        mappedValue = keyToRef(key);
                        if (mappedValue == value) {
                            throw new ValidationException("cannot map to @Reference/Key<T>/DBRef field: " + value);
                        }
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
                                ((DBObject) o).removeField(CLASS_NAME_FIELDNAME);
                            }
                        }
                    }
                }
            } else if (mappedValue instanceof DBObject && !EmbeddedMapper.shouldSaveClassName(value, mappedValue, mf)) {
                ((DBObject) mappedValue).removeField(CLASS_NAME_FIELDNAME);
            }
        }

        return mappedValue;
    }

    private Object extractFirstElement(final Object value) {
        return value.getClass().isArray() ? Array.get(value, 0) : ((Iterable) value).iterator().next();
    }

    private Object getDBRefs(final MappedField field, final Iterable value) {
        final List<Object> refs = new ArrayList<Object>();
        boolean idOnly = field.getAnnotation(Reference.class).idOnly();
        for (final Object o : value) {
            Key<?> key = (o instanceof Key) ? (Key<?>) o : getKey(o);
            refs.add(idOnly ? key.getId() : keyToRef(key));
        }
        return refs;
    }

    private boolean isAssignable(final MappedField mf, final Object value) {
        return mf != null
               && (mf.hasAnnotation(Reference.class) || Key.class.isAssignableFrom(mf.getType())
                   || DBRef.class.isAssignableFrom(mf.getType()) || isMultiValued(mf, value));

    }

    private boolean isMultiValued(final MappedField mf, final Object value) {
        final Class subClass = mf.getSubClass();
        return value instanceof Iterable
               && mf.isMultipleValues()
               && (Key.class.isAssignableFrom(subClass) || DBRef.class.isAssignableFrom(subClass));
    }

    private boolean isEntity(final MappedClass mc) {
        return (mc != null && mc.getEntityAnnotation() != null);
    }

    public Object getId(final Object entity) {
        Object unwrapped = entity;
        if (unwrapped == null) {
            return null;
        }
        unwrapped = ProxyHelper.unwrap(unwrapped);
        try {
            return getMappedClass(unwrapped.getClass()).getIdField().get(unwrapped);
        } catch (Exception e) {
            return null;
        }
    }

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
     * Converts an entity (POJO) to a DBObject; A special field will be added to keep track of the class: {@link
     * Mapper#CLASS_NAME_FIELDNAME}
     *
     * @param entity The POJO
     */
    public DBObject toDBObject(final Object entity) {
        return toDBObject(entity, null);
    }

    /**
     * <p> Converts an entity (POJO) to a DBObject (for use with low-level driver); A special field will be added to keep track of the
     * class: {@link Mapper#CLASS_NAME_FIELDNAME} </p>
     *
     * @param entity          The POJO
     * @param involvedObjects A Map of (already converted) POJOs
     */
    public DBObject toDBObject(final Object entity, final Map<Object, DBObject> involvedObjects) {
        return toDBObject(entity, involvedObjects, true);
    }

    DBObject toDBObject(final Object entity, final Map<Object, DBObject> involvedObjects, final boolean lifecycle) {

        DBObject dbObject = new BasicDBObject();
        final MappedClass mc = getMappedClass(entity);

        if (mc.getEntityAnnotation() == null || !mc.getEntityAnnotation().noClassnameStored()) {
            dbObject.put(CLASS_NAME_FIELDNAME, entity.getClass().getName());
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

    public <T> T fromDb(final DBObject dbObject, final T entity, final EntityCache cache) {
        //hack to bypass things and just read the value.
        if (entity instanceof MappedField) {
            readMappedField(dbObject, (MappedField) entity, entity, cache);
            return entity;
        }

        // check the history key (a key is the namespace + id)

        if (dbObject.containsField(ID_KEY) && getMappedClass(entity).getIdField() != null
            && getMappedClass(entity).getEntityAnnotation() != null) {
            final Key<T> key = new Key(entity.getClass(), getCollectionName(entity.getClass()), dbObject.get(ID_KEY));
            final T cachedInstance = cache.getEntity(key);
            if (cachedInstance != null) {
                return cachedInstance;
            } else {
                cache.putEntity(key, entity); // to avoid stackOverflow in recursive refs
            }
        }

        final MappedClass mc = getMappedClass(entity);

        final DBObject updated = mc.callLifecycleMethods(PreLoad.class, entity, dbObject, this);
        try {
            for (final MappedField mf : mc.getPersistenceFields()) {
                readMappedField(updated, mf, entity, cache);
            }
        } catch (final MappingException e) {
            Object id = dbObject.get(ID_KEY);
            String entityName = entity.getClass().getName();
            throw new MappingException(format("Could not map %s with ID: %s", entityName, id), e);
        }

        if (updated.containsField(ID_KEY) && getMappedClass(entity).getIdField() != null) {
            final Key key = new Key(entity.getClass(), getCollectionName(entity.getClass()), updated.get(ID_KEY));
            cache.putEntity(key, entity);
        }
        mc.callLifecycleMethods(PostLoad.class, entity, updated, this);
        return entity;
    }

    private void readMappedField(final DBObject dbObject, final MappedField mf, final Object entity, final EntityCache cache) {
        if (mf.hasAnnotation(Property.class) || mf.hasAnnotation(Serialized.class)
            || mf.isTypeMongoCompatible() || getConverters().hasSimpleValueConverter(mf)) {
            opts.getValueMapper().fromDBObject(dbObject, mf, entity, cache, this);
        } else if (mf.hasAnnotation(Embedded.class)) {
            opts.getEmbeddedMapper().fromDBObject(dbObject, mf, entity, cache, this);
        } else if (mf.hasAnnotation(Reference.class)) {
            opts.getReferenceMapper().fromDBObject(dbObject, mf, entity, cache, this);
        } else {
            opts.getDefaultMapper().fromDBObject(dbObject, mf, entity, cache, this);
        }
    }

    private void writeMappedField(final DBObject dbObject, final MappedField mf, final Object entity,
                                  final Map<Object, DBObject> involvedObjects) {
        Class<? extends Annotation> annType = null;

        //skip not saved fields.
        if (mf.hasAnnotation(NotSaved.class)) {
            return;
        }

        // get the annotation from the field.
        for (final Class<? extends Annotation> testType : new Class[]{Property.class, Embedded.class, Serialized.class, Reference.class}) {
            if (mf.hasAnnotation(testType)) {
                annType = testType;
                break;
            }
        }

        if (Property.class.equals(annType) || Serialized.class.equals(annType) || mf.isTypeMongoCompatible()
            || (getConverters().hasSimpleValueConverter(mf) || (getConverters().hasSimpleValueConverter(mf.getFieldValue(entity))))) {
            opts.getValueMapper().toDBObject(entity, mf, dbObject, involvedObjects, this);
        } else if (Reference.class.equals(annType)) {
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

    public org.mongodb.morphia.converters.Converters getConverters() {
        return converters;
    }

    public EntityCache createEntityCache() {
        return new DefaultEntityCache();
    }

    public <T> Key<T> refToKey(final DBRef ref) {
        return ref == null ? null : new Key<T>((Class<? extends T>) getClassFromCollection(ref.getCollectionName()),
                                               ref.getCollectionName(), ref.getId());
    }

    public <T> Key<T> manualRefToKey(final Class<T> type, final Object id) {
        return id == null ? null : new Key<T>(type, getCollectionName(type), id);
    }

    public <T> Key<T> manualRefToKey(final String collection, final Object id) {
        return id == null ? null : new Key<T>((Class<? extends T>) getClassFromCollection(collection), collection, id);
    }

    /**
     * Queries the server to check for each manual ref
     */
    public <T> List<Key<T>> getKeysByManualRefs(final Class<T> kindClass, final List<Object> refs) {
        final String kind = getCollectionName(kindClass);
        final List<Key<T>> keys = new ArrayList<Key<T>>(refs.size());
        for (final Object ref : refs) {
            keys.add(this.<T>manualRefToKey(kind, ref));
        }

        return keys;
    }

    /**
     * Queries the server to check for each DBRef
     */
    public <T> List<Key<T>> getKeysByRefs(final List<DBRef> refs) {
        final List<Key<T>> keys = new ArrayList<Key<T>>(refs.size());
        for (final DBRef ref : refs) {
            final Key<T> testKey = refToKey(ref);
            keys.add(testKey);
        }
        return keys;
    }

    
    public DBRef keyToRef(final Key key) {
        if (key == null) {
            return null;
        }
        if (key.getType() == null && key.getCollection() == null) {
            throw new IllegalStateException("How can it be missing both?");
        }
        if (key.getCollection() == null) {
            key.setCollection(getCollectionName(key.getType()));
        }

        return new DBRef(key.getCollection(), key.getId());
    }

    public Object keyToManualRef(final Key key) {
        return key == null ? null : key.getId();
    }

    public String updateKind(final Key key) {
        if (key.getCollection() == null && key.getType() == null) {
            throw new IllegalStateException("Key is invalid! " + toString());
        } else if (key.getCollection() == null) {
            key.setCollection(getMappedClass(key.getType()).getCollectionName());
        }

        return key.getCollection();
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

    public Class<?> getClassFromCollection(final String collection) {
        final Set<MappedClass> mcs = mappedClassesByCollection.get(collection);
        if (mcs.isEmpty()) {
            throw new MappingException(format("The collection '%s' is not mapped to a java class.", collection));
        }
        if (mcs.size() > 1) {
            if (LOG.isInfoEnabled()) {
                LOG.info(format("Found more than one class mapped to collection '%s'%s", collection, mcs));
            }
        }
        return mcs.iterator().next().getClazz();
    }

    public Map<Class, Object> getInstanceCache() {
        return instanceCache;
    }

    public LazyProxyFactory getProxyFactory() {
        return proxyFactory;
    }

    public DatastoreProvider getDatastoreProvider() {
        return datastoreProvider;
    }

}
