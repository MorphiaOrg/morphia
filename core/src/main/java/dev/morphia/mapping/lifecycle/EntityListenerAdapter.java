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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static dev.morphia.mapping.Mapper.LIFECYCLE_ANNOTATIONS;

/**
 * @hidden
 * @since 2.4
 * @morphia.internal
 */
@MorphiaInternal
public class EntityListenerAdapter implements EntityListener<Object> {
    private static final Logger LOG = LoggerFactory.getLogger(EntityListenerAdapter.class);

    private final Map<Class<? extends Annotation>, List<Method>> methods;
    private final Class<?> listenerType;

    private static final Map<String, String> annotationMethods = new HashMap<>();

    private Object listener;

    static {
        annotationMethods.put(PostPersist.class.getSimpleName(), "postPersist");
        annotationMethods.put(PostLoad.class.getSimpleName(), "postLoad");
        annotationMethods.put(PrePersist.class.getSimpleName(), "prePersist");
        annotationMethods.put(PreLoad.class.getSimpleName(), "preLoad");
    }

    public EntityListenerAdapter(Class<?> listenerType) {
        this.listenerType = listenerType;
        this.methods = mapAnnotationsToMethods();
    }

    @Override
    public boolean hasAnnotation(Class<? extends Annotation> type) {
        return methods.containsKey(type);
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

    void invoke(Class<?> annotation, Object entity, Document document, Datastore datastore) {
        try {
            methods.get(annotation)
                    .forEach(method -> {
                        try {
                            method.invoke(getListener(), entity, document, datastore);
                        } catch (Exception e) {
                            throw new RuntimeException(e.getMessage(), e);
                        }
                    });
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
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
    private Map<Class<? extends Annotation>, List<Method>> mapAnnotationsToMethods() {
        final Map<Class<? extends Annotation>, List<Method>> methods = new HashMap<>();
        EntityListener<?> listener = (EntityListener.class.isAssignableFrom(listenerType)) ? (EntityListener<?>) getListener() : null;
        for (Class<? extends Annotation> annotationClass : LIFECYCLE_ANNOTATIONS) {
            for (Method method : getDeclaredAndInheritedMethods(listenerType)) {
                if (method.isAnnotationPresent(annotationClass)) {
                    method.setAccessible(true);
                    methods.computeIfAbsent(annotationClass, c -> new ArrayList<>())
                            .add(method);
                }
            }
            if (listener != null && listener.hasAnnotation(annotationClass)) {
                String name = annotationMethods.get(annotationClass.getSimpleName());
                try {
                    Method method = listenerType.getMethod(name, Object.class, Document.class, Datastore.class);
                    method.setAccessible(true);
                    methods.computeIfAbsent(annotationClass, c -> new ArrayList<>())
                            .add(method);
                } catch (NoSuchMethodException e) {
                    LOG.error(Sofia.missingLifecycleEvent(name, annotationClass.getName()));
                }
            }
        }
        return methods;
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
