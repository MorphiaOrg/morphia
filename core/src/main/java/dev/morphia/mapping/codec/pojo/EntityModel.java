package dev.morphia.mapping.codec.pojo;

import java.lang.annotation.Annotation;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;

import com.mongodb.lang.NonNull;
import com.mongodb.lang.Nullable;

import dev.morphia.Datastore;
import dev.morphia.EntityListener;
import dev.morphia.MorphiaDatastore;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.EntityListeners;
import dev.morphia.annotations.ExternalEntity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.PostLoad;
import dev.morphia.annotations.PostPersist;
import dev.morphia.annotations.PreLoad;
import dev.morphia.annotations.PrePersist;
import dev.morphia.annotations.ShardKeys;
import dev.morphia.annotations.Version;
import dev.morphia.annotations.internal.MorphiaInternal;
import dev.morphia.mapping.InstanceCreatorFactory;
import dev.morphia.mapping.InstanceCreatorFactoryImpl;
import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.MappingException;
import dev.morphia.mapping.codec.MorphiaInstanceCreator;
import dev.morphia.mapping.conventions.ConfigureProperties;
import dev.morphia.mapping.conventions.FieldDiscovery;
import dev.morphia.mapping.conventions.MethodDiscovery;
import dev.morphia.mapping.conventions.MorphiaConvention;
import dev.morphia.mapping.conventions.MorphiaDefaultsConvention;
import dev.morphia.mapping.lifecycle.EntityListenerAdapter;
import dev.morphia.mapping.lifecycle.OnEntityListenerAdapter;
import dev.morphia.mapping.lifecycle.UntypedEntityListenerAdapter;
import dev.morphia.sofia.Sofia;

import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static dev.morphia.mapping.PropertyDiscovery.FIELDS;
import static java.lang.String.format;
import static java.util.Arrays.stream;
import static java.util.Collections.emptyList;
import static java.util.List.of;

/**
 * A model of metadata about a type
 *
 * @hidden
 * @morphia.internal
 * @since 2.0
 */
@MorphiaInternal
@SuppressWarnings({ "unchecked", "deprecation" })
public class EntityModel {
    private static final Logger LOG = LoggerFactory.getLogger(EntityModel.class);

    private final Map<Class<? extends Annotation>, Annotation> annotations = new HashMap<>();
    private final Map<String, PropertyModel> propertyModelsByName = new UniqueMap();
    final Map<String, PropertyModel> propertyModelsByMappedName = new UniqueMap();

    private Map<String, Map<String, Type>> parameterization;

    private List<PropertyModel> shardKeys;

    private InstanceCreatorFactory creatorFactory;
    private boolean discriminatorEnabled;
    private String discriminatorKey;
    private String discriminator;
    private Class<?> type;
    private String collectionName;
    private final Set<EntityModel> subtypes = new CopyOnWriteArraySet<>();
    public EntityModel superClass;
    private PropertyModel idProperty;
    private PropertyModel versionProperty;
    private final List<EntityListener<?>> listeners = new ArrayList<>();
    private final Set<Class<?>> classes = new LinkedHashSet<>();

    public EntityModel(Class<?> type) {
        if (!Modifier.isStatic(type.getModifiers()) && type.isMemberClass()) {
            throw new MappingException(Sofia.noInnerClasses(type.getName()));
        }
        this.type = type;
        creatorFactory = new InstanceCreatorFactoryImpl(this);
    }

    public EntityModel(Mapper mapper, Class<?> type) {
        this(type);

        new MappingUtil(mapper);

        ExternalEntity externalEntity = getAnnotation(ExternalEntity.class);
        if (externalEntity != null) {
            this.type = externalEntity.target();
        }
    }

    public EntityModel(EntityModel other) {
        type = other.type;

        discriminatorEnabled = other.discriminatorEnabled;
        discriminatorKey = other.discriminatorKey;
        discriminator = other.discriminator;

        this.annotations.putAll(other.annotations);
        other.propertyModelsByName.values()
                .forEach(otherProperty -> {
                    PropertyModel model = new PropertyModel(this, otherProperty);
                    addProperty(model);
                    model.alternateNames(model.getLoadNames().toArray(new String[0]));
                });

        ShardKeys shardKeys = getAnnotation(ShardKeys.class);
        if (shardKeys != null) {
            this.shardKeys = stream(shardKeys.value())
                    .map(k -> getProperty(k.value()))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        } else {
            this.shardKeys = emptyList();
        }

        this.collectionName = other.collectionName;
        creatorFactory = new InstanceCreatorFactoryImpl(this);

        PropertyModel otherId = other.idProperty;
        idProperty = otherId != null ? getProperty(otherId.getName()) : null;

        PropertyModel otherVersion = other.versionProperty;
        versionProperty = otherVersion != null ? getProperty(otherVersion.getName()) : null;

        final EntityListeners entityLisAnn = getAnnotation(EntityListeners.class);
        if (entityLisAnn != null) {
            for (Class<?> aClass : entityLisAnn.value()) {
                if (EntityListener.class.isAssignableFrom(aClass)) {
                    listeners.add(new EntityListenerAdapter(aClass));
                } else {
                    listeners.add(new UntypedEntityListenerAdapter(aClass));
                }
            }
        }

        OnEntityListenerAdapter adapter = OnEntityListenerAdapter.listen(getType());
        if (adapter != null) {
            listeners.add(adapter);
        }
    }

    public boolean addProperty(PropertyModel property) {
        var added = propertyModelsByName.putIfAbsent(property.getName(), property) == null;
        added &= propertyModelsByMappedName.put(property.getMappedName(), property) == null;

        if (added) {
            if (property.hasAnnotation(Id.class)) {
                idProperty = property;
            } else if (property.hasAnnotation(Version.class)) {
                versionProperty = property;
            }
        }
        return added;
    }

    public void annotation(Annotation annotation) {
        annotations.putIfAbsent(annotation.annotationType(), annotation);
    }

    /**
     * Invokes any lifecycle methods
     *
     * @param event     the event to run
     * @param entity    the entity to use
     * @param document  the document used in persistence
     * @param datastore the Datastore to use
     */
    @SuppressWarnings("rawtypes")
    public void callLifecycleMethods(Class<? extends Annotation> event, Object entity, Document document,
            MorphiaDatastore datastore) {
        listeners.forEach((listener) -> {
            invokeLifecycleEvent(event, entity, document, datastore, listener);
        });
        datastore.getMapper().getListeners().forEach((listener) -> {
            LOG.debug(Sofia.callingInterceptorMethod(event.getSimpleName(), listener));
            invokeLifecycleEvent(event, entity, document, datastore, listener);
        });
    }

    public Set<Class<?>> classHierarchy() {
        return classes;
    }

    public void discriminator(String discriminator) {
        this.discriminator = discriminator;
    }

    public EntityModel discriminatorKey(String discriminatorKey) {
        this.discriminatorKey = discriminatorKey;
        return this;
    }

    public EntityModel discriminatorEnabled(boolean discriminatorEnabled) {
        this.discriminatorEnabled = discriminatorEnabled;
        return this;
    }

    /**
     * @param clazz the annotation class
     * @param <A>   the annotation type
     * @return the annotation instance or null if not found
     */
    @Nullable
    public <A extends Annotation> A getAnnotation(Class<A> clazz) {
        return (A) annotations.get(clazz);
    }

    /**
     * @return the mapped collection name for the type
     */
    @NonNull
    public String collectionName() {
        if (collectionName == null) {
            throw new MappingException(Sofia.noMappedCollection(getType().getName()));
        }
        return collectionName;
    }

    public EntityModel collectionName(String collectionName) {
        this.collectionName = collectionName;
        return this;
    }

    /**
     * @return the shard keys
     */
    public List<PropertyModel> getShardKeys() {
        if (shardKeys == null) {
            ShardKeys ann = getAnnotation(ShardKeys.class);
            if (ann != null) {
                shardKeys = stream(ann.value())
                        .map(k -> getProperty(k.value()))
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
            } else {
                shardKeys = emptyList();
            }
        }
        return shardKeys;
    }

    /**
     * @return the discriminator
     */
    public String discriminator() {
        return discriminator;
    }

    /**
     * @return the discriminator key
     */
    public String discriminatorKey() {
        return discriminatorKey;
    }

    /**
     * @return the entityAn
     */
    @Nullable
    public Entity getEntityAnnotation() {
        return getAnnotation(Entity.class);
    }

    /**
     * @return the model for the id property
     */
    @Nullable
    public PropertyModel getIdProperty() {
        return idProperty;
    }

    @Nullable
    public void setIdProperty(PropertyModel model) {
        idProperty = model;
    }

    /**
     * @return a new InstanceCreator instance for the ClassModel
     */
    public MorphiaInstanceCreator getInstanceCreator() {
        return creatorFactory.create();
    }

    /**
     * @return the name of this model
     */
    public String getName() {
        return type.getSimpleName();
    }

    /**
     * Returns all the properties on this model annotated by the given type
     *
     * @param type the annotation type
     * @return the list of properties
     */
    public List<PropertyModel> getProperties(Class<? extends Annotation> type) {
        return propertyModelsByName.values().stream()
                .filter(model -> model.hasAnnotation(type))
                .collect(Collectors.toList());
    }

    /**
     * Returns all the properties on this model
     *
     * @return the list of properties
     */
    public List<PropertyModel> getProperties() {
        return new ArrayList<>(propertyModelsByName.values());
    }

    /**
     * @param name the property name
     * @return the named PropertyModel or null if it does not exist
     */
    @Nullable
    public PropertyModel getProperty(@Nullable String name) {
        return name != null ? propertyModelsByMappedName.getOrDefault(name, propertyModelsByName.get(name)) : null;
    }

    /*
     * @Nullable
     * public EntityModel getSubtype(Class<?> type) {
     * return subtypes.stream().filter(subtype -> subtype.type.equals(type))
     * .findFirst()
     * .orElse(null);
     * }
     */

    /**
     * Get the subtypes of this model
     *
     * @return the subtypes
     */
    public Set<EntityModel> getSubtypes() {
        Set<EntityModel> set = new HashSet<>(subtypes);
        set.addAll(subtypes.stream()
                .flatMap(type -> type.getSubtypes().stream())
                .collect(Collectors.toSet()));
        return set;
    }

    public void addSubtype(EntityModel subtype) {
        subtypes.add(subtype);
        subtype.superClass = this;
    }

    /**
     * @return the model of the superclass of this type or null
     */
    @Nullable
    public EntityModel getSuperClass() {
        return superClass;
    }

    /**
     * @return the type of this model
     */
    public Class<?> getType() {
        return type;
    }

    public void setType(Class<?> type) {
        this.type = type;
    }

    public TypeData<?> getTypeData(Class<?> type, TypeData<?> suggested, Type genericType) {

        if (genericType instanceof TypeVariable) {
            Map<String, Type> map = parameterization.get(type.getName());
            if (map != null) {
                Type mapped = map.get(((TypeVariable<?>) genericType).getName());
                if (mapped != null) {
                    suggested = TypeData.get(mapped);
                }
            }
        }
        return suggested;
    }

    /**
     * @return the version property for the class
     */
    @Nullable
    public PropertyModel getVersionProperty() {
        return versionProperty;
    }

    public void setVersionProperty(PropertyModel model) {
        versionProperty = model;
    }

    /**
     * @param type the lifecycle event type
     * @return true if that even has been configured
     */
    public boolean hasLifecycle(Class<? extends Annotation> type) {
        return listeners.stream()
                .anyMatch(listener -> listener.hasAnnotation(type));
    }

    @Override
    public int hashCode() {
        return Objects.hash(annotations, propertyModelsByName, propertyModelsByMappedName, creatorFactory,
                discriminatorEnabled, discriminatorKey(), discriminator(), getType(), collectionName(), listeners);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof EntityModel)) {
            return false;
        }
        final EntityModel that = (EntityModel) o;
        return discriminatorEnabled == that.discriminatorEnabled
                && Objects.equals(annotations, that.annotations)
                && Objects.equals(propertyModelsByName, that.propertyModelsByName)
                && Objects.equals(propertyModelsByMappedName, that.propertyModelsByMappedName)
                && Objects.equals(creatorFactory, that.creatorFactory)
                && Objects.equals(discriminatorKey(), that.discriminatorKey())
                && Objects.equals(discriminator(), that.discriminator())
                && Objects.equals(getType(), that.getType())
                && Objects.equals(collectionName(), that.collectionName())
                && Objects.equals(listeners, that.listeners);
    }

    @Override
    public String toString() {
        String properties = propertyModelsByName.values().stream()
                .map(PropertyModel::toString)
                .collect(Collectors.joining(", "));
        return format("%s<%s> { %s } ", EntityModel.class.getSimpleName(), type.getSimpleName(), properties);
    }

    /**
     * This is an internal method subject to change without notice.
     *
     * @return true if the EntityModel is abstract
     * @since 1.3
     */
    public boolean isAbstract() {
        return Modifier.isAbstract(getType().getModifiers());
    }

    /**
     * @return true if the EntityModel is an interface
     */
    public boolean isInterface() {
        return getType().isInterface();
    }

    /**
     * @return true if the discriminator should be used
     */
    public boolean useDiscriminator() {
        return discriminatorEnabled;
    }

    @SuppressWarnings("rawtypes")
    private void invokeLifecycleEvent(Class<? extends Annotation> event,
            Object entity,
            Document document,
            Datastore datastore,
            EntityListener ei) {
        if (event.equals(PreLoad.class)) {
            ei.preLoad(entity, document, datastore);
        } else if (event.equals(PostLoad.class)) {
            ei.postLoad(entity, document, datastore);
        } else if (event.equals(PrePersist.class)) {
            ei.prePersist(entity, document, datastore);
        } else if (event.equals(PostPersist.class)) {
            ei.postPersist(entity, document, datastore);
        }
    }

    private class UniqueMap extends LinkedHashMap<String, PropertyModel> {
        @Override
        public PropertyModel put(String name, PropertyModel value) {
            PropertyModel propertyModel = putIfAbsent(name, value);
            if (propertyModel != null) {
                throw new MappingException(Sofia.duplicatedMappedName(type.getCanonicalName(), name));
            }
            return propertyModel;
        }
    }

    private class MappingUtil {

        private final Mapper mapper;

        public MappingUtil(Mapper mapper) {
            this.mapper = mapper;
            buildHierarchy();
            propagateTypes();

            List<MorphiaConvention> conventions = new ArrayList<>(of(
                    new MorphiaDefaultsConvention(),
                    mapper.getConfig().propertyDiscovery() == FIELDS ? new FieldDiscovery() : new MethodDiscovery(),
                    new ConfigureProperties()));

            ServiceLoader.load(MorphiaConvention.class)
                    .forEach(conventions::add);

            for (MorphiaConvention convention : conventions) {
                convention.apply(mapper, EntityModel.this);
            }

            if (discriminatorEnabled) {
                Objects.requireNonNull(discriminatorKey, Sofia.notNull("discriminatorKey"));
                Objects.requireNonNull(discriminator, Sofia.notNull("discriminator"));
            }

            build();
        }

        private void build() {

            collectionName(getCollectionName());

            //            if (superClass != null) {
            //                superClass.addSubtype(EntityModel.this);
            //            }

            final EntityListeners entityLisAnn = getAnnotation(EntityListeners.class);
            if (entityLisAnn != null) {
                for (Class<?> aClass : entityLisAnn.value()) {
                    if (EntityListener.class.isAssignableFrom(aClass)) {
                        listeners.add(new EntityListenerAdapter(aClass));
                    } else {
                        listeners.add(new UntypedEntityListenerAdapter(aClass));
                    }
                }
            }

            OnEntityListenerAdapter adapter = OnEntityListenerAdapter.listen(getType());
            if (adapter != null) {
                listeners.add(adapter);
            }

        }

        private void buildHierarchy() {
            for (Annotation annotation : type.getAnnotations()) {
                annotation(annotation);
            }
            Set<Class<?>> interfaces = new LinkedHashSet<>();
            interfaces.addAll(findInterfaces(type));

            classes.addAll(findParentClasses(type.getSuperclass()));
            classes.forEach(c -> interfaces.addAll(findInterfaces(c)));

            interfaces.stream()
                    .map(mapper::mapEntity)
                    .filter(Objects::nonNull)
                    .forEach(i -> i.addSubtype(EntityModel.this));

        }

        private List<? extends Class<?>> findInterfaces(Class<?> type) {
            List<Class<?>> list = new ArrayList<>();
            List<Class<?>> interfaces = Arrays.asList(type.getInterfaces());
            for (Annotation annotation : type.getAnnotations()) {
                annotation(annotation);
            }
            list.addAll(interfaces);
            list.addAll(interfaces.stream()
                    .flatMap(i -> findInterfaces(i).stream())
                    .collect(Collectors.toList()));

            return list;
        }

        private Set<Class<?>> findParentClasses(Class<?> type) {
            Set<Class<?>> classes = new LinkedHashSet<>();
            while (type != null && !type.isEnum() && !type.equals(Object.class)) {
                classes.add(type);
                for (Annotation annotation : type.getAnnotations()) {
                    annotation(annotation);
                }
                type = type.getSuperclass();
            }
            return classes;
        }

        private Map<String, Map<String, Type>> findParameterization(Class<?> type) {
            if (type.getSuperclass() == null) {
                return new LinkedHashMap<>();
            }
            Map<String, Map<String, Type>> parentMap = findParameterization(type.getSuperclass());
            Map<String, Type> typeMap = mapArguments(type.getSuperclass(), type.getGenericSuperclass());

            parentMap.put(type.getSuperclass().getName(), typeMap);
            return parentMap;
        }

        private void propagateTypes() {
            parameterization = findParameterization(type);

            List<Map<String, Type>> parameters = new ArrayList<>(parameterization.values());

            for (int index = 0; index < parameters.size(); index++) {
                Map<String, Type> current = parameters.get(index);
                if (index + 1 < parameters.size()) {
                    for (Entry<String, Type> entry : current.entrySet()) {
                        int peek = index + 1;
                        while (entry.getValue() instanceof TypeVariable && peek < parameters.size()) {
                            TypeVariable<?> typeVariable = (TypeVariable<?>) entry.getValue();
                            Map<String, Type> next = parameters.get(peek++);
                            entry.setValue(next.get(typeVariable.getName()));
                        }
                    }
                }
            }
        }

        private Map<String, Type> mapArguments(@Nullable Class<?> type, Type typeSignature) {
            Map<String, Type> map = new HashMap<>();
            if (type != null && typeSignature instanceof ParameterizedType) {
                TypeVariable<?>[] typeParameters = type.getTypeParameters();
                if (typeParameters.length != 0) {
                    Type[] arguments = ((ParameterizedType) typeSignature).getActualTypeArguments();
                    for (int i = 0; i < typeParameters.length; i++) {
                        TypeVariable<?> typeParameter = typeParameters[i];
                        map.put(typeParameter.getName(), arguments[i]);
                    }
                }
            }
            return map;
        }

        protected String getCollectionName() {
            Entity entityAn = getAnnotation(Entity.class);
            return entityAn != null && !entityAn.value().equals(Mapper.IGNORED_FIELDNAME)
                    ? entityAn.value()
                    : mapper.getConfig().collectionNaming().apply(type.getSimpleName());
        }
    }

}
