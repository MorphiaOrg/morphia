package dev.morphia.mapping.codec.pojo;

import com.mongodb.lang.NonNull;
import com.mongodb.lang.Nullable;
import dev.morphia.Datastore;
import dev.morphia.EntityInterceptor;
import dev.morphia.annotations.Embedded;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.EntityListeners;
import dev.morphia.annotations.PostLoad;
import dev.morphia.annotations.PostPersist;
import dev.morphia.annotations.PreLoad;
import dev.morphia.annotations.PrePersist;
import dev.morphia.mapping.InstanceCreatorFactory;
import dev.morphia.mapping.InstanceCreatorFactoryImpl;
import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.MappingException;
import dev.morphia.mapping.codec.MorphiaInstanceCreator;
import dev.morphia.sofia.Sofia;
import org.bson.Document;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.util.Arrays.asList;

/**
 * A model of metadata about a type
 *
 * @morphia.internal
 * @since 2.0
 */
@SuppressWarnings("unchecked")
public class EntityModel {
    private static final List<Class<? extends Annotation>> LIFECYCLE_ANNOTATIONS = asList(PrePersist.class,
        PreLoad.class,
        PostPersist.class,
        PostLoad.class);

    private final Map<Class<? extends Annotation>, Annotation> annotations;
    private final Map<String, PropertyModel> propertyModelsByName;
    private final Map<String, PropertyModel> propertyModelsByMappedName;
    private final InstanceCreatorFactory creatorFactory;
    private final boolean discriminatorEnabled;
    private final String discriminatorKey;
    private final String discriminator;
    private final Class<?> type;
    private final String collectionName;
    private final List<EntityModel> subtypes = new ArrayList<>();
    private final EntityModel superClass;
    private final PropertyModel idProperty;
    private final PropertyModel versionProperty;
    private final Mapper mapper;
    private Map<Class<? extends Annotation>, List<ClassMethodPair>> lifecycleMethods;

    /**
     * Creates a new instance
     *
     * @param builder the builder to pull values from
     */
    EntityModel(Mapper mapper, EntityModelBuilder builder) {
        type = builder.type();
        this.mapper = mapper;
        if (!Modifier.isStatic(type.getModifiers()) && type.isMemberClass()) {
            throw new MappingException(Sofia.noInnerClasses(type.getName()));
        }

        superClass = builder.superclass();
        discriminatorEnabled = builder.isDiscriminatorEnabled();
        discriminatorKey = builder.discriminatorKey();
        discriminator = builder.discriminator();

        this.annotations = builder.annotationsMap();
        this.propertyModelsByName = new LinkedHashMap<>();
        this.propertyModelsByMappedName = new LinkedHashMap<>();
        builder.propertyModels().forEach(modelBuilder -> {
            PropertyModel model = modelBuilder
                .owner(this)
                .build();
            propertyModelsByMappedName.put(model.getMappedName(), model);
            for (String name : modelBuilder.alternateNames()) {
                if (propertyModelsByMappedName.put(name, model) != null) {
                    throw new MappingException(Sofia.duplicatedMappedName(type.getCanonicalName(), name));
                }
            }
            propertyModelsByName.putIfAbsent(model.getName(), model);
        });

        this.collectionName = builder.getCollectionName();
        creatorFactory = new InstanceCreatorFactoryImpl(this);

        if (superClass != null) {
            superClass.addSubtype(this);
        }
        idProperty = getProperty(builder.idPropertyName());
        versionProperty = getProperty(builder.versionPropertyName());

        builder.interfaces().forEach(i -> i.addSubtype(this));
    }

    /**
     * Invokes any lifecycle methods
     *
     * @param event     the event to run
     * @param entity    the entity to use
     * @param document  the document used in persistence
     * @param datastore the Datastore to use
     */
    public void callLifecycleMethods(Class<? extends Annotation> event, Object entity, Document document,
                                     Datastore datastore) {
        final List<ClassMethodPair> methodPairs = getLifecycleMethods().get(event);
        if (methodPairs != null) {
            for (ClassMethodPair cm : methodPairs) {
                cm.invoke(datastore, document, entity);
            }
        }

        callGlobalInterceptors(event, entity, document, mapper);
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
     * Returns all the annotations on this model
     *
     * @return the list of annotations
     */
    public Map<Class<? extends Annotation>, Annotation> getAnnotations() {
        return annotations;
    }

    /**
     * @return the mapped collection name for the type
     */
    @NonNull
    public String getCollectionName() {
        if (collectionName == null) {
            throw new MappingException(Sofia.noMappedCollection(getType().getName()));
        }
        return collectionName;
    }

    /**
     * @return the discriminator
     */
    public String getDiscriminator() {
        return discriminator;
    }

    /**
     * @return the discriminator key
     */
    public String getDiscriminatorKey() {
        return discriminatorKey;
    }

    /**
     * @return the embeddedAn
     */
    @Nullable
    public Embedded getEmbeddedAnnotation() {
        return getAnnotation(Embedded.class);
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

    /**
     * @return a new InstanceCreator instance for the ClassModel
     */
    public MorphiaInstanceCreator getInstanceCreator() {
        return creatorFactory.create();
    }

    /**
     * @return thee creator factory
     * @morphia.internal
     */
    public InstanceCreatorFactory getInstanceCreatorFactory() {
        return creatorFactory;
    }

    /**
     * @return the lifecycle event methods
     */
    public Map<Class<? extends Annotation>, List<ClassMethodPair>> getLifecycleMethods() {
        if (lifecycleMethods == null) {
            lifecycleMethods = new HashMap<>();

            final EntityListeners entityLisAnn = getAnnotation(EntityListeners.class);
            if (entityLisAnn != null && entityLisAnn.value().length != 0) {
                for (Class<?> aClass : entityLisAnn.value()) {
                    mapEvent(aClass, true);
                }
            }

            mapEvent(getType(), false);
        }
        return lifecycleMethods;
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

    /**
     * Get the subtypes of this model
     *
     * @return the subtypes
     */
    public List<EntityModel> getSubtypes() {
        return subtypes;
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

    /**
     * @return the version property for the class
     */
    @Nullable
    public PropertyModel getVersionProperty() {
        return versionProperty;
    }

    /**
     * @param type the lifecycle event type
     * @return true if that even has been configured
     */
    public boolean hasLifecycle(Class<? extends Annotation> type) {
        return getLifecycleMethods().containsKey(type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getAnnotations(), propertyModelsByName, propertyModelsByMappedName, creatorFactory,
            discriminatorEnabled,
            getDiscriminatorKey(), getDiscriminator(), getType(), getCollectionName(), getLifecycleMethods());
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
               && Objects.equals(getAnnotations(), that.getAnnotations())
               && Objects.equals(propertyModelsByName, that.propertyModelsByName)
               && Objects.equals(propertyModelsByMappedName, that.propertyModelsByMappedName)
               && Objects.equals(creatorFactory, that.creatorFactory)
               && Objects.equals(getDiscriminatorKey(), that.getDiscriminatorKey())
               && Objects.equals(getDiscriminator(), that.getDiscriminator())
               && Objects.equals(getType(), that.getType())
               && Objects.equals(getCollectionName(), that.getCollectionName())
               && Objects.equals(getLifecycleMethods(), that.getLifecycleMethods());
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

    public boolean useDiscriminator() {
        return discriminatorEnabled;
    }

    private void addSubtype(EntityModel entityModel) {
        subtypes.add(entityModel);
        if (superClass != null) {
            superClass.addSubtype(entityModel);
        }
    }

    private void callGlobalInterceptors(Class<? extends Annotation> event, Object entity, Document document,
                                        Mapper mapper) {
        for (EntityInterceptor ei : mapper.getInterceptors()) {
            Sofia.logCallingInterceptorMethod(event.getSimpleName(), ei);

            if (event.equals(PreLoad.class)) {
                ei.preLoad(entity, document, mapper);
            } else if (event.equals(PostLoad.class)) {
                ei.postLoad(entity, document, mapper);
            } else if (event.equals(PrePersist.class)) {
                ei.prePersist(entity, document, mapper);
            } else if (event.equals(PostPersist.class)) {
                ei.postPersist(entity, document, mapper);
            }
        }
    }

    private List<Method> getDeclaredAndInheritedMethods(Class<?> type) {
        final List<Method> methods = new ArrayList<>();
        if (type == Object.class) {
            return methods;
        }

        final Class<?> parent = type.getSuperclass();
        if (parent != null) {
            methods.addAll(getDeclaredAndInheritedMethods(parent));
        }

        for (Method m : type.getDeclaredMethods()) {
            if (!Modifier.isStatic(m.getModifiers())) {
                methods.add(m);
            }
        }

        return methods;
    }

    private void mapEvent(Class<?> type, boolean entityListener) {
        for (Method method : getDeclaredAndInheritedMethods(type)) {
            for (Class<? extends Annotation> annotationClass : LIFECYCLE_ANNOTATIONS) {
                if (method.isAnnotationPresent(annotationClass)) {
                    lifecycleMethods.computeIfAbsent(annotationClass, c -> new ArrayList<>())
                                    .add(new ClassMethodPair(method, entityListener ? type : null, annotationClass));
                }
            }
        }
    }
}
