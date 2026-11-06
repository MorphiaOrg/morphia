package dev.morphia.mapping.lifecycle;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.mongodb.lang.NonNull;
import com.mongodb.lang.Nullable;

import dev.morphia.Datastore;
import dev.morphia.EntityListener;
import dev.morphia.annotations.internal.MorphiaInternal;
import dev.morphia.mapping.MappingException;
import dev.morphia.sofia.Sofia;

import org.bson.Document;

import static dev.morphia.mapping.Mapper.LIFECYCLE_ANNOTATIONS;
import static java.lang.String.format;

/**
 * Calls an entity's own lifecycle methods: its methods annotated with a lifecycle event annotation, and those of any per-event
 * listener interface it implements. Also creates the instances of {@code @EntityListeners} classes, which implement the
 * interfaces only.
 *
 * @hidden
 * @since 2.4
 * @morphia.internal
 */
@MorphiaInternal
public final class EntityListenerAdapter {
    // an adapter only reads its type, so subtypes, mappers and model copies can all share it
    private static final ClassValue<Optional<EntityListenerAdapter>> ADAPTERS = new ClassValue<>() {
        @Override
        protected Optional<EntityListenerAdapter> computeValue(Class<?> entityType) {
            EntityListenerAdapter adapter = new EntityListenerAdapter(entityType);
            // checked here, when the entity is mapped: annotated methods would otherwise short-circuit every check
            boolean typed = TypedListeners.handlesAny(entityType);
            return typed || adapter.hasCallbacks() ? Optional.of(adapter) : Optional.empty();
        }
    };

    private final Map<Class<? extends Annotation>, List<Callback>> callbacks = new HashMap<>();
    private final Class<?> entityType;

    private EntityListenerAdapter(Class<?> entityType) {
        for (Class<? extends Annotation> annotationClass : LIFECYCLE_ANNOTATIONS) {
            callbacks.put(annotationClass, new ArrayList<>());
        }
        mapAnnotationsToMethods(entityType);
        this.entityType = entityType;
    }

    /**
     * @param entityType the entity
     * @return the adapter for the entity's own lifecycle methods, or null when it has none
     * @throws MappingException if it implements two listener interfaces for one event
     */
    @Nullable
    public static EntityListenerAdapter listen(Class<?> entityType) {
        return ADAPTERS.get(entityType).orElse(null);
    }

    /**
     * @param listenerType an {@code @EntityListeners} class
     * @return a new instance of it
     * @throws MappingException if it implements none of the per-event listener interfaces, or two for one event, declares
     *                          annotated lifecycle methods, or has no no-argument constructor or can't be created with it
     */
    public static EntityListener<?> create(Class<? extends EntityListener<?>> listenerType) {
        for (Method method : getDeclaredAndInheritedMethods(listenerType)) {
            for (Class<? extends Annotation> annotationClass : LIFECYCLE_ANNOTATIONS) {
                if (method.isAnnotationPresent(annotationClass)) {
                    throw new MappingException(Sofia.annotatedListenerMethod(listenerType.getName(), method.getName(),
                            annotationClass.getSimpleName()));
                }
            }
        }
        if (!TypedListeners.handlesAny(listenerType)) {
            throw new MappingException(Sofia.noLifecycleMethods(listenerType.getName()));
        }
        Constructor<? extends EntityListener<?>> constructor;
        try {
            constructor = listenerType.getDeclaredConstructor();
        } catch (NoSuchMethodException e) {
            throw new MappingException(Sofia.noargConstructorNotFound(listenerType), e);
        }
        try {
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (ReflectiveOperationException e) {
            Throwable cause = e instanceof InvocationTargetException target && target.getCause() != null ? target.getCause() : e;
            throw new MappingException(Sofia.cannotInstantiate(listenerType.getName(), cause.getMessage()), cause);
        }
    }

    /**
     * @param event the lifecycle event annotation
     * @return true if the entity handles the event, with an annotated method or its per-event listener interface
     */
    public boolean handles(@NonNull Class<? extends Annotation> event) {
        return !callbacks.get(event).isEmpty() || TypedListeners.handles(entityType, event);
    }

    /**
     * @param event      the lifecycle event annotation
     * @param entityType the type of the entity the model is for: the declaring entity or a subtype
     * @return true if a method for the event takes the {@code Document}: an annotated method with a {@code Document}
     *         parameter, or the event's listener interface's method if it accepts every entity of the type. One that
     *         accepts only a subtype doesn't count: a codec checks the subtype's own needs once it knows the entity is one
     */
    public boolean usesDocument(@NonNull Class<? extends Annotation> event, Class<?> entityType) {
        for (Callback callback : callbacks.get(event)) {
            if (callback.usesDocument()) {
                return true;
            }
        }
        return TypedListeners.usesDocument(this.entityType, event, entityType);
    }

    private boolean hasCallbacks() {
        for (List<Callback> list : callbacks.values()) {
            if (!list.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Calls the entity's annotated methods for the event, then its per-event listener interface's method.
     *
     * @param event     the lifecycle event annotation
     * @param entity    the entity
     * @param document  the document, or null when no method for the event takes one
     * @param datastore the datastore
     */
    public void invoke(Class<? extends Annotation> event, Object entity, @Nullable Document document, Datastore datastore) {
        for (Callback callback : callbacks.get(event)) {
            try {
                callback.method.invoke(entity, callback.arguments(entity, document, datastore));
            } catch (IllegalArgumentException | ReflectiveOperationException e) {
                throw failure(callback.method, entity, e);
            }
        }
        if (TypedListeners.handles(entityType, event)) {
            TypedListeners.invoke(entity, event, entity, document, datastore);
        }
    }

    private static RuntimeException failure(Method method, Object entity, Exception e) {
        if (e instanceof IllegalArgumentException && "object is not an instance of declaring class".equals(e.getMessage())) {
            return new RuntimeException(format("%s is not an instance of the method's type: %s",
                    entity.getClass().getName(), method.getDeclaringClass().getName()), e);
        }
        return new RuntimeException("Failed to invoke method: " + method, e);
    }

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

    private static List<Method> getDeclaredAndInheritedMethods(Class<?> type) {
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
