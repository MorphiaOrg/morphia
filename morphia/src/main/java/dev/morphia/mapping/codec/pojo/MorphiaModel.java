package dev.morphia.mapping.codec.pojo;

import dev.morphia.Datastore;
import dev.morphia.EntityInterceptor;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.EntityListeners;
import dev.morphia.annotations.PostLoad;
import dev.morphia.annotations.PostPersist;
import dev.morphia.annotations.PreLoad;
import dev.morphia.annotations.PrePersist;
import dev.morphia.mapping.Mapper;
import dev.morphia.sofia.Sofia;
import org.bson.Document;
import org.bson.codecs.pojo.ClassModel;
import org.bson.codecs.pojo.IdPropertyModelHolder;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
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
 */
@SuppressWarnings("unchecked")
public class MorphiaModel<T> extends ClassModel<T> {
    private static final List<Class<? extends Annotation>> LIFECYCLE_ANNOTATIONS = asList(PrePersist.class,
        PreLoad.class,
        PostPersist.class,
        PostLoad.class);

    private final Map<Class<? extends Annotation>, List<Annotation>> annotations;
    private final List<FieldModel<?>> fieldModels;
    private final Datastore datastore;
    private Map<Class<? extends Annotation>, List<ClassMethodPair>> lifecycleMethods;
    private String collectionName;

    /**
     * Copies an existing model
     *
     * @param morphiaModel the model to copy
     * @param useDiscriminator the new use discriminator value
     */
    public MorphiaModel(final MorphiaModel morphiaModel, final boolean useDiscriminator) {
        super(morphiaModel.getType(), morphiaModel.getPropertyNameToTypeParameterMap(), morphiaModel.getInstanceCreatorFactory(),
            useDiscriminator, morphiaModel.getDiscriminatorKey(),
            morphiaModel.getDiscriminator(),
            morphiaModel.getIdPropertyModelHolder(), morphiaModel.getPropertyModels());

        this.annotations = morphiaModel.getAnnotations();
        this.fieldModels = morphiaModel.getFieldModels();
        this.datastore = morphiaModel.datastore;
    }

    /**
     * Creates a new instance
     *
     * @param builder the builder to pull values from
     */
    MorphiaModel(final MorphiaModelBuilder builder) {
        super(builder.getType(), builder.getPropertyNameToTypeParameterMap(), builder.getInstanceCreatorFactory(),
            builder.useDiscriminator(), builder.getDiscriminatorKey(), builder.getDiscriminator(),
            IdPropertyModelHolder.create(builder.getType(), builder.getIdPropertyModel(), builder.getIdGenerator()),
            builder.getPropertyModels());

        this.annotations = builder.getAnnotationsMap();
        this.fieldModels = builder.getFieldModels();
        this.datastore = builder.getDatastore();
    }

    /**
     * Returns all the annotations on this model
     *
     * @return the list of annotations
     */
    public Map<Class<? extends Annotation>, List<Annotation>> getAnnotations() {
        return annotations;
    }

    /**
     * Returns all the fields on this model
     *
     * @return the list of fields
     */
    public List<FieldModel<?>> getFieldModels() {
        return fieldModels;
    }

    /**
     * @param clazz the annotation class
     * @param <A>   the annotation type
     * @return the annotation instance or null if not found
     */
    public <A> A getAnnotation(final Class<? extends Annotation> clazz) {
        final List<Annotation> found = annotations.get(clazz);
        return found == null || found.isEmpty() ? null : (A) found.get(found.size() - 1);
    }

    /**
     * @param clazz the annotation class
     * @param <A>   the annotation type
     * @return all annotation instance of the given type
     */
    public <A> List<A> getAnnotations(final Class<? extends Annotation> clazz) {
        return (List<A>) annotations.get(clazz);
    }

    /**
     * @return the mapped collection name for the type
     */
    public String getCollectionName() {
        if (collectionName == null) {
            Entity entityAn = getAnnotation(Entity.class);
            if (entityAn != null) {
                collectionName = null;
                if (entityAn.value().equals(Mapper.IGNORED_FIELDNAME)) {
                    return datastore.getMapper().getOptions().isUseLowerCaseCollectionNames()
                           ? getType().getSimpleName().toLowerCase()
                           : getType().getSimpleName();
                } else {
                    return entityAn.value();
                }
            }
        }
        return collectionName;
    }

    /**
     * Invokes any lifecycle methods
     *
     * @param event    the event to run
     * @param entity   the entity to use
     * @param document the document used in persistence
     * @param mapper   the mapper to use
     */
    public void callLifecycleMethods(final Class<? extends Annotation> event, final Object entity, final Document document,
                                     final Mapper mapper) {
        final List<ClassMethodPair> methodPairs = getLifecycleMethods().get(event);
        if (methodPairs != null) {
            for (final ClassMethodPair cm : methodPairs) {
                cm.invoke(document, entity);
            }
        }

        callGlobalInterceptors(event, entity, document, mapper);
    }

    /**
     * @return the lifecycle event methods
     */
    public Map<Class<? extends Annotation>, List<ClassMethodPair>> getLifecycleMethods() {
        if (lifecycleMethods == null) {
            lifecycleMethods = new HashMap<>();

            final EntityListeners entityLisAnn = getAnnotation(EntityListeners.class);
            if (entityLisAnn != null && entityLisAnn.value().length != 0) {
                for (final Class<?> aClass : entityLisAnn.value()) {
                    mapEvent(aClass, true);
                }
            }

            mapEvent(getType(), false);
        }
        return lifecycleMethods;
    }

    /**
     * @param type the lifecycle event type
     * @return true if that even has been configured
     */
    public boolean hasLifecycle(final Class<? extends Annotation> type) {
        return getLifecycleMethods().containsKey(type);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof MorphiaModel)) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }

        final MorphiaModel<?> that = (MorphiaModel<?>) o;

        if (getAnnotations() != null ? !getAnnotations().equals(that.getAnnotations()) : that.getAnnotations() != null) {
            return false;
        }
        if (getFieldModels() != null ? !getFieldModels().equals(that.getFieldModels()) : that.getFieldModels() != null) {
            return false;
        }
        if (!Objects.equals(datastore, that.datastore)) {
            return false;
        }
        if (getLifecycleMethods() != null ? !getLifecycleMethods().equals(that.getLifecycleMethods())
                                          : that.getLifecycleMethods() != null) {
            return false;
        }
        return getCollectionName() != null ? getCollectionName().equals(that.getCollectionName()) : that.getCollectionName() == null;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (getAnnotations() != null ? getAnnotations().hashCode() : 0);
        result = 31 * result + (getFieldModels() != null ? getFieldModels().hashCode() : 0);
        result = 31 * result + (datastore != null ? datastore.hashCode() : 0);
        result = 31 * result + (getLifecycleMethods() != null ? getLifecycleMethods().hashCode() : 0);
        result = 31 * result + (getCollectionName() != null ? getCollectionName().hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        String fields = fieldModels.stream().map(f -> format("%s %s", f.getTypeData(), f.getName()))
                                   .collect(Collectors.joining(", "));
        return format("%s<%s> { %s } ", MorphiaModel.class.getSimpleName(), getName(), fields);
    }

    private void mapEvent(final Class<?> type, final boolean entityListener) {
        for (final Method method : getDeclaredAndInheritedMethods(type)) {
            for (final Class<? extends Annotation> annotationClass : LIFECYCLE_ANNOTATIONS) {
                if (method.isAnnotationPresent(annotationClass)) {
                    lifecycleMethods.computeIfAbsent(annotationClass, c -> new ArrayList<>())
                                    .add(new ClassMethodPair(datastore, method, entityListener ? type : null, annotationClass));
                }
            }
        }
    }

    private void callGlobalInterceptors(final Class<? extends Annotation> event, final Object entity, final Document document,
                                        final Mapper mapper) {
        for (final EntityInterceptor ei : mapper.getInterceptors()) {
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

    private List<Method> getDeclaredAndInheritedMethods(final Class type) {
        final List<Method> methods = new ArrayList<>();
        if ((type == null) || (type == Object.class)) {
            return methods;
        }

        final Class parent = type.getSuperclass();
        methods.addAll(getDeclaredAndInheritedMethods(parent));

        for (final Method m : type.getDeclaredMethods()) {
            if (!Modifier.isStatic(m.getModifiers())) {
                methods.add(m);
            }
        }

        return methods;
    }
}
