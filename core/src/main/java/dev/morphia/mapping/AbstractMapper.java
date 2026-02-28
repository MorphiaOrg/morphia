package dev.morphia.mapping;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;

import com.mongodb.WriteConcern;
import com.mongodb.lang.Nullable;

import dev.morphia.EntityListener;
import dev.morphia.annotations.Entity;
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
    protected final List<EntityListener<?>> listeners = new ArrayList<>();
    protected final MorphiaConfig config;
    protected final DiscriminatorLookup discriminatorLookup;
    protected final ClassLoader contextClassLoader;

    /**
     * Creates an AbstractMapper with the given config.
     *
     * @param config the config to use
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    protected AbstractMapper(MorphiaConfig config) {
        this.config = config;
        this.contextClassLoader = Thread.currentThread().getContextClassLoader();
        this.discriminatorLookup = new DiscriminatorLookup();
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
        this.config = other.config;
        this.contextClassLoader = other.contextClassLoader;
        this.discriminatorLookup = new DiscriminatorLookup();
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

    @Override
    public synchronized void map(String packageName) {
        try {
            List<Class> classes = getClasses(contextClassLoader, packageName);
            classes.forEach(type -> mapEntity(type));
        } catch (ClassNotFoundException e) {
            throw new MappingException("Could not get map classes from package " + packageName, e);
        }
    }

    @Override
    public synchronized void mapPackage(String packageName) {
        Sofia.logConfiguredOperation("Mapper#mapPackage");
        try {
            getClasses(contextClassLoader, packageName)
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

    @MorphiaInternal
    protected EntityModel register(EntityModel model, boolean validate) {
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
