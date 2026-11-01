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
    private final Map<Class<? extends Annotation>, List<Callback>> callbacks = new HashMap<>();
    private final Class<?> listenerType;
    private Object listener;

    public EntityListenerAdapter(Class<?> listenerType) {
        for (Class<? extends Annotation> annotationClass : LIFECYCLE_ANNOTATIONS) {
            callbacks.put(annotationClass, new ArrayList<>());
        }
        mapAnnotationsToMethods(listenerType);
        this.listenerType = listenerType;
    }

    @Override
    public boolean hasAnnotation(@NonNull Class<? extends Annotation> type) {
        return !callbacks.get(type).isEmpty();
    }

    /**
     * @param type the lifecycle event annotation
     * @return true if a lifecycle method for the event takes a {@code Document}
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public boolean usesDocument(@NonNull Class<? extends Annotation> type) {
        for (Callback callback : callbacks.get(type)) {
            if (callback.usesDocument()) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return true if any lifecycle method was found
     */
    boolean hasCallbacks() {
        for (List<Callback> list : callbacks.values()) {
            if (!list.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    void invoke(Class<? extends Annotation> annotation, Object entity, Document document, Object datastore) {
        for (Callback callback : callbacks.get(annotation)) {
            try {
                callback.method.invoke(target(entity), callback.arguments(entity, document, datastore));
            } catch (IllegalArgumentException | ReflectiveOperationException e) {
                throw failure(callback.method, entity, e);
            }
        }
    }

    /**
     * @param entity the entity the event is for
     * @return the object to call the lifecycle methods on: an instance of the listener type
     */
    protected Object target(Object entity) {
        return getListener();
    }

    /**
     * @param method the lifecycle method that failed
     * @param entity the entity the event is for
     * @param e      the failure
     * @return the exception to throw
     */
    protected RuntimeException failure(Method method, Object entity, Exception e) {
        return e instanceof IllegalArgumentException illegal ? illegal : new RuntimeException(e);
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
    public void postLoad(@NonNull Object entity, @NonNull Document document, @NonNull Object datastore) {
        invoke(PostLoad.class, entity, document, datastore);
    }

    @Override
    public void postPersist(@NonNull Object entity, @NonNull Document document, @NonNull Object datastore) {
        invoke(PostPersist.class, entity, document, datastore);
    }

    @Override
    public void preLoad(@NonNull Object entity, @NonNull Document document, @NonNull Object datastore) {
        invoke(PreLoad.class, entity, document, datastore);
    }

    @Override
    public void prePersist(@NonNull Object entity, @NonNull Document document, @NonNull Object datastore) {
        invoke(PrePersist.class, entity, document, datastore);
    }

    @NonNull
    private void mapAnnotationsToMethods(Class<?> type) {
        for (Method method : getDeclaredAndInheritedMethods(type)) {
            for (Class<? extends Annotation> annotationClass : LIFECYCLE_ANNOTATIONS) {
                if (method.isAnnotationPresent(annotationClass)) {
                    method.setAccessible(true);
                    callbacks.get(annotationClass)
                            .add(new Callback(method));
                }
            }
        }
    }

    /**
     * A lifecycle method and, worked out once, what to pass for each of its parameters.
     */
    private static final class Callback {
        private static final byte ENTITY = 0;
        private static final byte DOCUMENT = 1;
        private static final byte DATASTORE = 2;

        private final Method method;
        private final byte[] parameters;

        Callback(Method method) {
            this.method = method;
            Class<?>[] types = method.getParameterTypes();
            parameters = new byte[types.length];
            for (int i = 0; i < types.length; i++) {
                parameters[i] = types[i].equals(Document.class) ? DOCUMENT
                        : types[i].equals(Datastore.class) ? DATASTORE
                                : ENTITY;
            }
        }

        boolean usesDocument() {
            for (byte parameter : parameters) {
                if (parameter == DOCUMENT) {
                    return true;
                }
            }
            return false;
        }

        Object[] arguments(Object entity, Document document, Object datastore) {
            Object[] arguments = new Object[parameters.length];
            for (int i = 0; i < parameters.length; i++) {
                arguments[i] = switch (parameters[i]) {
                    case DOCUMENT -> document;
                    case DATASTORE -> datastore;
                    default -> entity;
                };
            }
            return arguments;
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
