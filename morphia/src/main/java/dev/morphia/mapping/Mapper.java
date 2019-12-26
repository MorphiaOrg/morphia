package dev.morphia.mapping;


import com.mongodb.DBRef;
import com.mongodb.WriteConcern;
import com.mongodb.client.MongoCollection;
import dev.morphia.Datastore;
import dev.morphia.EntityInterceptor;
import dev.morphia.Key;
import dev.morphia.aggregation.experimental.codecs.AggregationCodecProvider;
import dev.morphia.annotations.Embedded;
import dev.morphia.annotations.Entity;
import dev.morphia.mapping.codec.DocumentWriter;
import dev.morphia.mapping.codec.EnumCodecProvider;
import dev.morphia.mapping.codec.MorphiaCodecProvider;
import dev.morphia.mapping.codec.MorphiaTypesCodecProvider;
import dev.morphia.mapping.codec.PrimitiveCodecProvider;
import dev.morphia.mapping.codec.pojo.EntityModel;
import dev.morphia.mapping.codec.pojo.EntityModelBuilder;
import dev.morphia.mapping.codec.reader.DocumentReader;
import dev.morphia.mapping.codec.references.MorphiaProxy;
import dev.morphia.sofia.Sofia;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ScanResult;
import org.bson.Document;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.DiscriminatorLookup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;

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

    private static final Logger LOG = LoggerFactory.getLogger(Mapper.class);

    /**
     * Set of classes that registered by this mapper
     */
    private final Map<Class, MappedClass> mappedClasses = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Set<MappedClass>> mappedClassesByCollection = new ConcurrentHashMap<>();

    //EntityInterceptors; these are called after EntityListeners and lifecycle methods on an Entity, for all Entities
    private final List<EntityInterceptor> interceptors = new LinkedList<>();
    private final MapperOptions options;
    private final DiscriminatorLookup discriminatorLookup = new DiscriminatorLookup(Collections.emptyMap(), Collections.emptySet());
    private Datastore datastore;
    private CodecRegistry codecRegistry;

    /**
     * Creates a Mapper with the given options.
     *
     * @param datastore     the datastore to use
     * @param codecRegistry the codec registry
     * @param options       the options to use
     * @morphia.internal
     */
    public Mapper(final Datastore datastore, final CodecRegistry codecRegistry, final MapperOptions options) {
        this.datastore = datastore;
        this.options = options;
        this.codecRegistry = fromRegistries(
            new PrimitiveCodecProvider(codecRegistry),
            codecRegistry,
            fromProviders(
                new EnumCodecProvider(),
                new MorphiaTypesCodecProvider(this),
                new AggregationCodecProvider(this),
                new MorphiaCodecProvider(this, datastore)));
    }

    /**
     * Checks if a type is mappable or not
     *
     * @param type the class to check
     * @param <T>  the type
     * @return true if the type is mappable
     */
    public <T> boolean isMappable(final Class<T> type) {
        final Class actual = MorphiaProxy.class.isAssignableFrom(type) ? type.getSuperclass() : type;
        return hasAnnotation(actual, List.of(Entity.class, Embedded.class));
    }

    /**
     * @return the DiscriminatorLookup in use
     */
    public DiscriminatorLookup getDiscriminatorLookup() {
        return discriminatorLookup;
    }

    /**
     * @param clazz the model type
     * @param <T>   type model type
     * @return the new model
     * @morphia.internal
     */
    public <T> EntityModel<T> createMorphiaModel(final Class<T> clazz) {
        return new EntityModelBuilder<>(this.datastore, clazz)
                   .build();
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
     * Tries to map all classes in the package specified.
     *
     * @param packageName the name of the package to process
     */
    public synchronized void mapPackage(final String packageName) {
        try {
            for (final Class clazz : getClasses(getClass().getClassLoader(), packageName,
                getOptions().isMapSubPackages())) {
                map(clazz);
            }
        } catch (ClassNotFoundException e) {
            throw new MappingException("Could not get map classes from package " + packageName, e);
        }
    }

    /**
     * @return the options used by this Mapper
     */
    public MapperOptions getOptions() {
        return options;
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
     * Maps a set of classes
     *
     * @param entityClasses the classes to map
     * @return the MappedClass references
     */
    public List<MappedClass> map(final Class... entityClasses) {
        return map(List.of(entityClasses));
    }

    /**
     * Maps a set of classes
     *
     * @param classes the classes to map
     * @return the list of mapped classes
     */
    public List<MappedClass> map(final List<Class> classes) {
        return classes.stream()
                      .map(c -> getMappedClass(c))
                      .filter(mc -> mc != null)
                      .collect(Collectors.toList());
    }

    /**
     * Gets the {@link MappedClass} for the object (type). If it isn't mapped, create a new class and cache it (without validating).
     *
     * @param type the type to process
     * @return the MappedClass for the object given
     */
    public MappedClass getMappedClass(final Class type) {

        if (type == null || !isMappable(type)) {
            return null;
        }

        final Class actual = MorphiaProxy.class.isAssignableFrom(type) ? type.getSuperclass() : type;
        MappedClass mc = mappedClasses.get(actual);
        if (mc == null) {
            mc = addMappedClass(actual);
        }
        return mc;
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
     * @return true if there are global interceptors defined
     */
    public boolean hasInterceptors() {
        return !interceptors.isEmpty();
    }

    /**
     * Finds any subtypes for the given MappedClass.
     *
     * @param mc the parent type
     * @return the list of subtypes
     * @morphia.internal
     * @since 1.3
     */
    public List<MappedClass> getSubTypes(final MappedClass mc) {
        return mc.getSubtypes();
    }

    /**
     * @return collection of MappedClasses
     */
    public Collection<MappedClass> getMappedClasses() {
        return new ArrayList<>(mappedClasses.values());
    }

    /**
     * Converts a Document back to a type-safe java object (POJO)
     *
     * @param <T>      the type of the entity
     * @param type     the target type
     * @param document the Document containing the document from mongodb
     * @return the new entity
     * @morphia.internal
     */
    public <T> T fromDocument(final Class<T> type, final Document document) {
        if (document == null) {
            return null;
        }

        Class<T> aClass = type;
        if (document.containsKey(options.getDiscriminatorKey())) {
            aClass = getClass(document);
        }

        CodecRegistry codecRegistry = getCodecRegistry();

        DocumentReader reader = new DocumentReader(document);

        return codecRegistry
                   .get(aClass)
                   .decode(reader, DecoderContext.builder().build());
    }

    /**
     * Gets the class as defined by any discriminator field
     *
     * @param document the document to check
     * @param <T>      the class type
     * @return the class reference.  might be null
     */
    @SuppressWarnings("unchecked")
    public <T> Class<T> getClass(final Document document) {
        // see if there is a className value
        Class c = null;
        String discriminator = (String) document.get(getOptions().getDiscriminatorKey());
        if (discriminator != null) {
            c = getClass(discriminator);
        }
        return c;
    }

    /**
     * @param discriminator the lookup value
     * @return the class mapped to this discrimiator value
     */
    public Class getClass(final String discriminator) {
        final Class c;
        c = discriminatorLookup.lookup(discriminator);
        if (c == null) {
            throw new MappingException(Sofia.cannotFindTypeInDocument());
        }
        return c;
    }

    /**
     * @return the codec registry
     */
    public CodecRegistry getCodecRegistry() {
        return codecRegistry;
    }

    /**
     * Updates a collection to use a specific WriteConcern
     *
     * @param collection the collection to update
     * @param type       the entity type
     * @return the updated collection
     */
    public MongoCollection enforceWriteConcern(final MongoCollection collection, final Class type) {
        WriteConcern applied = getWriteConcern(type);
        return applied != null
               ? collection.withWriteConcern(applied)
               : collection;
    }

    /**
     * Gets the write concern for entity or returns the default write concern for this datastore
     *
     * @param clazz the class to use when looking up the WriteConcern
     * @return the write concern for the type
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
        if (entity instanceof Key) {
            return (Key<T>) entity;
        }

        final Object id = getId(entity);
        final Class<T> aClass = (Class<T>) entity.getClass();
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
        if (entity instanceof Key) {
            return (Key<T>) entity;
        }

        final Object id = getId(entity);
        final Class<T> aClass = (Class<T>) entity.getClass();
        return id == null ? null : new Key<>(aClass, collection, id);
    }

    /**
     * Gets the ID value for an entity
     *
     * @param entity the entity to process
     * @return the ID value
     */
    public Object getId(final Object entity) {
        if (entity == null) {
            return null;
        }
        final MappedClass mappedClass = getMappedClass(entity.getClass());
        if (mappedClass != null) {
            final MappedField idField = mappedClass.getIdField();
            if (idField != null) {
                return idField.getFieldValue(entity);
            }
        }

        return null;
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
     * Looks up the class mapped to a named collection.
     *
     * @param collection the collection name
     * @param <T>        the class type
     * @return the Class mapped to this collection name
     * @morphia.internal
     */
    public <T> Class<T> getClassFromCollection(final String collection) {
        final List<MappedClass> classes = getClassesMappedToCollection(collection);
        if (classes.size() > 1) {
            Sofia.logMoreThanOneMapper(collection,
                classes.stream()
                       .map(c -> c.getType().getName())
                       .collect(Collectors.joining(", ")));
        }
        return (Class<T>) classes.get(0).getType();
    }

    /**
     * @param type the type look up
     * @param <T>  the class type
     * @return the collection mapped for this class
     */
    public <T> MongoCollection<T> getCollection(final Class<T> type) {
        MappedClass mappedClass = getMappedClass(type);
        if (mappedClass == null) {
            throw new MappingException(Sofia.notMappable(type.getName()));
        }
        if (mappedClass.getCollectionName() == null) {
            throw new MappingException(Sofia.noMappedCollection(type.getName()));
        }

        MongoCollection<T> collection = datastore.getDatabase().getCollection(mappedClass.getCollectionName(), type);

        Entity annotation = mappedClass.getEntityAnnotation();
        if (annotation != null && WriteConcern.valueOf(annotation.concern()) != null) {
            collection = collection.withWriteConcern(WriteConcern.valueOf(annotation.concern()));
        }
        return collection;
    }


    /**
     * Finds all the types mapped to a named collection
     *
     * @param collection the collection to check
     * @return the mapped types
     * @morphia.internal
     */
    public List<MappedClass> getClassesMappedToCollection(final String collection) {
        final Set<MappedClass> mcs = mappedClassesByCollection.get(collection);
        if (mcs == null || mcs.isEmpty()) {
            throw new MappingException(Sofia.collectionNotMapped(collection));
        }
        return new ArrayList<>(mcs);
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
     * Checks to see if a Class has been mapped.
     *
     * @param c the Class to check
     * @return true if the Class has been mapped
     */
    public boolean isMapped(final Class c) {
        return mappedClasses.containsKey(c);
    }

    /**
     * Converts an entity (POJO) to a Document.  A special field will be added to keep track of the class type.
     *
     * @param entity The POJO
     * @return the Document
     * @morphia.internal
     */
    public Document toDocument(final Object entity) {

        final MappedClass mc = getMappedClass(entity.getClass());

        DocumentWriter writer = new DocumentWriter();
        Codec codec = getCodecRegistry().get(mc.getType());
        codec.encode(writer, entity,
            EncoderContext.builder()
                          .build());

        return writer.getRoot();
    }

    /**
     * Updates the collection value on a Key with the mapped value on the Key's type Class
     *
     * @param key the Key to update
     * @return the collection name on the Key
     */
    public String updateCollection(final Key key) {
        if (key.getCollection() == null && key.getType() == null) {
            throw new IllegalStateException("Key is invalid! " + key);
        } else if (key.getCollection() == null) {
            key.setCollection(getMappedClass(key.getType()).getCollectionName());
        }

        return key.getCollection();
    }

    private <T> boolean hasAnnotation(final Class<T> clazz, final List<Class<? extends Annotation>> annotations) {
        if (clazz == null) {
            return false;
        }
        for (Class<? extends Annotation> annotation : annotations) {
            if (clazz.getAnnotation(annotation) != null) {
                return true;
            }
        }

        return hasAnnotation(clazz.getSuperclass(), annotations)
               || Arrays.stream(clazz.getInterfaces())
                        .map(i -> hasAnnotation(i, annotations))
                        .reduce(false, (l, r) -> l || r);
    }

    private Set<Class<?>> getClasses(final ClassLoader loader, final String packageName, final boolean mapSubPackages)
        throws ClassNotFoundException {
        final Set<Class<?>> classes = new HashSet<>();

        ClassGraph classGraph = new ClassGraph()
                                    .addClassLoader(loader)
                                    .enableAllInfo();
        if (mapSubPackages) {
            classGraph.whitelistPackages(packageName);
            classGraph.whitelistPackages(packageName + ".*");
        } else {
            classGraph.whitelistPackagesNonRecursive(packageName);
        }

        try (ScanResult scanResult = classGraph.scan()) {
            for (final ClassInfo classInfo : scanResult.getAllClasses()) {
                classes.add(Class.forName(classInfo.getName(), true, loader));
            }
        }
        return classes;
    }

    /**
     * Creates a MappedClass and validates it.
     *
     * @param type the Class to map
     * @return the MappedClass for the given Class
     */
    private MappedClass addMappedClass(final Class type) {
        MappedClass mappedClass = mappedClasses.get(type);
        if (mappedClass == null && isMappable(type)) {
            EntityModel entityModel = createMorphiaModel(type);
            mappedClass = addMappedClass(new MappedClass(entityModel, this));
        }
        return mappedClass;
    }

    private MappedClass addMappedClass(final MappedClass mc) {
        mappedClasses.put(mc.getType(), mc);
        if (mc.getEntityAnnotation() != null) {
            mappedClassesByCollection.computeIfAbsent(mc.getCollectionName(), s -> new CopyOnWriteArraySet<>())
                                     .add(mc);
        }
        discriminatorLookup.addClassModel(mc.getEntityModel());

        if (!mc.isInterface()) {
            mc.validate(this);
        }

        return mc;
    }

    <T> Key<T> manualRefToKey(final String collection, final Object id) {
        return id == null ? null : new Key<>((Class<? extends T>) getClassFromCollection(collection), collection, id);
    }

}
