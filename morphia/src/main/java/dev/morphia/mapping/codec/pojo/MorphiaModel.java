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
import dev.morphia.mapping.MapperOptions;
import dev.morphia.sofia.Sofia;
import org.bson.Document;
import org.bson.codecs.pojo.ClassModel;
import org.bson.codecs.pojo.IdPropertyModelHolder;
import org.bson.codecs.pojo.InstanceCreatorFactory;
import org.bson.codecs.pojo.PropertyModel;
import org.bson.codecs.pojo.TypeParameterMap;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.lang.String.format;

public class MorphiaModel<T> extends ClassModel<T> {
    private final Map<Class<? extends Annotation>, List<Annotation>> annotations;
    private final List<FieldModel<?>> fieldModels;
    private final Datastore datastore;
    private final MapperOptions options;
    private Map<Class<? extends Annotation>, List<ClassMethodPair>> lifecycleMethods;
    private String collectionName;

    public MorphiaModel(final Datastore datastore,
                        final MapperOptions options,
                        final InstanceCreatorFactory<T> instanceCreatorFactory,
                        final Boolean discriminatorEnabled,
                        final String discriminatorKey,
                        final String discriminator,
                        final IdPropertyModelHolder<?> idPropertyModelHolder,
                        final Map<Class<? extends Annotation>, List<Annotation>> annotations,
                        final List<FieldModel<?>> fieldModels,
                        final List<PropertyModel<?>> propertyModels,
                        final Class<T> clazz, final Map<String, TypeParameterMap> propertyNameToTypeParameterMap) {
        super(clazz, propertyNameToTypeParameterMap, instanceCreatorFactory, discriminatorEnabled, discriminatorKey, discriminator,
            idPropertyModelHolder, propertyModels);
        this.annotations = annotations;
        this.fieldModels = fieldModels;
        this.datastore = datastore;
        this.options = options;
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

    public <A> A getAnnotation(final Class<? extends Annotation> clazz) {
        final List<Annotation> found = annotations.get(clazz);
        return found == null || found.isEmpty() ? null : (A) found.get(found.size() - 1);
    }

    public <A> List<A> getAnnotations(final Class<? extends Annotation> clazz) {
        return (List<A>) annotations.get(clazz);
    }

    @Override
    public String toString() {
        String fields = fieldModels.stream().map(f -> format("%s %s", f.getTypeData(), f.getName()))
                                         .collect(Collectors.joining(", "));
        return format("%s<%s> { %s } ", MorphiaModel.class.getSimpleName(), getName(), fields);
    }

    public String getCollectionName() {
        if(collectionName == null) {
            Entity entityAn = getAnnotation(Entity.class);
            if(entityAn != null) {
                collectionName = null;
                if (entityAn.value().equals(Mapper.IGNORED_FIELDNAME)) {
                    return options.isUseLowerCaseCollectionNames() ? getType().getSimpleName().toLowerCase() :
                           getType().getSimpleName();
                } else {
                    return entityAn.value();
                }
            }
        }
        return collectionName;
    }

    @SuppressWarnings("unchecked")
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

    private void mapEvent(final Class<?> type, final boolean entityListener) {
        for (final Method method : getDeclaredAndInheritedMethods(type)) {
            for (final Class<? extends Annotation> annotationClass : Mapper.LIFECYCLE_ANNOTATIONS) {
                if (method.isAnnotationPresent(annotationClass)) {
                    lifecycleMethods.computeIfAbsent(annotationClass, c -> new ArrayList<>())
                       .add(new ClassMethodPair(datastore, method, entityListener ? type : null, annotationClass));
                }
            }
        }
    }

    public Map<Class<? extends Annotation>, List<ClassMethodPair>> getLifecycleMethods() {
        if(lifecycleMethods == null) {
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

    public boolean hasLifecycle(Class<? extends Annotation> klass) {
        return getLifecycleMethods().containsKey(klass);
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
