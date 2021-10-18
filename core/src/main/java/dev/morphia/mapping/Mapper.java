package dev.morphia.mapping;


import com.mongodb.WriteConcern;
import com.mongodb.client.MongoCollection;
import com.mongodb.lang.Nullable;
import dev.morphia.EntityInterceptor;
import dev.morphia.Key;
import dev.morphia.annotations.Embedded;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.builders.EmbeddedBuilder;
import dev.morphia.mapping.codec.pojo.EntityModel;
import dev.morphia.mapping.codec.pojo.EntityModelBuilder;
import dev.morphia.mapping.codec.pojo.PropertyModel;
import dev.morphia.mapping.codec.references.MorphiaProxy;
import dev.morphia.mapping.validation.MappingValidator;
import dev.morphia.sofia.Sofia;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ScanResult;
import org.bson.Document;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;

import static dev.morphia.annotations.builders.EmbeddedBuilder.embeddedBuilder;
import static dev.morphia.sofia.Sofia.entityOrEmbedded;


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

    /**
     * Set of classes that registered by this mapper
     */
    private final Map<Class, EntityModel> mappedEntities = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Set<EntityModel>> mappedEntitiesByCollection = new ConcurrentHashMap<>();

    //EntityInterceptors; these are called after EntityListeners and lifecycle methods on an Entity, for all Entities
    private final List<EntityInterceptor> interceptors = new LinkedList<>();
    private final MapperOptions options;
    private final DiscriminatorLookup discriminatorLookup;

    /**
     * Creates a Mapper with the given options.
     *
     * @param options the options to use
     * @morphia.internal
     */
    public Mapper(MapperOptions options) {
        this.options = options;
        discriminatorLookup = new DiscriminatorLookup(options.getClassLoader());
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
     * @param type the class
     * @return the id property model
     * @morphia.internal
     * @since 2.2
     */
    public PropertyModel findIdProperty(Class<?> type) {
        EntityModel entityModel = getEntityModel(type);
        PropertyModel idField = entityModel.getIdProperty();

        if (idField == null) {
            throw new MappingException(Sofia.idRequired(type.getName()));
        }
        return idField;
    }

    /**
     * Gets the class as defined by any discriminator field
     *
     * @param document the document to check
     * @param <T>      the class type
     * @return the class reference.  might be null
     */
    @SuppressWarnings("unchecked")
    @Nullable
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
        return discriminatorLookup.lookup(discriminator);
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
        final List<EntityModel> classes = getClassesMappedToCollection(collection);
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
    public List<EntityModel> getClassesMappedToCollection(String collection) {
        final Set<EntityModel> entities = mappedEntitiesByCollection.get(collection);
        if (entities == null || entities.isEmpty()) {
            throw new MappingException(Sofia.collectionNotMapped(collection));
        }
        return new ArrayList<>(entities);
    }

    /**
     * @return the DiscriminatorLookup in use
     */
    public DiscriminatorLookup getDiscriminatorLookup() {
        return discriminatorLookup;
    }

    /**
     * Gets the {@link EntityModel} for the object (type). If it isn't mapped, create a new class and cache it (without validating).
     *
     * @param type the type to process
     * @return the EntityModel for the object given
     */
    @Nullable
    public EntityModel getEntityModel(Class type) {
        final Class actual = MorphiaProxy.class.isAssignableFrom(type) ? type.getSuperclass() : type;
        EntityModel model = mappedEntities.get(actual);

        if (model == null) {
            if (!isMappable(actual)) {
                throw new NotMappableException(type);
            }
            model = register(createEntityModel(type));
        }

        return model;
    }

    /**
     * Gets the ID value for an entity
     *
     * @param entity the entity to process
     * @return the ID value
     */
    @Nullable
    public Object getId(@Nullable Object entity) {
        if (entity == null) {
            return null;
        }
        try {
            final EntityModel model = getEntityModel(entity.getClass());
            final PropertyModel idField = model.getIdProperty();
            if (idField != null) {
                return idField.getValue(entity);
            }
        } catch (NotMappableException ignored) {
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
    @Nullable
    @Deprecated(since = "2.0", forRemoval = true)
    public <T> Key<T> getKey(T entity) {
        if (entity instanceof Key) {
            return (Key<T>) entity;
        }

        final Object id = getId(entity);
        final Class<T> aClass = (Class<T>) entity.getClass();
        return id == null ? null : new Key<>(aClass, getEntityModel(aClass).getCollectionName(), id);
    }

    /**
     * Gets the Key for an entity and a specific collection
     *
     * @param entity     the entity to process
     * @param collection the collection to use in the Key rather than the mapped collection as defined on the entity's class
     * @param <T>        the type of the entity
     * @return the Key
     */
    @Nullable
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
     * @return collection of EntityModels
     */
    public List<EntityModel> getMappedEntities() {
        return new ArrayList<>(mappedEntities.values());
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
    public void setOptions(MapperOptions options) {
    }

    /**
     * Gets the write concern for entity or returns the default write concern for this datastore
     *
     * @param clazz the class to use when looking up the WriteConcern
     * @return the write concern for the type
     * @morphia.internal
     */
    @Nullable
    public WriteConcern getWriteConcern(Class clazz) {
        WriteConcern wc = null;
        EntityModel entityModel = getEntityModel(clazz);
        if (entityModel != null) {
            final Entity entityAnn = entityModel.getEntityAnnotation();
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
        return mappedEntities.containsKey(c);
    }

    /**
     * Maps a set of classes
     *
     * @param entityClasses the classes to map
     * @return the EntityModel references
     */
    public List<EntityModel> map(Class... entityClasses) {
        return map(List.of(entityClasses));
    }

    /**
     * Maps a set of classes
     *
     * @param classes the classes to map
     * @return the list of mapped classes
     */
    public List<EntityModel> map(List<Class> classes) {
        for (Class type : classes) {
            if (!isMappable(type)) {
                throw new MappingException(entityOrEmbedded(type.getName()));
            }
        }
        return classes.stream()
                      .map(this::getEntityModel)
                      .filter(Objects::nonNull)
                      .collect(Collectors.toList());
    }

    /**
     * Maps an external class.  This is intended for use on types needed in a system but come from an external source where the more
     * traditional approach of decorating the type in source with Morphia annotations is not possible.
     *
     * @param annotation the annotation to apply.  pass null to apply the defaults.
     * @param type       the type to map
     * @param <A>        the annotation to apply.  Currently only {@code @Embedded} is supported
     * @return the list of mapped classes
     * @morphia.experimental
     * @see EmbeddedBuilder
     * @since 2.1
     */
    public <A extends Annotation> EntityModel mapExternal(@Nullable A annotation, Class type) {
        final Class actual = MorphiaProxy.class.isAssignableFrom(type) ? type.getSuperclass() : type;
        EntityModel model = mappedEntities.get(actual);

        if (model == null) {
            if (annotation == null) {
                annotation = (A) embeddedBuilder().build();
            }
            model = register(createEntityModel(type, annotation));
        }


        return model;
    }

    /**
     * Tries to map all classes in the package specified.
     *
     * @param packageName the name of the package to process
     */
    public synchronized void mapPackage(String packageName) {
        try {
            getClasses(options.getClassLoader(), packageName, getOptions().isMapSubPackages())
                .stream()
                .map(type -> {
                    try {
                        return getEntityModel(type);
                    } catch (NotMappableException e) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        } catch (ClassNotFoundException e) {
            throw new MappingException("Could not get map classes from package " + packageName, e);
        }
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
     * Updates the collection value on a Key with the mapped value on the Key's type Class
     *
     * @param key the Key to update
     * @return the collection name on the Key
     */
    @Deprecated(since = "2.0", forRemoval = true)
    public String updateCollection(Key key) {
        String collection = key.getCollection();
        Class type = key.getType();
        if (collection == null && type == null) {
            throw new IllegalStateException("Key is invalid! " + key);
        } else if (collection == null) {
            collection = getEntityModel(type).getCollectionName();
            key.setCollection(collection);
        }

        return collection;
    }

    /**
     * Updates a query with any discriminators from subtypes if polymorphic queries are enabled
     *
     * @param model the query model
     * @param query the query document
     */
    public void updateQueryWithDiscriminators(EntityModel model, Document query) {
        Entity annotation = model.getEntityAnnotation();
        if (annotation != null && annotation.useDiscriminator()
            && !query.containsKey("_id")
            && !query.containsKey(model.getDiscriminatorKey())) {
            List<EntityModel> subtypes = model.getSubtypes();
            List<String> values = new ArrayList<>();
            values.add(model.getDiscriminator());
            if (options.isEnablePolymorphicQueries()) {
                for (EntityModel subtype : subtypes) {
                    values.add(subtype.getDiscriminator());
                }
            }
            query.put(model.getDiscriminatorKey(),
                new Document("$in", values));
        }
    }

    /**
     * @param entityModel the model to register
     * @return the model
     * @morphia.internal
     * @since 2.3
     */
    public EntityModel register(EntityModel entityModel) {
        discriminatorLookup.addModel(entityModel);
        mappedEntities.put(entityModel.getType(), entityModel);
        if (entityModel.getCollectionName() != null) {
            mappedEntitiesByCollection.computeIfAbsent(entityModel.getCollectionName(), s -> new CopyOnWriteArraySet<>())
                                      .add(entityModel);
        }

        if (!entityModel.isInterface()) {
            new MappingValidator()
                .validate(this, entityModel);

        }
        return entityModel;
    }

    /**
     * @param clazz the model type
     * @param <T>   type model type
     * @return the new model
     * @morphia.internal
     */
    private <T> EntityModel createEntityModel(Class<T> clazz) {
        return new EntityModelBuilder(this, clazz)
            .build();
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

    private <T> boolean hasAnnotation(Class<T> clazz, List<Class<? extends Annotation>> annotations) {
        for (Class<? extends Annotation> annotation : annotations) {
            if (clazz.getAnnotation(annotation) != null) {
                return true;
            }
        }

        return clazz.getSuperclass() != null && hasAnnotation(clazz.getSuperclass(), annotations)
               || Arrays.stream(clazz.getInterfaces())
                        .map(i -> hasAnnotation(i, annotations))
                        .reduce(false, (l, r) -> l || r);
    }

    private <T, A extends Annotation> EntityModel createEntityModel(Class<T> clazz, A annotation) {
        return new EntityModelBuilder(this, annotation, clazz)
            .build();
    }

}
