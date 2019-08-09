package dev.morphia.mapping;


import com.mongodb.DBRef;
import com.mongodb.WriteConcern;
import com.mongodb.client.MongoCollection;
import dev.morphia.Datastore;
import dev.morphia.EntityInterceptor;
import dev.morphia.Key;
import dev.morphia.annotations.Converters;
import dev.morphia.annotations.Embedded;
import dev.morphia.annotations.Entity;
import dev.morphia.mapping.cache.EntityCache;
import dev.morphia.mapping.codec.DocumentWriter;
import dev.morphia.mapping.codec.EnumCodecProvider;
import dev.morphia.mapping.codec.MorphiaCodecProvider;
import dev.morphia.mapping.codec.MorphiaTypesCodecProvider;
import dev.morphia.mapping.codec.PrimitiveCodecProvider;
import dev.morphia.mapping.codec.pojo.MorphiaCodec;
import dev.morphia.mapping.lazy.proxy.ProxiedEntityReference;
import dev.morphia.mapping.lazy.proxy.ProxyHelper;
import dev.morphia.query.Query;
import dev.morphia.query.QueryImpl;
import dev.morphia.sofia.Sofia;
import dev.morphia.utils.ReflectionUtils;
import org.bson.BsonDocumentReader;
import org.bson.Document;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecConfigurationException;
import org.bson.codecs.configuration.CodecRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.lang.Thread.currentThread;
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

    private CodecRegistry codecRegistry;
    private Datastore datastore;
    private final MapperOptions opts;

    // TODO:  unify with DefaultCreator if it survives the Codec switchover
    private Map<String, Class> classNameCache = new ConcurrentHashMap<>();

    /**
     * Creates a Mapper with the given options.
     *
     * @morphia.internal
     * @param opts the options to use
     */
    public Mapper(final Datastore datastore, final CodecRegistry codecRegistry, final MapperOptions opts) {
        this.datastore = datastore;
        this.opts = opts;
        this.codecRegistry = fromRegistries(
            new PrimitiveCodecProvider(codecRegistry),
            codecRegistry,
            fromProviders(
                new EnumCodecProvider(),
                new MorphiaTypesCodecProvider(this),
                new MorphiaCodecProvider(datastore, this,
                    List.of(new MorphiaConvention(datastore, opts)), Set.of(""))));
    }

    public Datastore getDatastore() {
        return datastore;
    }

    /**
     * Maps a set of classes
     *
     * @param entityClasses the classes to map
     * @return
     * @deprecated use {@link #map(Set)}
     */
    @Deprecated(since = "2.0", forRemoval = true)
    public List<MappedClass> map(final Class... entityClasses) {
        return map(Set.of(entityClasses));
    }

    /**
     * Maps a set of classes
     *
     * @param classes the classes to map
     * @return the list of mapped classes
     */
    public List<MappedClass> map(final Set<Class> classes) {
        return classes.stream()
                      .map(c -> getMappedClass(c))
                      .filter(mc -> mc != null)
                      .collect(Collectors.toList());
    }

    /**
     * Tries to map all classes in the package specified.
     *
     * @param packageName the name of the package to process
     */
    public synchronized void mapPackage(final String packageName) {
        try {
            for (final Class clazz : ReflectionUtils.getClasses(getClass().getClassLoader(), packageName,
                getOptions().isMapSubPackages())) {
                final Embedded embeddedAnn = ReflectionUtils.getClassEmbeddedAnnotation(clazz);
                final Entity entityAnn = ReflectionUtils.getClassEntityAnnotation(clazz);
                final boolean isAbstract = Modifier.isAbstract(clazz.getModifiers());
                if ((entityAnn != null || embeddedAnn != null) && !isAbstract) {
                    map(clazz);
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
        mapPackage(clazz.getPackage().getName());
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
    private MappedClass addMappedClass(final Class c) {
        MappedClass mappedClass = mappedClasses.get(c);
        if (mappedClass == null) {
            try {
                final Codec codec1 = codecRegistry.get(c);
                if (codec1 instanceof MorphiaCodec) {
                    return addMappedClass(((MorphiaCodec) codec1).getMappedClass());
                }
            } catch (CodecConfigurationException e) {
               // unmappable type
            }
        }
        return mappedClass;
    }

    private MappedClass addMappedClass(final MappedClass mc) {
        mappedClasses.put(mc.getType(), mc);
        if(mc.getEntityAnnotation() != null) {
            mappedClassesByCollection.computeIfAbsent(mc.getCollectionName(), s -> new CopyOnWriteArraySet<>())
                                     .add(mc);
        }

        if (!mc.isInterface()) {
            mc.validate(this);
        }

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

    public CodecRegistry getCodecRegistry() {
        return codecRegistry;
    }

    /**
     * Converts a Document back to a type-safe java object (POJO)
     *
     * @param <T>         the type of the entity
     * @param clazz
     * @param document    the Document containing the document from mongodb
     * @return the new entity
     * @morphia.internal
     */
    public <T> T fromDocument(final Class<T> clazz, final Document document) {
        if (document == null) {
            return null;
        }

        if (document.containsKey(opts.getDiscriminatorField())) {
            CodecRegistry codecRegistry = getCodecRegistry();
            BsonDocumentReader reader = new BsonDocumentReader(document.toBsonDocument(getClass(document), codecRegistry));

            return codecRegistry
                       .get(clazz)
                       .decode(reader, DecoderContext.builder().build());
        } else {
            throw new MappingException(format("The Document does not contain a %s key.  Unable to determine the entity type.",
                opts.getDiscriminatorField()));
        }
    }

    @SuppressWarnings("unchecked")
    private <T> Class<T> getClass(final Document document) {
        // see if there is a className value
        Class c = null;
        if (document.containsKey(getOptions().getDiscriminatorField())) {
            final String className = (String) document.get(getOptions().getDiscriminatorField());
            // try to Class.forName(className) as defined in the documentect first,
            // otherwise return the entityClass
            try {
                if (getOptions().isCacheClassLookups()) {
                    c = classNameCache.get(className);
                    if (c == null) {
                        c = Class.forName(className, true, currentThread().getContextClassLoader());
                        classNameCache.put(className, c);
                    }
                } else {
                    c = Class.forName(className, true, currentThread().getContextClassLoader());
                }
            } catch (ClassNotFoundException e) {
                if (LOG.isWarnEnabled()) {
                    LOG.warn("Class not found defined in document: ", e);
                }
            }
        }
        return c;
    }

    /**
     * Converts a Document back to a type-safe java object (POJO)
     *
     * @param <T>       the type of the entity
     * @param document  the Document containing the document from mongodb
     * @return the entity
     * @morphia.internal
     * @deprecated no replacement is planned
     */
    private <T> T fromDb(final Document document) {

        CodecRegistry codecRegistry = getCodecRegistry();
        BsonDocumentReader reader = new BsonDocumentReader(document.toBsonDocument(getClass(document), codecRegistry));
        T decoded = (T) codecRegistry
                            .get(Object.class)
                            .decode(reader, DecoderContext.builder().checkedDiscriminator(true).build());

        return decoded;

/*
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
                for (final MappedField mf : mc.getFields()) {
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
*/
    }

    /**
     * Looks up the class mapped to a named collection.
     *
     * @param collection the collection name
     * @param <T> the class type
     * @return the Class mapped to this collection name
     * @morphia.internal
     */
    public <T> Class<T> getClassFromCollection(final String collection) {
        final List<MappedClass> classes = getClassesMappedToCollection(collection);
        if (classes.size() > 1) {
                Sofia.logMoreThanOneMapper(collection,
                    classes.stream()
                           .map(c-> c.getType().getName())
                               .collect(Collectors.joining(", ")));
        }
        return (Class<T>) classes.get(0).getType();
    }

    /**
     * @morphia.internal
     * @param collection
     * @return
     */
    public List<MappedClass> getClassesMappedToCollection(final String collection) {
        final Set<MappedClass> mcs = mappedClassesByCollection.get(collection);
        if (mcs == null || mcs.isEmpty()) {
            throw new MappingException(Sofia.collectionNotMapped(collection));
        }
        return new ArrayList<>(mcs);
    }

    public <T> MongoCollection<T> getCollection(final Class<T> clazz) {
        MappedClass mappedClass = getMappedClass(clazz);
        if (mappedClass == null) {
            throw new IllegalArgumentException(Sofia.notMappable(clazz.getName()));
        }
        if (mappedClass.getCollectionName() == null) {
            throw new MappingException(Sofia.noMappedCollection(clazz.getName()));
        }

        MongoCollection<T> collection = null;
        if (mappedClass.getEntityAnnotation() != null) {
            collection = datastore.getDatabase().getCollection(mappedClass.getCollectionName(), clazz);
            collection = enforceWriteConcern(collection, clazz);
        }
        return collection;
    }

    public MongoCollection enforceWriteConcern(final MongoCollection collection, final Class klass) {
        WriteConcern applied = getWriteConcern(klass);
        return applied != null
               ? collection.withWriteConcern(applied)
               : collection;
    }

    /**
     * Gets the write concern for entity or returns the default write concern for this datastore
     *
     * @param clazz the class to use when looking up the WriteConcern
     * @morphia.internal
     */
    public WriteConcern getWriteConcern(final Class clazz) {
        WriteConcern wc = null;
        if (clazz != null) {
            final Entity entityAnn = getMappedClass(clazz).getEntityAnnotation();
            if (entityAnn != null && !entityAnn.concern().isEmpty()) {
                wc = WriteConcern.valueOf(entityAnn.concern());
            }
        }

        return wc;
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
        return id == null ? null : new Key<>(aClass, getMappedClass(aClass).getCollectionName(), id);
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
        final String collection = getMappedClass(clazz).getCollectionName();
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
     * @param type the type to process
     * @return the MappedClass for the object given
     */
    public MappedClass getMappedClass(final Class type) {
        if (type == null) {
            return null;
        }

        MappedClass mc = mappedClasses.get(type);
        if (mc == null) {
            mc = addMappedClass(type);
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
     * @deprecated no longer used
     */
    @SuppressWarnings("unused")
    @Deprecated(since = "2.0", forRemoval = true)
    public void setOptions(final MapperOptions options) {
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
            key.setCollection(getMappedClass(key.getType()).getCollectionName());
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
        return id == null ? null : new Key<>(type, getMappedClass(type).getCollectionName(), id);
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
     * Converts an entity (POJO) to a Document.  A special field will be added to keep track of the class type.
     *
     * @param entity          The POJO
     * @return the Document
     */
    public Document toDocument(final Object entity) {
        return toDocument(entity, true);
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

        Object mappedValue = value;


        if (value instanceof Query) {
            mappedValue = ((QueryImpl) value).getQueryDocument();
        } else {
            mappedValue = toDocument(value);
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
            throw new IllegalStateException("Key is invalid! " + this);
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
        final MappedClass mc = getMappedClass(entity.getClass());

        // update id field, if there.
        if ((mc.getIdField() != null) && (dbObj != null) && (dbObj.get("_id") != null)) {
            try {
                final MappedField mf = mc.getIdField();
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
        if (mc.getVersionField() != null && (dbObj != null)) {
            readMappedField(datastore, mc.getVersionField(), entity, cache, dbObj);
        }
    }

    private void addConverters(final MappedClass mc) {
        if (mc.<Annotation>getAnnotations(Converters.class) != null) {
            LOG.debug("Converters have been replaced by Conversions and custom codecs.  This annotation should be removed once the code "
                      + "has been updated to the new approach.");
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

        mappedClasses.put(mc.getType(), mc);

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

/*
    private void writeMappedField(final Document document, final MappedField mf, final Object entity,
                                  final Map<Object, Document> involvedObjects) {

        if( 1 == 1) {
            throw new UnsupportedOperationException();
        }

*/
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

*//*

    }
*/

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
        DocumentWriter writer = new DocumentWriter();
        getCodecRegistry().get(origClass).encode(writer, javaObj,
            EncoderContext.builder()
                          .isEncodingCollectibleDocument(includeClassName)
                          .build());

        return writer.getRoot();

/*
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
*/
    }

    <T> Document toDocument(final T entity, final boolean lifecycle) {

        final MappedClass mc = getMappedClass(entity.getClass());

        DocumentWriter writer = new DocumentWriter();
        Codec<T> codec = (Codec<T>) getCodecRegistry().get(mc.getType());
        codec.encode(writer, entity,
            EncoderContext.builder()
                          .isEncodingCollectibleDocument(lifecycle)
                          .build());

        return writer.getRoot();

/*
        Document document = new Document();

        if (mc.getEntityAnnotation() == null || mc.getEntityAnnotation().useDiscriminator()) {
            document.put(opts.getDiscriminatorField(), entity.getClass().getName());
        }

        if (lifecycle) {
            mc.callLifecycleMethods(PrePersist.class, entity, document, this);
        }

        for (final MappedField mf : mc.getFields()) {
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
*/
    }

}
