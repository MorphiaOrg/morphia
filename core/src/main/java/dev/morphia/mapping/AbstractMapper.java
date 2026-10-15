package dev.morphia.mapping;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;

import com.mongodb.WriteConcern;
import com.mongodb.lang.Nullable;

import dev.morphia.EntityListener;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.internal.MorphiaInternal;
import dev.morphia.config.MorphiaConfig;
import dev.morphia.mapping.codec.Conversions;
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

/**
 * Base class for Mapper implementations providing shared entity registration and lookup logic.
 *
 * @morphia.internal
 * @hidden
 */
@MorphiaInternal
@SuppressWarnings({ "unchecked", "rawtypes" })
public abstract class AbstractMapper implements Mapper {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractMapper.class);

    protected final Map<String, EntityModel> mappedEntities = new ConcurrentHashMap<>();
    protected final ConcurrentHashMap<String, Set<EntityModel>> mappedEntitiesByCollection = new ConcurrentHashMap<>();
    // Models registered before their direct superclass/interface(s) are themselves registered,
    // keyed by that not-yet-mapped parent's class name, so the relationship can be linked once
    // the parent does show up — without rescanning every already-registered model on every new
    // registration (see register()/linkHierarchy()).
    private final Map<String, List<EntityModel>> pendingSubtypesByParentName = new ConcurrentHashMap<>();
    // Models whose registration is in progress (being validated), guarded by the mapper's lock. Only the registering
    // thread can see them, so its recursive lookups resolve, while other threads wait on the lock for the outcome.
    private final Map<String, EntityModel> registering = new HashMap<>();
    protected final List<EntityListener<?>> listeners = new ArrayList<>();
    protected final MorphiaConfig config;
    protected final DiscriminatorLookup discriminatorLookup;
    protected final ClassLoader classLoader;
    private final Conversions conversions;

    /**
     * Creates an AbstractMapper with the given config.
     *
     * @param config the config to use
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    protected AbstractMapper(MorphiaConfig config) {
        this(config, Thread.currentThread().getContextClassLoader());
    }

    /**
     * Creates an AbstractMapper with the given config and classloader.
     *
     * @param config      the config to use
     * @param classLoader the classloader to use for class and resource resolution
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    protected AbstractMapper(MorphiaConfig config, ClassLoader classLoader) {
        this.config = config;
        this.classLoader = classLoader;
        this.conversions = new Conversions(classLoader);
        this.discriminatorLookup = new DiscriminatorLookup(classLoader);
    }

    /**
     * Copy constructor.
     *
     * @param other the original to copy
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    protected AbstractMapper(AbstractMapper other) {
        this(other, other.config);
    }

    /**
     * Copy constructor that reuses another mapper's already-mapped/validated entity
     * graph under a different config (e.g. a different database name), rather than
     * assuming the copy operates under the original's config.
     *
     * @param other  the original to clone the entity graph from
     * @param config the config the new mapper should report/operate under
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    protected AbstractMapper(AbstractMapper other, MorphiaConfig config) {
        this.config = config;
        this.classLoader = other.classLoader;
        this.conversions = other.conversions;
        this.discriminatorLookup = new DiscriminatorLookup(other.classLoader);
        other.mappedEntities.values().forEach(this::clone);
        this.listeners.addAll(other.listeners);
    }

    @Nullable
    private EntityModel clone(@Nullable EntityModel original) {
        if (original == null) {
            return null;
        }
        return register(new EntityModel(original), false);
    }

    @Override
    @Deprecated(forRemoval = true, since = "2.4.0")
    public void addInterceptor(EntityListener<?> ei) {
        listeners.add(ei);
    }

    @Override
    public PropertyModel findIdProperty(Class<?> type) {
        EntityModel entityModel = getEntityModel(type);
        PropertyModel idField = entityModel.getIdProperty();

        if (idField == null) {
            throw new MappingException(Sofia.idRequired(type.getName()));
        }
        return idField;
    }

    @Override
    @SuppressWarnings("unchecked")
    @Nullable
    public <T> Class<T> getClass(Document document) {
        Class c = null;
        String discriminator = (String) document.get(getConfig().discriminatorKey());
        if (discriminator != null) {
            c = getClass(discriminator);
        }
        return c;
    }

    @Override
    public Class getClass(String discriminator) {
        return discriminatorLookup.lookup(discriminator);
    }

    @Override
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

    @Override
    @MorphiaInternal
    public List<EntityModel> getClassesMappedToCollection(String collection) {
        final Set<EntityModel> entities = mappedEntitiesByCollection.get(collection);
        if (entities == null || entities.isEmpty()) {
            throw new MappingException(Sofia.collectionNotMapped(collection));
        }
        return new ArrayList<>(entities);
    }

    @Override
    @MorphiaInternal
    public DiscriminatorLookup getDiscriminatorLookup() {
        return discriminatorLookup;
    }

    /**
     * @return the Conversions instance used by this Mapper
     * @morphia.internal
     * @hidden
     */
    @Override
    @MorphiaInternal
    public Conversions getConversions() {
        return conversions;
    }

    @Override
    @MorphiaInternal
    public Optional<EntityModel> tryGetEntityModel(Class type) {
        final Class actual = MorphiaProxy.class.isAssignableFrom(type) ? type.getSuperclass() : type;
        if (actual == null && MorphiaProxy.class.equals(type)) {
            return Optional.empty();
        }
        EntityModel model = mappedEntities.get(actual.getName());

        if (model == null) {
            if (!isMappable(actual)) {
                return Optional.empty();
            }
            model = mapEntity(actual);
        }

        return Optional.of(model);
    }

    @Override
    @MorphiaInternal
    public EntityModel getEntityModel(Class type) {
        return tryGetEntityModel(type)
                .orElseThrow(() -> new NotMappableException(type));
    }

    @Override
    @Nullable
    public Object getId(@Nullable Object entity) {
        if (entity == null) {
            return null;
        }
        return tryGetEntityModel(entity.getClass())
                .map(EntityModel::getIdProperty)
                .map((idField) -> idField.getValue(entity))
                .orElse(null);
    }

    @Override
    @MorphiaInternal
    public List<EntityListener<?>> getListeners() {
        return listeners;
    }

    @Override
    @MorphiaInternal
    public List<EntityModel> getMappedEntities() {
        return new ArrayList<>(mappedEntities.values());
    }

    @Override
    public MorphiaConfig getConfig() {
        return config;
    }

    @Override
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

    @Override
    @MorphiaInternal
    public boolean hasListeners() {
        return !listeners.isEmpty();
    }

    @Override
    @MorphiaInternal
    public <T> boolean isMappable(@Nullable Class<T> type) {
        if (type == null) {
            return false;
        }
        final Class actual = MorphiaProxy.class.isAssignableFrom(type) ? type.getSuperclass() : type;
        return actual != null && hasAnnotation(actual, MAPPING_ANNOTATIONS);
    }

    @Override
    @MorphiaInternal
    public boolean isMapped(Class c) {
        return mappedEntities.containsKey(c.getName());
    }

    @Override
    public List<EntityModel> map(Class... entityClasses) {
        return map(List.of(entityClasses));
    }

    @Override
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
     * Maps a type, building and registering its model if it isn't mapped yet. Models are built under the mapper's lock
     * so concurrent first use of a type builds and registers it once; lookups of already-mapped types don't lock.
     *
     * @param type the class to map
     * @return the model, or null if the type isn't mappable
     */
    @Override
    @Nullable
    public final EntityModel mapEntity(@Nullable Class type) {
        if (!isMappable(type)) {
            return null;
        }
        EntityModel model = mappedEntities.get(type.getName());
        if (model != null) {
            return model;
        }
        synchronized (this) {
            model = mappedEntities.get(type.getName());
            if (model == null) {
                model = registering.get(type.getName());
            }
            return model != null ? model : register(buildModel(type));
        }
    }

    /**
     * Builds a new, unregistered model for a mappable type. Called by {@link #mapEntity(Class)} under the mapper's lock,
     * only for types that aren't mapped yet.
     *
     * @param type the type to model
     * @return the new model
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    protected abstract EntityModel buildModel(Class<?> type);

    @Override
    public synchronized void map(String packageName) {
        try {
            List<Class> classes = getClasses(classLoader, packageName);
            classes.forEach(type -> mapEntity(type));
        } catch (ClassNotFoundException e) {
            throw new MappingException("Could not get map classes from package " + packageName, e);
        }
    }

    @Override
    public synchronized void mapPackage(String packageName) {
        Sofia.logConfiguredOperation("Mapper#mapPackage");
        try {
            getClasses(classLoader, packageName)
                    .forEach(this::tryGetEntityModel);
        } catch (ClassNotFoundException e) {
            throw new MappingException("Could not get map classes from package " + packageName, e);
        }
    }

    @Override
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
            query.put(model.discriminatorKey(), new Document("$in", values));
        }
    }

    @Override
    @MorphiaInternal
    public EntityModel register(EntityModel entityModel) {
        return register(entityModel, true);
    }

    /**
     * Registers a model unless one is already registered for its type, in which case the existing model is returned.
     * Registration happens under the mapper's lock. The model is published to {@link #mappedEntities} only after it
     * passes validation and discriminator registration, so concurrent lookups never see a model that may yet be
     * rejected. While it's being validated, lookups from the registering thread (e.g. a reference back to the type)
     * resolve to it through {@link #mapEntity(Class)}. If validation or discriminator registration fails, the failure
     * propagates and nothing is registered, so a later lookup fails the same way. The model is linked into its hierarchy
     * only after it passes, and published only after it's linked.
     *
     * @param model    the model to register
     * @param validate true to validate the model before completing its registration
     * @return the registered model for the type, which is {@code model} unless another was registered first
     */
    @MorphiaInternal
    protected synchronized EntityModel register(EntityModel model, boolean validate) {
        String name = model.getType().getName();
        var existing = mappedEntities.get(name);
        if (existing == null) {
            existing = registering.get(name);
        }
        if (existing != null) {
            return existing;
        }
        registering.put(name, model);
        try {
            if (validate && !model.isInterface()) {
                new MappingValidator()
                        .validate(this, model);
            }
            discriminatorLookup.addModel(model);
        } finally {
            registering.remove(name);
        }

        linkHierarchy(model);
        mappedEntitiesByCollection.computeIfAbsent(model.collectionName(), s -> new CopyOnWriteArraySet<>())
                .add(model);
        mappedEntities.put(name, model);
        return model;
    }

    /**
     * Links a newly registered model to its registered superclass and interfaces (including interfaces it inherits), and
     * links any registered models that were waiting on this one as a parent. A parent that isn't registered yet is
     * recorded as pending, unless it can never be mapped (e.g. {@code Object}). Lookups are name-keyed in both
     * directions, so registration doesn't rescan every registered model.
     */
    private void linkHierarchy(EntityModel model) {
        Class<?> type = model.getType();

        Set<Class<?>> parents = new LinkedHashSet<>();
        if (type.getSuperclass() != null) {
            parents.add(type.getSuperclass());
        }
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            addInterfaces(current, parents);
        }

        for (Class<?> parentType : parents) {
            EntityModel parent = mappedEntities.get(parentType.getName());
            if (parent != null) {
                parent.addSubtype(model);
            } else if (isMappable(parentType)) {
                pendingSubtypesByParentName.computeIfAbsent(parentType.getName(), k -> new CopyOnWriteArrayList<>())
                        .add(model);
            }
        }

        List<EntityModel> waiting = pendingSubtypesByParentName.remove(type.getName());
        if (waiting != null) {
            waiting.forEach(model::addSubtype);
        }
    }

    private static void addInterfaces(Class<?> type, Set<Class<?>> interfaces) {
        for (Class<?> iface : type.getInterfaces()) {
            if (interfaces.add(iface)) {
                addInterfaces(iface, interfaces);
            }
        }
    }

    protected List<Class> getClasses(ClassLoader loader, String packageName)
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

    protected <T> boolean hasAnnotation(Class<T> clazz, List<Class<? extends Annotation>> annotations) {
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
