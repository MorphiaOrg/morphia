package dev.morphia.mapping;


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
import dev.morphia.mapping.codec.PrimitiveCodecRegistry;
import dev.morphia.mapping.codec.pojo.EntityModel;
import dev.morphia.mapping.codec.pojo.EntityModelBuilder;
import dev.morphia.mapping.codec.reader.DocumentReader;
import dev.morphia.mapping.codec.references.MorphiaProxy;
import dev.morphia.query.experimental.filters.Filters;
import dev.morphia.sofia.Sofia;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ScanResult;
import org.bson.BsonWriter;
import org.bson.Document;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;

import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;


/**
 * @morphia.internal
 */
@SuppressWarnings({"unchecked", "rawtypes", "removal"})
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
    private final MorphiaCodecProvider morphiaCodecProvider;
    private final Datastore datastore;
    private final CodecRegistry codecRegistry;
    private final Map<Class<?>, List<MappedClass>> hierarchy = new HashMap<>();

    /**
     * Creates a Mapper with the given options.
     *
     * @param datastore     the datastore to use
     * @param codecRegistry the codec registry
     * @param options       the options to use
     * @morphia.internal
     */
    public Mapper(Datastore datastore, CodecRegistry codecRegistry, MapperOptions options) {
        this.datastore = datastore;
        this.options = options;
        morphiaCodecProvider = new MorphiaCodecProvider(this, datastore);
        this.codecRegistry = fromRegistries(
            fromProviders(new MorphiaTypesCodecProvider(this)),
            new PrimitiveCodecRegistry(codecRegistry),
            codecRegistry,
            fromProviders(
                new EnumCodecProvider(),
                new AggregationCodecProvider(this),
                morphiaCodecProvider));
    }

    /**
     * Adds an {@link EntityInterceptor}
     *
     * @param ei the interceptor to add
     */
    public void addInterceptor(EntityInterceptor ei) {
        interceptors.add(ei);
    }

    /**
     * @param clazz the model type
     * @param <T>   type model type
     * @return the new model
     * @morphia.internal
     */
    public <T> EntityModel<T> createEntityModel(Class<T> clazz) {
        return new EntityModelBuilder<>(this.datastore, clazz)
                   .build();
    }

    /**
     * Updates a collection to use a specific WriteConcern
     *
     * @param collection the collection to update
     * @param type       the entity type
     * @return the updated collection
     */
    public MongoCollection enforceWriteConcern(MongoCollection collection, Class type) {
        WriteConcern applied = getWriteConcern(type);
        return applied != null
               ? collection.withWriteConcern(applied)
               : collection;
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
    public <T> T fromDocument(Class<T> type, Document document) {
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
    public <T> Class<T> getClass(Document document) {
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
    public Class getClass(String discriminator) {
        final Class c;
        c = discriminatorLookup.lookup(discriminator);
        if (c == null) {
            throw new MappingException(Sofia.cannotFindTypeInDocument());
        }
        return c;
    }

    /**
     * Looks up the class mapped to a named collection.
     *
     * @param collection the collection name
     * @param <T>        the class type
     * @return the Class mapped to this collection name
     * @morphia.internal
     */
    public <T> Class<T> getClassFromCollection(String collection) {
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
     * Finds all the types mapped to a named collection
     *
     * @param collection the collection to check
     * @return the mapped types
     * @morphia.internal
     */
    public List<MappedClass> getClassesMappedToCollection(String collection) {
        final Set<MappedClass> mcs = mappedClassesByCollection.get(collection);
        if (mcs == null || mcs.isEmpty()) {
            throw new MappingException(Sofia.collectionNotMapped(collection));
        }
        return new ArrayList<>(mcs);
    }

    /**
     * @return the codec registry
     */
    public CodecRegistry getCodecRegistry() {
        return codecRegistry;
    }

    /**
     * @param type the type look up
     * @param <T>  the class type
     * @return the collection mapped for this class
     * @morphia.internal
     */
    public <T> MongoCollection<T> getCollection(Class<T> type) {
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
     * @return the DiscriminatorLookup in use
     */
    public DiscriminatorLookup getDiscriminatorLookup() {
        return discriminatorLookup;
    }

    public List<MappedClass> getHierarcy(Class<?> type) {
        return hierarchy.get(type);
    }

    /**
     * Gets the ID value for an entity
     *
     * @param entity the entity to process
     * @return the ID value
     */
    public Object getId(Object entity) {
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
    @Deprecated(since = "2.0", forRemoval = true)
    public <T> Key<T> getKey(T entity) {
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
    @Deprecated(since = "2.0", forRemoval = true)
    public <T> Key<T> getKey(T entity, String collection) {
        if (entity instanceof Key) {
            return (Key<T>) entity;
        }

        final Object id = getId(entity);
        final Class<T> aClass = (Class<T>) entity.getClass();
        return id == null ? null : new Key<>(aClass, collection, id);
    }

    /**
     * Gets the {@link MappedClass} for the object (type). If it isn't mapped, create a new class and cache it (without validating).
     *
     * @param type the type to process
     * @return the MappedClass for the object given
     */
    public MappedClass getMappedClass(Class type) {
        return getMappedClass(type, false);
    }

    /**
     * Maps a set of classes
     *
     * @param entityClasses the classes to map
     * @return the MappedClass references
     */
    public List<MappedClass> map(Class... entityClasses) {
        return map(List.of(entityClasses), true);
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
        return options;
    }

    public void updateQueryWithDiscriminators(BsonWriter writer, Class<?> type) {
        if (options.enablePolymorphicQueries()) {
            MappedClass mappedClass = getMappedClass(type);
            if (mappedClass == null) {
                return;
            }
            Entity entityAnnotation = mappedClass.getEntityAnnotation();
            if (entityAnnotation == null || entityAnnotation.useDiscriminator()) {
                String key = discriminatorKey(type);
                Set<String> discriminators = subtypeDiscriminators(type);
                if (discriminators.size() > 1) {
                    Filters.in(key, discriminators)
                           .encode(this, writer, EncoderContext.builder().build());
                } else {
                    Filters.eq(key, discriminators.iterator().next())
                           .encode(this, writer, EncoderContext.builder().build());
                }
            }
        }
    }

    private String discriminatorKey(Class<?> type) {
        return mappedClasses.get(type)
                            .getEntityModel()
                            .getDiscriminatorKey();
    }

    /**
     * Sets the options this Mapper should use
     *
     * @param options the options to use
     * @deprecated no longer used
     */
    @SuppressWarnings("unused")
    @Deprecated(since = "2.0", forRemoval = true)
    public void setOptions(MapperOptions options) {
    }

    /**
     * Finds any subtypes for the given MappedClass.
     *
     * @param mc the parent type
     * @return the list of subtypes
     * @morphia.internal
     * @since 1.3
     */
    public List<MappedClass> getSubTypes(MappedClass mc) {
        return mc.getSubtypes();
    }

    /**
     * Gets the write concern for entity or returns the default write concern for this datastore
     *
     * @param clazz the class to use when looking up the WriteConcern
     * @return the write concern for the type
     * @morphia.internal
     */
    public WriteConcern getWriteConcern(Class clazz) {
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
     * @return true if there are global interceptors defined
     */
    public boolean hasInterceptors() {
        return !interceptors.isEmpty();
    }

    /**
     * Checks if a type is mappable or not
     *
     * @param type the class to check
     * @param <T>  the type
     * @return true if the type is mappable
     */
    public <T> boolean isMappable(Class<T> type) {
        final Class actual = MorphiaProxy.class.isAssignableFrom(type) ? type.getSuperclass() : type;
        return hasAnnotation(actual, List.of(Entity.class, Embedded.class));
    }

    /**
     * Checks to see if a Class has been mapped.
     *
     * @param c the Class to check
     * @return true if the Class has been mapped
     */
    public boolean isMapped(Class c) {
        return mappedClasses.containsKey(c);
    }

    /**
     * Maps a set of classes
     *
     * @param classes the classes to map
     * @return the list of mapped classes
     */
    public List<MappedClass> map(List<Class> classes) {
        return map(classes, true);
    }

    /**
     * Tries to map all classes in the package specified.
     *
     * @param packageName the name of the package to process
     */
    public synchronized void mapPackage(String packageName) {
        try {
            map(getClasses(getClass().getClassLoader(), packageName, getOptions().isMapSubPackages()), false);
        } catch (ClassNotFoundException e) {
            throw new MappingException("Could not get map classes from package " + packageName, e);
        }
    }

    private MappedClass register(MappedClass mc) {
        EntityModel<?> entityModel = mc.getEntityModel();
        discriminatorLookup.addModel(entityModel);
        registerHierarchy(entityModel.getType(), mc);

        mappedClasses.put(mc.getType(), mc);
        if (mc.getEntityAnnotation() != null) {
            mappedClassesByCollection.computeIfAbsent(mc.getCollectionName(), s -> new CopyOnWriteArraySet<>())
                                     .add(mc);
        }

        if (!mc.isInterface()) {
            mc.validate(this);
        }

        return mc;
    }

    private List<Class> getClasses(ClassLoader loader, String packageName, boolean mapSubPackages)
        throws ClassNotFoundException {
        final Set<Class> classes = new HashSet<>();

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
            for (ClassInfo classInfo : scanResult.getAllClasses()) {
                classes.add(Class.forName(classInfo.getName(), true, loader));
            }
        }
        return new ArrayList<>(classes);
    }

    /**
     * Gets the {@link MappedClass} for the object (type). If it isn't mapped, create a new class and cache it (without validating).
     *
     * @param type the type to process
     * @return the MappedClass for the object given
     */
    private MappedClass getMappedClass(Class type, boolean allowUnannotated) {
        if (type == null) {
            return null;
        }
        final Class actual = MorphiaProxy.class.isAssignableFrom(type) ? type.getSuperclass() : type;
        MappedClass mc = mappedClasses.get(actual);

        if (mc == null) {
            if (!isMappable(actual) && !allowUnannotated) {
                return null;
            }
            mc = register(new MappedClass(createEntityModel(type), this));
        }

        return mc;
    }

    /**
     * Maps all the classes found in the package to which the given class belongs.
     *
     * @param clazz the class to use when trying to find others to map
     */
    public void mapPackageFromClass(Class clazz) {
        mapPackage(clazz.getPackage().getName());
    }

    /**
     * Refreshes an entity with the current state in the database.
     *
     * @param entity the entity to refresh
     * @param <T>    the entity type
     */
    public <T> void refresh(T entity) {
        Codec<T> refreshCodec = morphiaCodecProvider.getRefreshCodec(entity, getCodecRegistry());

        MongoCollection<?> collection = getCollection(entity.getClass());
        Document id = collection.find(new Document("_id", getMappedClass(entity.getClass())
                                                              .getIdField()
                                                              .getFieldValue(entity)), Document.class)
                                .first();

        refreshCodec.decode(new DocumentReader(id), DecoderContext.builder().checkedDiscriminator(true).build());
    }

    /**
     * Converts an entity (POJO) to a Document.  A special field will be added to keep track of the class type.
     *
     * @param entity The POJO
     * @return the Document
     * @morphia.internal
     */
    public Document toDocument(Object entity) {

        final MappedClass mc = getMappedClass(entity.getClass());

        DocumentWriter writer = new DocumentWriter();
        Codec codec = getCodecRegistry().get(mc.getType());
        codec.encode(writer, entity,
            EncoderContext.builder()
                          .build());

        return writer.getDocument();
    }

    /**
     * Updates the collection value on a Key with the mapped value on the Key's type Class
     *
     * @param key the Key to update
     * @return the collection name on the Key
     */
    @Deprecated(since = "2.0", forRemoval = true)
    public String updateCollection(Key key) {
        if (key.getCollection() == null && key.getType() == null) {
            throw new IllegalStateException("Key is invalid! " + key);
        } else if (key.getCollection() == null) {
            key.setCollection(getMappedClass(key.getType()).getCollectionName());
        }

        return key.getCollection();
    }

    private List<MappedClass> map(List<Class> classes, boolean allowUnannotated) {
        return classes.stream()
                      .map(c -> getMappedClass(c, allowUnannotated))
                      .filter(Objects::nonNull)
                      .collect(Collectors.toList());
    }

    private void registerHierarchy(Class<?> type, MappedClass mappedClass) {
        if (type != null && !type.equals(Object.class)) {
            List<MappedClass> subtypes = hierarchy.get(type);
            if (subtypes == null) {
                subtypes = new ArrayList<>();
                hierarchy.put(type, subtypes);
                registerHierarchy(type.getSuperclass(), mappedClass);
            }
            if (mappedClass != null) {
                subtypes.add(mappedClass);
            }
        }
    }

    private Set<String> subtypeDiscriminators(Class<?> type) {
        List<MappedClass> mappedClasses = hierarchy.get(type);
        String key = discriminatorKey(type);

        return mappedClasses.stream()
                            .map(MappedClass::getEntityModel)
                            .filter(m -> key.equals(m.getDiscriminatorKey()))
                            .map(EntityModel::getDiscriminator)
                            .collect(Collectors.toSet());
    }

    private <T> boolean hasAnnotation(Class<T> clazz, List<Class<? extends Annotation>> annotations) {
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

}
