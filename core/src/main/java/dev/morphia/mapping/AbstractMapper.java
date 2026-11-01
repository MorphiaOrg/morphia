package dev.morphia.mapping;

import java.lang.annotation.Annotation;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.ServiceLoader;
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
import dev.morphia.mapping.conventions.MorphiaConvention;
import dev.morphia.mapping.lifecycle.EntityListenerAdapter;
import dev.morphia.mapping.lifecycle.GlobalListenerAdapter;
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
    // Per collection, the registered models no other model mapped to that collection is a supertype of, in registration
    // order. Normally one root; several only if unrelated classes share a collection. Replaced whole under the mapper's
    // lock, so lock-free readers see a consistent list.
    private final Map<String, List<EntityModel>> rootsByCollection = new ConcurrentHashMap<>();
    // Collections already warned about for having unrelated roots, so the warning is logged once per collection.
    private final Set<String> warnedCollections = ConcurrentHashMap.newKeySet();
    // Models registered before their direct superclass/interface(s) are themselves registered,
    // keyed by that not-yet-mapped parent's class name, so the relationship can be linked once
    // the parent does show up — without rescanning every already-registered model on every new
    // registration (see register()/linkHierarchy()).
    private final Map<String, List<EntityModel>> pendingSubtypesByParentName = new ConcurrentHashMap<>();
    // Models whose registration is in progress (being validated), guarded by the mapper's lock. Only the registering
    // thread can see them, so its recursive lookups resolve, while other threads wait on the lock for the outcome.
    private final Map<String, EntityModel> registering = new HashMap<>();
    protected final List<EntityListener<?>> listeners = new ArrayList<>();
    // from MorphiaConfig.listeners(), fixed once the mapper exists (a copy may adopt its original's while it's built)
    private List<EntityListener<?>> globalListeners;
    protected final MorphiaConfig config;
    protected final DiscriminatorLookup discriminatorLookup;
    protected final ClassLoader classLoader;
    // stateless, so one serves every registration
    private final MappingValidator validator = new MappingValidator();
    // loaded on first use; see getConventions()
    private volatile List<MorphiaConvention> conventions;
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
        this.globalListeners = loadGlobalListeners(config, classLoader);
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
        this.globalListeners = loadGlobalListeners(config, other.classLoader);
        shareGlobalListeners(other);
    }

    /**
     * For a copy, while it's being built: uses the original's global listener instances when the config names the same
     * classes, so the copy shares any state they keep.
     *
     * @param other the original
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    protected final void shareGlobalListeners(AbstractMapper other) {
        if (config.listeners().equals(other.config.listeners())) {
            globalListeners = other.globalListeners;
        }
    }

    @Nullable
    private EntityModel clone(@Nullable EntityModel original) {
        if (original == null) {
            return null;
        }
        return register(new EntityModel(original), false);
    }

    /**
     * Creates the configured global listeners. Each class is wrapped the way an {@code @EntityListeners} class is, so its
     * lifecycle methods are found by annotation and called with arguments matched by type. A class is looked up through
     * the mapper's loader first, then the thread's context loader.
     */
    private static List<EntityListener<?>> loadGlobalListeners(MorphiaConfig config, ClassLoader classLoader) {
        List<EntityListener<?>> loaded = new ArrayList<>();
        for (String name : config.listeners().orElse(List.of())) {
            Class<?> type = loadListenerClass(name.trim(), classLoader);
            EntityListenerAdapter adapter = new GlobalListenerAdapter(type);
            if (LIFECYCLE_ANNOTATIONS.stream().noneMatch(adapter::hasAnnotation)) {
                throw new MappingException(Sofia.noLifecycleMethods(type.getName()));
            }
            loaded.add(adapter);
        }
        return List.copyOf(loaded);
    }

    private static Class<?> loadListenerClass(String name, ClassLoader classLoader) {
        try {
            return Class.forName(name, true, classLoader);
        } catch (ClassNotFoundException e) {
            ClassLoader context = Thread.currentThread().getContextClassLoader();
            if (context != null && context != classLoader) {
                try {
                    return Class.forName(name, true, context);
                } catch (ClassNotFoundException ignored) {
                    // report the first failure
                }
            }
            throw new MappingException(Sofia.listenerClassNotFound(name), e);
        }
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
        List<EntityModel> roots = rootsByCollection.get(collection);
        if (roots == null || roots.isEmpty()) {
            throw new MappingException(Sofia.collectionNotMapped(collection));
        }
        if (roots.size() > 1 && warnedCollections.add(collection)) {
            LOG.warn(Sofia.moreThanOneMapper(collection,
                    roots.stream()
                            .map(c -> c.getType().getName())
                            .collect(Collectors.joining(", "))));
        }
        return (Class<T>) roots.get(0).getType();
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

    /**
     * Loads the {@link MorphiaConvention}s once for this mapper, with its class loader, falling back to the thread's
     * context class loader if that finds none.
     */
    @Override
    @MorphiaInternal
    public List<MorphiaConvention> getConventions() {
        List<MorphiaConvention> loaded = conventions;
        if (loaded == null) {
            // under the mapper's lock, so concurrent first use discovers (and instantiates) them once
            synchronized (this) {
                loaded = conventions;
                if (loaded == null) {
                    loaded = loadConventions(classLoader);
                    ClassLoader context = Thread.currentThread().getContextClassLoader();
                    if (loaded.isEmpty() && context != null && context != classLoader) {
                        loaded = loadConventions(context);
                    }
                    conventions = loaded;
                }
            }
        }
        return loaded;
    }

    private static List<MorphiaConvention> loadConventions(ClassLoader loader) {
        List<MorphiaConvention> loaded = new ArrayList<>();
        ServiceLoader.load(MorphiaConvention.class, loader).forEach(loaded::add);
        return List.copyOf(loaded);
    }

    @Override
    @MorphiaInternal
    public List<EntityListener<?>> getListeners() {
        return listeners;
    }

    @Override
    @MorphiaInternal
    public List<EntityListener<?>> getGlobalListeners() {
        return globalListeners;
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
        return !globalListeners.isEmpty() || !listeners.isEmpty();
    }

    @Override
    @MorphiaInternal
    public <T> boolean isMappable(@Nullable Class<T> type) {
        if (type == null) {
            return false;
        }
        return MAPPABLE.get(type);
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
        map(packageName);
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
                validator.validate(this, model);
            }
            discriminatorLookup.addModel(model);
        } finally {
            registering.remove(name);
        }

        linkHierarchy(model);
        mappedEntitiesByCollection.computeIfAbsent(model.collectionName(), s -> new CopyOnWriteArraySet<>())
                .add(model);
        updateRoots(model);
        mappedEntities.put(name, model);
        return model;
    }

    /**
     * Updates the roots of the model's collection: the model replaces any roots it's a supertype of, keeping the first
     * one's position, and becomes a new root if no root is a supertype of it.
     */
    private void updateRoots(EntityModel model) {
        Class<?> type = model.getType();
        List<EntityModel> roots = rootsByCollection.getOrDefault(model.collectionName(), List.of());
        if (roots.stream().anyMatch(root -> root.getType().isAssignableFrom(type))) {
            return;
        }
        List<EntityModel> updated = new ArrayList<>();
        boolean added = false;
        for (EntityModel root : roots) {
            if (type.isAssignableFrom(root.getType())) {
                if (!added) {
                    updated.add(model);
                    added = true;
                }
            } else {
                updated.add(root);
            }
        }
        if (!added) {
            updated.add(model);
        }
        rootsByCollection.put(model.collectionName(), List.copyOf(updated));
    }

    /**
     * Links a newly registered model to its nearest registered superclass and its interfaces (including interfaces it
     * inherits), and links any registered models that were waiting on this one as a parent. A parent that isn't
     * registered yet is recorded as pending, unless it's a JDK type such as {@code Object} or {@code Serializable}, which
     * can never be mapped. The parent's own annotations don't decide this: an {@code @ExternalEntity} stand-in can map a
     * parent that has none. Lookups are name-keyed in both directions, so registration doesn't rescan every registered
     * model.
     */
    private void linkHierarchy(EntityModel model) {
        Class<?> type = model.getType();

        // up the superclass chain to the nearest registered class, so a subtype below a class that is never mapped is
        // still a subtype of the classes above it; unregistered classes on the way record it as pending
        for (Class<?> ancestor = type.getSuperclass(); ancestor != null; ancestor = ancestor.getSuperclass()) {
            if (link(ancestor, model)) {
                break;
            }
        }
        Set<Class<?>> interfaces = new LinkedHashSet<>();
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            addInterfaces(current, interfaces);
        }
        for (Class<?> iface : interfaces) {
            link(iface, model);
        }

        List<EntityModel> waiting = pendingSubtypesByParentName.remove(type.getName());
        if (waiting != null) {
            waiting.forEach(model::addSubtype);
        }
    }

    /**
     * Links a model under a parent type if it's registered, or records it as pending under that type.
     *
     * @return true if the parent is registered
     */
    private boolean link(Class<?> parentType, EntityModel model) {
        EntityModel parent = mappedEntities.get(parentType.getName());
        if (parent != null) {
            parent.addSubtype(model);
            return true;
        }
        if (parentType.getClassLoader() != null) {
            pendingSubtypesByParentName.computeIfAbsent(parentType.getName(), k -> new CopyOnWriteArrayList<>())
                    .add(model);
        }
        return false;
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
        if (packageName.equals(".*")) {
            Sofia.logScanningEntireClasspath();
        }
        // ClassGraph lists classes by name, so a LinkedHashSet keeps the mapping order, and any error, the same each run
        final Set<Class> classes = new LinkedHashSet<>();

        ClassGraph classGraph = new ClassGraph()
                .enableClasspath()
                .enableNonSystemModules()
                .enableClassLoaders(loader)
                .enableClassInfo()
                .enableAnnotationInfo()
                // superclasses and interfaces outside the accepted packages can carry the mapping annotation
                .enableExternalClasses()
                .ignoreClassVisibility();
        if (packageName.endsWith(".*")) {
            String base = packageName.substring(0, packageName.length() - 2);
            // acceptPackages is recursive; a bare ".*" accepts nothing so every package is scanned
            if (!base.isEmpty()) {
                classGraph.acceptPackages(base);
            }
        } else {
            classGraph.acceptPackagesNonRecursive(packageName);
        }

        try (ScanResult scanResult = classGraph.scan()) {
            for (ClassInfo classInfo : scanResult.getAllClasses()) {
                if (classInfo.isExternalClass() || !mayBeMappable(classInfo)) {
                    continue;
                }
                try {
                    // initialize=false: mapping a class doesn't need its static initializer to have run
                    classes.add(Class.forName(classInfo.getName(), false, loader));
                } catch (Throwable ignored) {
                }
            }
        }
        return new ArrayList<>(classes);
    }

    /**
     * Whether a type can be mapped: it, a superclass, or an interface either of them implements (directly or by
     * extension) carries a mapping annotation. A proxy stands for its superclass. Annotations don't change, so the answer
     * is worked out once per type.
     */
    private static final ClassValue<Boolean> MAPPABLE = new ClassValue<>() {
        @Override
        protected Boolean computeValue(Class<?> type) {
            Class<?> actual = MorphiaProxy.class.isAssignableFrom(type) ? type.getSuperclass() : type;
            return actual != null && hasMappingAnnotation(actual);
        }
    };

    private static boolean hasMappingAnnotation(Class<?> type) {
        // @Entity and @ExternalEntity are @Inherited, so this sees them on superclasses too
        if (annotated(type)) {
            return true;
        }
        // but annotations on interfaces aren't inherited, so visit every interface of the type and its superclasses
        Set<Class<?>> visited = new HashSet<>();
        Deque<Class<?>> pending = new ArrayDeque<>();
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            pending.addAll(Arrays.asList(current.getInterfaces()));
        }
        while (!pending.isEmpty()) {
            Class<?> iface = pending.removeFirst();
            if (visited.add(iface)) {
                if (annotated(iface)) {
                    return true;
                }
                pending.addAll(Arrays.asList(iface.getInterfaces()));
            }
        }
        return false;
    }

    /**
     * A pre-filter on the scanned class files, so only classes that might be mappable get loaded. It matches at least
     * every class {@link #hasMappingAnnotation(Class)} does: {@code hasAnnotation} covers annotations inherited from
     * superclasses, and the interfaces are checked separately since theirs aren't inherited. {@code isMappable} still
     * decides once the class is loaded.
     */
    private static boolean mayBeMappable(ClassInfo type) {
        if (annotated(type)) {
            return true;
        }
        for (ClassInfo iface : type.getAllSuperinterfaces()) {
            if (annotated(iface)) {
                return true;
            }
        }
        return false;
    }

    private static boolean annotated(ClassInfo type) {
        for (Class<? extends Annotation> annotation : MAPPING_ANNOTATIONS) {
            if (type.hasAnnotation(annotation.getName())) {
                return true;
            }
        }
        return false;
    }

    private static boolean annotated(Class<?> type) {
        for (Class<? extends Annotation> annotation : MAPPING_ANNOTATIONS) {
            if (type.isAnnotationPresent(annotation)) {
                return true;
            }
        }
        return false;
    }
}
