package dev.morphia.mapping;


import com.mongodb.BasicDBList;
import com.mongodb.DBRef;
import dev.morphia.Datastore;
import dev.morphia.EntityInterceptor;
import dev.morphia.Key;
import dev.morphia.annotations.Converters;
import dev.morphia.annotations.Embedded;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.PostLoad;
import dev.morphia.annotations.PreLoad;
import dev.morphia.annotations.PrePersist;
import dev.morphia.annotations.PreSave;
import dev.morphia.converters.TypeConverter;
import dev.morphia.mapping.cache.EntityCache;
import dev.morphia.mapping.codec.EnumCodecProvider;
import dev.morphia.mapping.codec.MorphiaCodecProvider;
import dev.morphia.mapping.codec.MorphiaTypesCodecProvider;
import dev.morphia.mapping.codec.PrimitiveCodecProvider;
import dev.morphia.mapping.codec.pojo.MorphiaCodec;
import dev.morphia.mapping.lazy.LazyFeatureDependencies;
import dev.morphia.mapping.lazy.LazyProxyFactory;
import dev.morphia.mapping.lazy.proxy.ProxiedEntityReference;
import dev.morphia.mapping.lazy.proxy.ProxyHelper;
import dev.morphia.utils.ReflectionUtils;
import org.bson.Document;
import org.bson.codecs.Codec;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.Convention;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Modifier;
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
import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;


/**
 * @morphia.internal
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class Mapper {

    /**
     * Special name that can never be used. Used as default for some fields to indicate default state.
     *
     * @morphia.internal
     */
    public static final String IGNORED_FIELDNAME = ".";

    static final String CLASS_NAME_FIELDNAME = "className";

    private static final Logger LOG = LoggerFactory.getLogger(Mapper.class);
    /**
     * Set of classes that registered by this mapper
     */
    private final Map<Class, MappedClass> mappedClasses = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Set<MappedClass>> mappedClassesByCollection = new ConcurrentHashMap<>();

    //EntityInterceptors; these are called after EntityListeners and lifecycle methods on an Entity, for all Entities
    private final List<EntityInterceptor> interceptors = new LinkedList<>();

    //A general cache of instances of classes; used by MappedClass for EntityListener(s)
    private final Map<Class, Object> instanceCache = new ConcurrentHashMap();
    private CodecRegistry codecRegistry;
    private final LazyProxyFactory proxyFactory = LazyFeatureDependencies.createDefaultProxyFactory();
    private final MapperOptions opts;

    /**
     * Creates a Mapper with the given options.
     *
     * @morphia.internal
     * @param opts the options to use
     */
    public Mapper(final Datastore datastore, final CodecRegistry codecRegistry, final MapperOptions opts) {
        this.opts = opts;
        final MorphiaCodecProvider codecProvider = new MorphiaCodecProvider(datastore, this,
            List.of(new MorphiaConvention(datastore, opts)), Set.of(""));
        final MorphiaTypesCodecProvider typesCodecProvider = new MorphiaTypesCodecProvider(this);

        this.codecRegistry = fromRegistries(fromProviders(new MorphiaShortCutProvider(this, codecProvider)),
            new PrimitiveCodecProvider(codecRegistry),
            codecRegistry,
            fromProviders(new EnumCodecProvider(), typesCodecProvider, codecProvider));
    }

    /**
     * Maps a set of classes
     *
     * @param entityClasses the classes to map
     */
    public synchronized void map(final Class... entityClasses) {
        if (entityClasses != null && entityClasses.length > 0) {
            for (final Class entityClass : entityClasses) {
                if (!isMapped(entityClass)) {
                    addMappedClass(entityClass);
                }
            }
        }
    }

    /**
     * Maps a set of classes
     *
     * @param entityClasses the classes to map
     */
    public synchronized void map(final Set<Class> entityClasses) {
        if (entityClasses != null && !entityClasses.isEmpty()) {
            for (final Class entityClass : entityClasses) {
                if (!isMapped(entityClass)) {
                    addMappedClass(entityClass);
                }
            }
        }
    }

    /**
     * Tries to map all classes in the package specified. Fails if one of the classes is not valid for mapping.
     *
     * @param packageName the name of the package to process
     */
    public synchronized void mapPackage(final String packageName) {
        mapPackage(packageName, false);
    }

    /**
     * Tries to map all classes in the package specified.
     *
     * @param packageName          the name of the package to process
     * @param ignoreInvalidClasses specifies whether to ignore classes in the package that cannot be mapped
     */
    public synchronized void mapPackage(final String packageName, final boolean ignoreInvalidClasses) {
        try {
            for (final Class clazz : ReflectionUtils.getClasses(getClass().getClassLoader(), packageName,
                getOptions().isMapSubPackages())) {
                try {
                    final Embedded embeddedAnn = ReflectionUtils.getClassEmbeddedAnnotation(clazz);
                    final Entity entityAnn = ReflectionUtils.getClassEntityAnnotation(clazz);
                    final boolean isAbstract = Modifier.isAbstract(clazz.getModifiers());
                    if ((entityAnn != null || embeddedAnn != null) && !isAbstract) {
                        map(clazz);
                    }
                } catch (final MappingException ex) {
                    if (!ignoreInvalidClasses) {
                        throw ex;
                    }
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            throw new MappingException("Could not get map classes from package " + packageName, e);
        }
    }

    /**
     * Maps all the classes found in the package to which the given class belongs.
     *
     * @param clazz the class to use when trying to find others to map
     */
    public void mapPackageFromClass(final Class clazz) {
        mapPackage(clazz.getPackage().getName(), false);
    }

    /**
     * Adds an {@link EntityInterceptor}
     *
     * @param ei the interceptor to add
     */
    public void addInterceptor(final EntityInterceptor ei) {
        interceptors.add(ei);
    }

    public boolean hasInterceptors() {
        return !interceptors.isEmpty();
    }

    /**
     * Creates a MappedClass and validates it.
     *
     * @param c the Class to map
     * @return the MappedClass for the given Class
     */
    public MappedClass addMappedClass(final Class c) {
        MappedClass mappedClass = mappedClasses.get(c);
        if (mappedClass == null) {
            //            try {
            final Codec codec1 = codecRegistry.get(c);
            if (codec1 instanceof MorphiaCodec) {
                return addMappedClass(((MorphiaCodec) codec1).getMappedClass());
            }
            //            } catch (CodecConfigurationException e) {
            //                LOG.error(e.getMessage(), e);
            //                return null;
            //            }
        }
        return mappedClass;
    }

    private MappedClass addMappedClass(final MappedClass mc) {
        if (!mc.isInterface()) {
            mc.validate(this);
        }

        mappedClasses.put(mc.getClazz(), mc);
        mappedClassesByCollection.computeIfAbsent(mc.getCollectionName(), s -> new CopyOnWriteArraySet<>())
                                 .add(mc);

        return mc;
    }

    /**
     * Creates a cache for tracking entities seen during processing
     *
     * @return the cache
     */
    public EntityCache createEntityCache() {
        return getOptions().getCacheFactory().createCache();
    }

    /**
     * Converts a Document back to a type-safe java object (POJO)
     *
     * @param <T>         the type of the entity
     * @param datastore   the Datastore to use when fetching this reference
     * @param entityClass The type to return, or use; can be overridden by the @see Mapper.CLASS_NAME_FIELDNAME in the Document
     * @param document    the Document containing the document from mongodb
     * @param cache       the EntityCache to use
     * @return the new entity
     * @morphia.internal
     * @deprecated no replacement is planned
     */
    @Deprecated
    public <T> T fromDocument(final Datastore datastore, final Class<T> entityClass, final Document document, final EntityCache cache) {
        if (document == null) {
            return null;
        }

        return fromDb(datastore, document, opts.getObjectFactory().createInstance(entityClass, document), cache);
    }

    /**
     * Finds any subtypes for the given MappedClass.
     *
     * @param mc the parent type
     * @return the list of subtypes
     * @since 1.3
     */
    public List<MappedClass> getSubTypes(final MappedClass mc) {
        List<MappedClass> subtypes = new ArrayList<>();
        for (MappedClass mappedClass : getMappedClasses()) {
            if (mappedClass.isSubType(mc)) {
                subtypes.add(mappedClass);
            }
        }

        return subtypes;
    }

    public <T> boolean isMappable(final Class<T> clazz) {
        return hasAnnotation(clazz, Entity.class, Embedded.class);
    }

    public CodecRegistry getCodecRegistry() {
        throw new UnsupportedOperationException();
    }

    private <T> boolean hasAnnotation(final Class<T> clazz, final Class<? extends Annotation>... annotations) {
        for (Class<? extends Annotation> annotation : annotations) {
            if(clazz.getAnnotation(annotation) != null) {
                return true;
            }
        }

        return false;
    }

    /**
     * Converts an entity (POJO) to a Document.  A special field will be added to keep track of the class type.
     *
     * @param datastore the Datastore to use when fetching this reference
     * @param document  the Document
     * @param <T>       the type of the referenced entity
     * @return the entity
     * @morphia.internal
     * @deprecated no replacement is planned
     */
    @Deprecated
    <T> T fromDocument(final Datastore datastore, final Document document) {
        if (document.containsKey(opts.getDiscriminatorField())) {
            T entity = opts.getObjectFactory().createInstance(null, document);
            entity = fromDb(datastore, document, entity, createEntityCache());

            return entity;
        } else {
            throw new MappingException(format("The Document does not contain a %s key.  Determining entity type is impossible.",
                opts.getDiscriminatorField()));
        }
    }

    /**
     * Converts a Document back to a type-safe java object (POJO)
     *
     * @param <T>       the type of the entity
     * @param datastore the Datastore to use when fetching this reference
     * @param document  the Document containing the document from mongodb
     * @param entity    the instance to populate
     * @param cache     the EntityCache to use
     * @return the entity
     * @morphia.internal
     * @deprecated no replacement is planned
     */
    @Deprecated
    public <T> T fromDb(final Datastore datastore, final Document document, final T entity, final EntityCache cache) {
        //hack to bypass things and just read the value.
        if (entity instanceof MappedField) {
            readMappedField(datastore, (MappedField) entity, entity, cache, document);
            return entity;
        }

        // check the history key (a key is the namespace + id)

        if (document.containsKey("_id") && getMappedClass(entity).getIdField() != null
            && getMappedClass(entity).getEntityAnnotation() != null) {
            final Key<T> key = new Key(entity.getClass(), getCollectionName(entity.getClass()), document.get("_id"));
            final T cachedInstance = cache.getEntity(key);
            if (cachedInstance != null) {
                return cachedInstance;
            } else {
                cache.putEntity(key, entity); // to avoid stackOverflow in recursive refs
            }
        }

        if (entity instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) entity;
            for (String key : document.keySet()) {
                Object o = document.get(key);
                map.put(key, (o instanceof Document) ? fromDocument(datastore, (Document) o) : o);
            }
        } else if (entity instanceof Collection) {
            Collection<Object> collection = (Collection<Object>) entity;
            for (Object o : ((List) document)) {
                collection.add((o instanceof Document) ? fromDocument(datastore, (Document) o) : o);
            }
        } else {
            final MappedClass mc = getMappedClass(entity);
            mc.callLifecycleMethods(PreLoad.class, entity, document, this);
            try {
                for (final MappedField mf : mc.getPersistenceFields()) {
                    readMappedField(datastore, mf, entity, cache, document);
                }
            } catch (final MappingException e) {
                Object id = document.get("_id");
                String entityName = entity.getClass().getName();
                throw new MappingException(format("Could not map %s with ID: %s in database '%s'", entityName, id,
                    datastore.getDatabase().getName()), e);
            }

            if (document.containsKey("_id") && getMappedClass(entity).getIdField() != null) {
                final Key key = new Key(entity.getClass(), getCollectionName(entity.getClass()), document.get("_id"));
                cache.putEntity(key, entity);
            }
            mc.callLifecycleMethods(PostLoad.class, entity, document, this);
        }
        return entity;
    }

    /**
     * Looks up the class mapped to a named collection.
     *
     * @param collection the collection name
     * @param <T> the class type
     * @return the Class mapped to this collection name
     * @morphia.internal
     * @deprecated no replacement is planned
     */
    @Deprecated
    public <T> Class<T> getClassFromCollection(final String collection) {
        final Set<MappedClass> mcs = mappedClassesByCollection.get(collection);
        if (mcs == null || mcs.isEmpty()) {
            throw new MappingException(format("The collection '%s' is not mapped to a java class.", collection));
        }
        if (mcs.size() > 1) {
            if (LOG.isInfoEnabled()) {
                LOG.info(format("Found more than one class mapped to collection '%s'%s", collection, mcs));
            }
        }
        return (Class<T>) mcs.iterator().next().getClazz();
    }

    /**
     * @morphia.internal
     * @param collection
     * @return
     */
    public List<MappedClass> getClassesMappedToCollection(final String collection) {
        final Set<MappedClass> mcs = mappedClassesByCollection.get(collection);
        if (mcs == null || mcs.isEmpty()) {
            throw new MappingException(format("The collection '%s' is not mapped to a java class.", collection));
        }
        return new ArrayList<>(mcs);
    }

    /**
     * Gets the mapped collection for an object instance or Class reference.
     *
     * @param object the object to process
     * @return the collection name
     * @morphia.internal
     */
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
        throw new UnsupportedOperationException();
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
                final MappedField idField = mappedClass.getIdField();
                if (idField != null) {
                    return idField.getFieldValue(unwrapped);
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
        return id == null ? null : new Key<>(aClass, getCollectionName(aClass), id);
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
        return id == null ? null : new Key<>(aClass, collection, id);
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
        final List<Key<T>> keys = new ArrayList<>(refs.size());
        for (final Object ref : refs) {
            keys.add(this.manualRefToKey(collection, ref));
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
        final List<Key<T>> keys = new ArrayList<>(refs.size());
        for (final DBRef ref : refs) {
            final Key<T> testKey = refToKey(ref);
            keys.add(testKey);
        }
        return keys;
    }

    /**
     * @return map of MappedClasses by class name
     */
    public Map<Class, MappedClass> getMCMap() {
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

        MappedClass mc = null;
        if (isMappable(type)) {

            mc = mappedClasses.get(type);
            if (mc == null) {
                mc = addMappedClass(type);
            }
        }
        return mc;
    }

    /**
     * @return collection of MappedClasses
     */
    public Collection<MappedClass> getMappedClasses() {
        return new ArrayList<>(mappedClasses.values());
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
        throw new UnsupportedOperationException();
//        opts = options;
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
        return id == null ? null : new Key<>(type, getCollectionName(type), id);
    }

    /**
     * Converts a DBRef to a Key
     *
     * @param ref the DBRef to convert
     * @param <T> the type of the referenced entity
     * @return the Key
     */
    public <T> Key<T> refToKey(final DBRef ref) {
        return ref == null ? null : new Key<>((Class<? extends T>) getClassFromCollection(ref.getCollectionName()),
            ref.getCollectionName(), ref.getId());
    }

    /**
     * /**
     * Converts an entity (POJO) to a Document.  A special field will be added to keep track of the class type.
     *
     * @param entity The POJO
     * @return the Document
     */
    public Document toDocument(final Object entity) {
        return toDocument(entity, null);
    }

    /**
     * Converts an entity (POJO) to a Document.  A special field will be added to keep track of the class type.
     *
     * @param entity          The POJO
     * @param involvedObjects A Map of (already converted) POJOs
     * @return the Document
     */
    public Document toDocument(final Object entity, final Map<Object, Document> involvedObjects) {
        return toDocument(entity, involvedObjects, true);
    }

    /**
     * Converts a java object to a mongo-compatible object (possibly a Document for complex mappings).  Very similar to {@link
     * Mapper#toDocument}.  Used (mainly) by query/update operations.
     *
     * @param mf    the MappedField for this value
     * @param mc    the MappedClass for this value
     * @param value the value to convert
     * @return the MongoDB compatible object
     */
    public Object toMongoObject(final MappedField mf, final MappedClass mc, final Object value) {
        if (value == null) {
            return null;
        }

        if( 1 == 1) {
            throw new UnsupportedOperationException();
        } else {
            return null;
        }
/*
        Object mappedValue = value;

        if (value instanceof Query) {
            mappedValue = ((QueryImpl) value).getQueryDocument();
        } else if (isAssignable(mf, value) || isEntity(mc)) {
            //convert the value to Key (DBRef) if the field is @Reference or type is Key/DBRef, or if the destination class is an @Entity
            try {
                if (value instanceof Iterable) {
                    MappedClass mapped = getMappedClass(mf.getSpecializedType());
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
                                                     ? createKey(mf.getSpecializedType(), value)
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
        } else if (value instanceof Document) {  //pass-through
            mappedValue = value;
        } else {
            mappedValue = toMongoObject(value, EmbeddedMapper.shouldSaveClassName(value, mappedValue, mf));
            if (mappedValue instanceof BasicDBList) {
                final BasicDBList list = (BasicDBList) mappedValue;
                if (list.size() != 0) {
                    if (!EmbeddedMapper.shouldSaveClassName(extractFirstElement(value), list.get(0), mf)) {
                        for (Object o : list) {
                            if (o instanceof Document) {
                                ((Document) o).remove(opts.getDiscriminatorField());
                            }
                        }
                    }
                }
            } else if (mappedValue instanceof Document) {
                ((Document) mappedValue).remove(opts.getDiscriminatorField());
            }
        }

        return mappedValue;
*/
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
    public void updateKeyAndVersionInfo(final Datastore datastore, final Document dbObj, final EntityCache cache, final Object entity) {
        final MappedClass mc = getMappedClass(entity);

        // update id field, if there.
        if ((mc.getIdField() != null) && (dbObj != null) && (dbObj.get("_id") != null)) {
            try {
                final MappedField mf = mc.getMappedIdField();
                final Object oldIdValue = mc.getIdField().getFieldValue(entity);
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

        mappedClasses.put(mc.getClazz(), mc);

        Set<MappedClass> mcs = mappedClassesByCollection.get(mc.getCollectionName());
        if (mcs == null) {
            mcs = new CopyOnWriteArraySet<>();
            final Set<MappedClass> temp = mappedClassesByCollection.putIfAbsent(mc.getCollectionName(), mcs);
            if (temp != null) {
                mcs = temp;
            }
        }

        mcs.add(mc);

        return mc;
    }

    private void readMappedField(final Datastore datastore, final MappedField mf, final Object entity, final EntityCache cache,
                                 final Document document) {
        if( 1 == 1) {
            throw new UnsupportedOperationException();
        }

    }

    private void writeMappedField(final Document document, final MappedField mf, final Object entity,
                                  final Map<Object, Document> involvedObjects) {

        if( 1 == 1) {
            throw new UnsupportedOperationException();
        }

/*
        //skip not saved fields.
        if (mf.hasAnnotation(NotSaved.class)) {
            return;
        }

        // get the annotation from the field.
        Class<? extends Annotation> annType = getFieldAnnotation(mf);

        if (Property.class.equals(annType) || Serialized.class.equals(annType) || mf.isTypeMongoCompatible()
            || (getConverters().hasSimpleValueConverter(mf) || (getConverters().hasSimpleValueConverter(mf.getFieldValue(entity))))) {
            opts.getValueMapper().toDocument(entity, mf, document, involvedObjects, this);
        } else if (Reference.class.equals(annType) || MorphiaReference.class == mf.getConcreteType()) {
            opts.getReferenceMapper().toDocument(entity, mf, document, involvedObjects, this);
        } else if (Embedded.class.equals(annType)) {
            opts.getEmbeddedMapper().toDocument(entity, mf, document, involvedObjects, this);
        } else {
            if (LOG.isDebugEnabled()) {
                LOG.debug("No annotation was found, using default mapper " + opts.getDefaultMapper() + " for " + mf);
            }
            opts.getDefaultMapper().toDocument(entity, mf, document, involvedObjects, this);
        }

*/
    }

    <T> Key<T> manualRefToKey(final String collection, final Object id) {
        return id == null ? null : new Key<>((Class<? extends T>) getClassFromCollection(collection), collection, id);
    }

    /**
     * <p> Converts a java object to a mongo-compatible object (possibly a Document for complex mappings). Very similar to {@link
     * Mapper#toDocument} </p> <p> Used (mainly) by query/update operations </p>
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
                final Document dbObj = toDocument(newObj);
                if (!includeClassName) {
                    dbObj.remove(opts.getDiscriminatorField());
                }
                return dbObj;
            } else if (newObj instanceof Document) {
                return newObj;
            } else if (isMap) {
                if (isPropertyType(subType)) {
                    return toDocument(newObj);
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

    Document toDocument(final Object entity, final Map<Object, Document> involvedObjects, final boolean lifecycle) {

        Document document = new Document();
        final MappedClass mc = getMappedClass(entity);

        if (mc.getEntityAnnotation() == null || mc.getEntityAnnotation().useDiscriminator()) {
            document.put(opts.getDiscriminatorField(), entity.getClass().getName());
        }

        if (lifecycle) {
            mc.callLifecycleMethods(PrePersist.class, entity, document, this);
        }

        for (final MappedField mf : mc.getPersistenceFields()) {
            try {
                writeMappedField(document, mf, entity, involvedObjects);
            } catch (Exception e) {
                throw new MappingException("Error mapping field:" + mf, e);
            }
        }
        if (involvedObjects != null) {
            involvedObjects.put(entity, document);
        }

        if (lifecycle) {
            mc.callLifecycleMethods(PreSave.class, entity, document, this);
        }

        return document;
    }

}
