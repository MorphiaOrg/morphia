package dev.morphia.mapping.codec.pojo;

import dev.morphia.Datastore;
import dev.morphia.EntityInterceptor;
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
import java.util.Collection;
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
 * @param <T> the entity type
 * @morphia.internal
 * @since 2.0
 */
@SuppressWarnings("unchecked")
public class EntityModel<T> {
    private static final List<Class<? extends Annotation>> LIFECYCLE_ANNOTATIONS = asList(PrePersist.class,
        PreLoad.class,
        PostPersist.class,
        PostLoad.class);

    private final Map<Class<? extends Annotation>, Annotation> annotations;
    private final Map<String, FieldModel<?>> fieldModelsByField;
    private final Map<String, FieldModel<?>> fieldModelsByMappedName;
    private final Datastore datastore;
    private final InstanceCreatorFactory<T> creatorFactory;
    private final boolean discriminatorEnabled;
    private final String discriminatorKey;
    private final String discriminator;
    private final Class<T> type;
    private final String collectionName;
    private Map<Class<? extends Annotation>, List<ClassMethodPair>> lifecycleMethods;

    /**
     * Creates a new instance
     *
     * @param builder the builder to pull values from
     */
    EntityModel(EntityModelBuilder<T> builder) {
        type = builder.getType();
        if (!Modifier.isStatic(type.getModifiers()) && type.isMemberClass()) {
            throw new MappingException(Sofia.noInnerClasses(type.getName()));
        }

        discriminatorEnabled = builder.isDiscriminatorEnabled();
        discriminatorKey = builder.discriminatorKey();
        discriminator = builder.discriminator();

        this.annotations = builder.annotationsMap();
        this.fieldModelsByField = new LinkedHashMap<>();
        this.fieldModelsByMappedName = new LinkedHashMap<>();
        builder.fieldModels().forEach(modelBuilder -> {
            FieldModel<?> model = modelBuilder.build();
            fieldModelsByMappedName.put(model.getMappedName(), model);
            for (String name : modelBuilder.alternateNames()) {
                if (fieldModelsByMappedName.put(name, model) != null) {
                    throw new MappingException(Sofia.duplicatedMappedName(type.getCanonicalName(), name));
                }
            }
            fieldModelsByField.putIfAbsent(model.getName(), model);
        });

        this.datastore = builder.getDatastore();
        this.collectionName = builder.getCollectionName();
        creatorFactory = new InstanceCreatorFactoryImpl<>(this);
    }

    /**
     * Invokes any lifecycle methods
     *
     * @param event    the event to run
     * @param entity   the entity to use
     * @param document the document used in persistence
     * @param mapper   the mapper to use
     */
    public void callLifecycleMethods(Class<? extends Annotation> event, Object entity, Document document,
                                     Mapper mapper) {
        final List<ClassMethodPair> methodPairs = getLifecycleMethods().get(event);
        if (methodPairs != null) {
            for (ClassMethodPair cm : methodPairs) {
                cm.invoke(document, entity);
            }
        }

        callGlobalInterceptors(event, entity, document, mapper);
    }

    /**
     * @param clazz the annotation class
     * @param <A>   the annotation type
     * @return the annotation instance or null if not found
     */
    public <A extends Annotation> A getAnnotation(Class<A> clazz) {
        return (A) annotations.get(clazz);
    }

    /**
     * @param <A>   the annotation type
     * @param clazz the annotation class
     * @return the annotation instance of the given type
     */
    public <A extends Annotation> A getAnnotations(Class<A> clazz) {
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
    public String getCollectionName() {
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
     * @param name the property name
     * @return the named FieldModel or null if it does not exist
     */
    public FieldModel<?> getFieldModelByName(String name) {
        return fieldModelsByMappedName.getOrDefault(name, fieldModelsByField.get(name));
    }

    /**
     * Returns all the fields on this model
     *
     * @return the list of fields
     */
    public Collection<FieldModel<?>> getFieldModels() {
        return fieldModelsByField.values();
    }

    /**
     * @return the model for the id field
     */
    public FieldModel<?> getIdModel() {
        return fieldModelsByMappedName.get("_id");
    }

    /**
     * @return a new InstanceCreator instance for the ClassModel
     */
    public MorphiaInstanceCreator<T> getInstanceCreator() {
        return creatorFactory.create();
    }

    /**
     * @return thee creator factory
     * @morphia.internal
     */
    public InstanceCreatorFactory<T> getInstanceCreatorFactory() {
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
     * @return the type of this model
     */
    public Class<T> getType() {
        return type;
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
        return Objects.hash(getAnnotations(), fieldModelsByField, fieldModelsByMappedName, datastore, creatorFactory, discriminatorEnabled,
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
        final EntityModel<?> that = (EntityModel<?>) o;
        return discriminatorEnabled == that.discriminatorEnabled
               && Objects.equals(getAnnotations(), that.getAnnotations())
               && Objects.equals(fieldModelsByField, that.fieldModelsByField)
               && Objects.equals(fieldModelsByMappedName, that.fieldModelsByMappedName)
               && Objects.equals(datastore, that.datastore)
               && Objects.equals(creatorFactory, that.creatorFactory)
               && Objects.equals(getDiscriminatorKey(), that.getDiscriminatorKey())
               && Objects.equals(getDiscriminator(), that.getDiscriminator())
               && Objects.equals(getType(), that.getType())
               && Objects.equals(getCollectionName(), that.getCollectionName())
               && Objects.equals(getLifecycleMethods(), that.getLifecycleMethods());
    }

    @Override
    public String toString() {
        String fields = fieldModelsByField.values().stream().map(f -> format("%s %s", f.getTypeData(), f.getName()))
                                          .collect(Collectors.joining(", "));
        return format("%s<%s> { %s } ", EntityModel.class.getSimpleName(), type.getSimpleName(), fields);
    }

    protected boolean useDiscriminator() {
        return discriminatorEnabled;
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
        if ((type == null) || (type == Object.class)) {
            return methods;
        }

        final Class<?> parent = type.getSuperclass();
        methods.addAll(getDeclaredAndInheritedMethods(parent));

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
                                    .add(new ClassMethodPair(datastore, method, entityListener ? type : null, annotationClass));
                }
            }
        }
    }
}
