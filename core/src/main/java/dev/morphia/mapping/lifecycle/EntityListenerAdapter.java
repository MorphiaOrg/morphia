package dev.morphia.mapping.lifecycle;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mongodb.lang.NonNull;

import dev.morphia.Datastore;
import dev.morphia.EntityListener;
import dev.morphia.annotations.PostLoad;
import dev.morphia.annotations.PostPersist;
import dev.morphia.annotations.PreLoad;
import dev.morphia.annotations.PrePersist;
import dev.morphia.annotations.internal.MorphiaInternal;
import dev.morphia.mapping.MappingException;
import dev.morphia.sofia.Sofia;

import org.bson.Document;

import static dev.morphia.mapping.Mapper.LIFECYCLE_ANNOTATIONS;

/**
 * @hidden
 * @since 2.4
 * @morphia.internal
 */
@MorphiaInternal
public class EntityListenerAdapter implements EntityListener<Object> {
    private final Map<Class<? extends Annotation>, List<Method>> methods = new HashMap<>();
    private final Class<?> listenerType;
    private Object listener;

    public EntityListenerAdapter(Class<?> listenerType) {
        LIFECYCLE_ANNOTATIONS.stream().forEach(annotationClass -> {
            methods.put(annotationClass, new ArrayList<>());
        });
        mapAnnotationsToMethods(listenerType);
        this.listenerType = listenerType;
    }

    @Override
    public boolean hasAnnotation(@NonNull Class<? extends Annotation> type) {
        return !methods.get(type).isEmpty();
    }

    Map<Class<? extends Annotation>, List<Method>> getMethods() {
        return methods;
    }

    protected Object[] collectArgs(Method method, Object entity, Document document, Datastore datastore) {
        List<Object> args = new ArrayList<>();
        for (Class<?> parameterType : method.getParameterTypes()) {
            if (parameterType.equals(Document.class)) {
                args.add(document);
            } else if (parameterType.equals(Datastore.class)) {
                args.add(datastore);
            } else {
                args.add(entity);
            }
        }
        return args.toArray();
    }

    void invoke(Class<? extends Annotation> annotation, Object entity, Document document, Datastore datastore) {
        methods.get(annotation)
                .forEach(method -> {
                    try {
                        method.invoke(getListener(), collectArgs(method, entity, document, datastore));
                    } catch (ReflectiveOperationException e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    Object getListener() {
        if (listener == null) {
            try {
                Constructor<?> constructor = listenerType.getDeclaredConstructor();
                constructor.setAccessible(true);
                listener = constructor.newInstance();
            } catch (ReflectiveOperationException e) {
                throw new MappingException(Sofia.noargConstructorNotFound(listenerType), e);
            }
        }
        return listener;
    }

    @Override
    public void postLoad(@NonNull Object entity, @NonNull Document document, @NonNull Datastore datastore) {
        invoke(PostLoad.class, entity, document, datastore);
    }

    @Override
    public void postPersist(@NonNull Object entity, @NonNull Document document, @NonNull Datastore datastore) {
        invoke(PostPersist.class, entity, document, datastore);
    }

    @Override
    public void preLoad(@NonNull Object entity, @NonNull Document document, @NonNull Datastore datastore) {
        invoke(PreLoad.class, entity, document, datastore);
    }

    @Override
    public void prePersist(@NonNull Object entity, @NonNull Document document, @NonNull Datastore datastore) {
        invoke(PrePersist.class, entity, document, datastore);
    }

    @NonNull
    private void mapAnnotationsToMethods(Class<?> type) {
        for (Method method : getDeclaredAndInheritedMethods(type)) {
            for (Class<? extends Annotation> annotationClass : LIFECYCLE_ANNOTATIONS) {
                if (method.isAnnotationPresent(annotationClass)) {
                    method.setAccessible(true);
                    methods.get(annotationClass)
                            .add(method);
                }
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
}
