package dev.morphia.mapping;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;

import com.mongodb.WriteConcern;
import com.mongodb.lang.Nullable;

import dev.morphia.EntityListener;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.ExternalEntity;
import dev.morphia.annotations.PostLoad;
import dev.morphia.annotations.PostPersist;
import dev.morphia.annotations.PreLoad;
import dev.morphia.annotations.PrePersist;
import dev.morphia.annotations.internal.MorphiaInternal;
import dev.morphia.config.MorphiaConfig;
import dev.morphia.mapping.codec.pojo.EntityModel;
import dev.morphia.mapping.codec.pojo.PropertyModel;
import dev.morphia.mapping.codec.references.MorphiaProxy;
import dev.morphia.mapping.validation.MappingValidator;
import dev.morphia.sofia.Sofia;

import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ScanResult;

import static java.util.Arrays.asList;

/**
 * @morphia.internal
 * @hidden
 */
@MorphiaInternal
@SuppressWarnings({ "unchecked", "rawtypes" })
public class Mapper {
    private static final Logger LOG = LoggerFactory.getLogger(Mapper.class);

    /**
     * Special name that can never be used. Used as default for some fields to indicate default state.
     *
     * @morphia.internal
     */
    @MorphiaInternal
    public static final String IGNORED_FIELDNAME = ".";
    @MorphiaInternal
    public static final List<Class<? extends Annotation>> MAPPING_ANNOTATIONS = List.of(Entity.class, ExternalEntity.class);
    @MorphiaInternal
    public static final List<Class<? extends Annotation>> LIFECYCLE_ANNOTATIONS = List.of(PrePersist.class,
            PreLoad.class,
            PostPersist.class,
            PostLoad.class);

    /**
     * Set of classes that registered by this mapper
     */
    private final Map<String, EntityModel> mappedEntities = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Set<EntityModel>> mappedEntitiesByCollection = new ConcurrentHashMap<>();
    private final List<EntityListener<?>> listeners = new ArrayList<>();
    private final MorphiaConfig config;
    private final DiscriminatorLookup discriminatorLookup;
    private final ClassLoader contextClassLoader;

    /**
     * Creates a Mapper with the given options.
     *
     * @param config the config to use
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public Mapper(MorphiaConfig config) {
        this.config = config;
        contextClassLoader = Thread.currentThread().getContextClassLoader();
        discriminatorLookup = new DiscriminatorLookup();
    }

    /**
     * @param other the original to copy
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public Mapper(Mapper other) {
        config = other.config;
        contextClassLoader = other.contextClassLoader;
        discriminatorLookup = new DiscriminatorLookup();
        other.mappedEntities.values().forEach(entity -> {
            clone(entity);
        });
        listeners.addAll(other.listeners);
    }

    @Nullable
    private EntityModel clone(@Nullable EntityModel original) {
        if (original == null) {
            return null;
            //        } else if (isMapped(original.getType())) {
            //            return getEntityModel(original.getType());
        }
        //        EntityModel superClone = clone(original.superClass);
        //        if (superClone == null || superClone.getSubtype(original.getType()) == null) {
        EntityModel copy = register(new EntityModel(original), false);

        /*
         * Set<EntityModel> subtypes = original.subtypes.stream().map(subtype -> {
         * EntityModel clonedSubtype = clone(subtype);
         * clonedSubtype.superClass = copy;
         * return clonedSubtype;
         * }).collect(Collectors.toSet());
         * copy.subtypes.addAll(subtypes);
         */
        //        }

        return copy;
    }

    /**
     * Adds an {@link EntityListener}
     *
     * @param ei the interceptor to add
     * @deprecated use {@link dev.morphia.annotations.EntityListeners} to define any lifecycle event listeners
     */
    @Deprecated(forRemoval = true, since = "2.4.0")
    public void addInterceptor(EntityListener<?> ei) {
        listeners.add(ei);
    }

    /**
     * @hidden
     * @morphia.internal
     * @return the copied Mapper
     */
    @MorphiaInternal
    public Mapper copy() {
        return new Mapper(this);
    }

    /**
     * @param type the class
     * @return the id property model
     * @hidden
     * @morphia.internal
     * @since 2.2
     */
    @MorphiaInternal
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
     * @return the class reference. might be null
     */
    @SuppressWarnings("unchecked")
    @Nullable
    public <T> Class<T> getClass(Document document) {
        // see if there is a className value
        Class c = null;
        String discriminator = (String) document.get(getConfig().discriminatorKey());
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
    @MorphiaInternal
    public <T> Class<T> getClassFromCollection(String collection) {
        final List<EntityModel> classes = getClassesMappedToCollection(collection);
        if (classes.size() > 1) {
            LOG.warn(Sofia.moreThanOneMapper(collection,
                    classes.stream()
                            .map(c -> c.getType().getName())
                            .collect(Collectors.joining(", "))));
        }
        return (Class<T>) classes.get(0).getType();
    }

    /**
     * Finds all the types mapped to a named collection
     *
     * @param collection the collection to check
     * @return the mapped types
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public List<EntityModel> getClassesMappedToCollection(String collection) {
        final Set<EntityModel> entities = mappedEntitiesByCollection.get(collection);
        if (entities == null || entities.isEmpty()) {
            throw new MappingException(Sofia.collectionNotMapped(collection));
        }
        return new ArrayList<>(entities);
    }

    /**
     * @return the DiscriminatorLookup in use
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public DiscriminatorLookup getDiscriminatorLookup() {
        return discriminatorLookup;
    }

    /**
     * Gets the {@link EntityModel} for the object (type). If it isn't mapped, create a new class and cache it (without validating).
     *
     * @param type the type to process
     * @return the EntityModel for the object given
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public EntityModel getEntityModel(Class type) {
        final Class actual = MorphiaProxy.class.isAssignableFrom(type) ? type.getSuperclass() : type;
        if (actual == null && MorphiaProxy.class.equals(type)) {
            throw new NotMappableException(type);
        }
        EntityModel model = mappedEntities.get(actual.getName());

        if (model == null) {
            if (!isMappable(actual)) {
                throw new NotMappableException(type);
            }
            model = mapEntity(type);
        }

        return model;
    }

    @Nullable
    public EntityModel mapEntity(@Nullable Class type) {
        if (isMappable(type)) {
            EntityModel model = mappedEntities.get(type.getName());
            return model != null ? model : register(new EntityModel(this, type));
        }
        return null;
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
     * Gets list of {@link EntityListener}s
     *
     * @return the Interceptors
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public List<EntityListener<?>> getListeners() {
        return listeners;
    }

    /**
     * @return collection of EntityModels
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public List<EntityModel> getMappedEntities() {
        return new ArrayList<>(mappedEntities.values());
    }

    /**
     * @return the options used by this Mapper
     */
    public MorphiaConfig getConfig() {
        return config;
    }

    /**
     * Gets the write concern for entity or returns the default write concern for this datastore
     *
     * @param clazz the class to use when looking up the WriteConcern
     * @return the write concern for the type
     * @hidden
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
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public boolean hasListeners() {
        return !listeners.isEmpty();
    }

    /**
     * Checks if a type is mappable or not
     *
     * @param type the class to check
     * @param <T>  the type
     * @return true if the type is mappable
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public <T> boolean isMappable(@Nullable Class<T> type) {
        final Class actual = MorphiaProxy.class.isAssignableFrom(type) ? type.getSuperclass() : type;
        return actual != null && hasAnnotation(actual, MAPPING_ANNOTATIONS);
    }

    /**
     * Checks to see if a Class has been mapped.
     *
     * @param c the Class to check
     * @return true if the Class has been mapped
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public boolean isMapped(Class c) {
        return mappedEntities.containsKey(c.getName());
    }

    /**
     * Maps a set of classes
     *
     * @param entityClasses the classes to map
     * @return the EntityModel references
     * @hidden
     */
    public List<EntityModel> map(Class... entityClasses) {
        return map(List.of(entityClasses));
    }

    /**
     * Maps a set of classes
     *
     * @hidden
     * @param classes the classes to map
     * @return the list of mapped classes
     */
    public List<EntityModel> map(List<Class<?>> classes) {
        Sofia.logConfiguredOperation("Mapper#map");
        for (Class type : classes) {
            if (!isMappable(type)) {
                throw new MappingException(Sofia.mappingAnnotationNeeded(type.getName()));
            }
        }
        return classes.stream()
                .map(this::getEntityModel)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * Tries to map all classes in the package specified.
     *
     * @param packageName the name of the package to process
     * @hidden
     * @since 2.4
     */
    public synchronized void map(String packageName) {
        try {
            List<Class> classes = getClasses(contextClassLoader, packageName);
            classes
                    .forEach(type -> {
                        mapEntity(type);
                    });
        } catch (ClassNotFoundException e) {
            throw new MappingException("Could not get map classes from package " + packageName, e);
        }
    }

    /**
     * Tries to map all classes in the package specified.
     *
     * @hidden
     * @param packageName the name of the package to process
     */
    public synchronized void mapPackage(String packageName) {
        Sofia.logConfiguredOperation("Mapper#mapPackage");
        try {
            getClasses(contextClassLoader, packageName)
                    .forEach(type -> {
                        try {
                            getEntityModel(type);
                        } catch (NotMappableException ignored) {
                        }
                    });
        } catch (ClassNotFoundException e) {
            throw new MappingException("Could not get map classes from package " + packageName, e);
        }
    }

    /**
     * Updates a query with any discriminators from subtypes if polymorphic queries are enabled
     *
     * @param model the query model
     * @param query the query document
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public void updateQueryWithDiscriminators(EntityModel model, Document query) {
        Entity annotation = model.getEntityAnnotation();
        if (annotation != null && annotation.useDiscriminator()
                && !query.containsKey("_id")
                && !query.containsKey(model.discriminatorKey())) {
            List<String> values = new ArrayList<>();
            values.add(model.discriminator());
            if (config.enablePolymorphicQueries()) {
                for (EntityModel subtype : model.getSubtypes()) {
                    values.add(subtype.discriminator());
                }
            }
            query.put(model.discriminatorKey(),
                    new Document("$in", values));
        }
    }

    /**
     * @param entityModel the model to register
     * @return the model
     * @hidden
     * @morphia.internal
     * @since 2.3
     */
    @MorphiaInternal
    public EntityModel register(EntityModel entityModel) {
        return register(entityModel, true);
    }

    @MorphiaInternal
    private EntityModel register(EntityModel model, boolean validate) {
        var existing = mappedEntities.get(model.getType().getName());
        if (existing != null) {
            return existing;
        }
        mappedEntities.put(model.getType().getName(), model);
        if (validate && !model.isInterface()) {
            new MappingValidator()
                    .validate(this, model);
        }

        discriminatorLookup.addModel(model);
        mappedEntitiesByCollection.computeIfAbsent(model.collectionName(), s -> new CopyOnWriteArraySet<>())
                .add(model);

        mappedEntities.values().forEach(mapped -> {
            if (isParent(mapped, model)) {
                mapped.addSubtype(model);
            } else if (isParent(model, mapped)) {
                model.addSubtype(mapped);
            }
        });
        return model;
    }

    private static boolean isParent(EntityModel parent, EntityModel child) {
        Class<?> parentType = parent.getType();
        return parentType.equals(child.getType().getSuperclass())
                || parentType.isInterface()
                        && asList(child.getType().getInterfaces()).contains(parentType);
    }

    private List<Class> getClasses(ClassLoader loader, String packageName)
            throws ClassNotFoundException {
        final Set<Class> classes = new HashSet<>();

        ClassGraph classGraph = new ClassGraph()
                .addClassLoader(loader)
                .enableAllInfo();
        if (packageName.endsWith(".*")) {
            String base = packageName.substring(0, packageName.length() - 2);
            if (!base.isEmpty()) {
                classGraph.acceptPackages(base);
            }
            classGraph.acceptPackages(packageName);
        } else {
            classGraph.acceptPackagesNonRecursive(packageName);
        }

        try (ScanResult scanResult = classGraph.scan()) {
            for (ClassInfo classInfo : scanResult.getAllClasses()) {
                try {
                    classes.add(Class.forName(classInfo.getName(), true, loader));
                } catch (Throwable ignored) {
                }
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

}
