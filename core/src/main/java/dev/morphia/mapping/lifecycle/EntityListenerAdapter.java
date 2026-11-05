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
import com.mongodb.lang.Nullable;

import dev.morphia.Datastore;
import dev.morphia.EntityListener;
import dev.morphia.annotations.internal.MorphiaInternal;
import dev.morphia.mapping.MappingException;
import dev.morphia.sofia.Sofia;

import org.bson.Document;

import static dev.morphia.mapping.Mapper.LIFECYCLE_ANNOTATIONS;

/**
 * Calls the lifecycle methods of an {@code @EntityListeners} class, or of an entity: its methods annotated with a lifecycle
 * event annotation, and those of any per-event listener interface it implements. It implements {@link EntityListener} only so
 * it can be named in {@code @EntityListeners}.
 *
 * @hidden
 * @since 2.4
 * @morphia.internal
 */
@MorphiaInternal
public class EntityListenerAdapter implements EntityListener<Object> {
    private final Map<Class<? extends Annotation>, List<Callback>> callbacks = new HashMap<>();
    private final Class<?> listenerType;
    // whether it implements a per-event listener interface
    private final boolean typed;
    private Object listener;

    /**
     * @param listenerType the {@code @EntityListeners} class or the entity
     * @throws MappingException if it implements two listener interfaces for one event
     */
    public EntityListenerAdapter(Class<?> listenerType) {
        for (Class<? extends Annotation> annotationClass : LIFECYCLE_ANNOTATIONS) {
            callbacks.put(annotationClass, new ArrayList<>());
        }
        mapAnnotationsToMethods(listenerType);
        this.listenerType = listenerType;
        // checked here, when the entity is mapped: annotated methods would otherwise short-circuit every check
        typed = TypedListeners.handlesAny(listenerType);
    }

    /**
     * @param event the lifecycle event annotation
     * @return true if the listener handles the event, with an annotated method or its per-event listener interface
     */
    public boolean handles(@NonNull Class<? extends Annotation> event) {
        return !callbacks.get(event).isEmpty() || TypedListeners.handles(listenerType, event);
    }

    /**
     * @param event the lifecycle event annotation
     * @return true if a method for the event takes the {@code Document}: an annotated method with a {@code Document}
     *         parameter, or the event's listener interface's method
     */
    public boolean usesDocument(@NonNull Class<? extends Annotation> event) {
        for (Callback callback : callbacks.get(event)) {
            if (callback.usesDocument()) {
                return true;
            }
        }
        return TypedListeners.usesDocument(listenerType, event);
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
        return typed;
    }

    /**
     * Calls the listener's annotated methods for the event, then its per-event listener interface's method.
     *
     * @param event     the lifecycle event annotation
     * @param entity    the entity
     * @param document  the document, or null when no method for the event takes one
     * @param datastore the datastore
     */
    public void invoke(Class<? extends Annotation> event, Object entity, @Nullable Document document, Datastore datastore) {
        for (Callback callback : callbacks.get(event)) {
            try {
                callback.method.invoke(target(entity), callback.arguments(entity, document, datastore));
            } catch (IllegalArgumentException | ReflectiveOperationException e) {
                throw failure(callback.method, entity, e);
            }
        }
        if (TypedListeners.handles(listenerType, event)) {
            TypedListeners.invoke(target(entity), event, entity, document, datastore);
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

        Object[] arguments(Object entity, @Nullable Document document, Datastore datastore) {
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
