package dev.morphia.mapping.lifecycle;

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.HashMap;
import java.util.Map;

import com.mongodb.lang.Nullable;

import dev.morphia.Datastore;
import dev.morphia.PostLoadDocumentListener;
import dev.morphia.PostLoadListener;
import dev.morphia.PostPersistDocumentListener;
import dev.morphia.PostPersistListener;
import dev.morphia.PreLoadListener;
import dev.morphia.PrePersistListener;
import dev.morphia.annotations.PostLoad;
import dev.morphia.annotations.PostPersist;
import dev.morphia.annotations.PreLoad;
import dev.morphia.annotations.PrePersist;
import dev.morphia.annotations.internal.MorphiaInternal;
import dev.morphia.mapping.MappingException;
import dev.morphia.sofia.Sofia;

import org.bson.Document;

/**
 * Calls the per-event listener interfaces, {@link PrePersistListener} and the rest. A listener may implement only one interface
 * for each event: {@link PostPersistListener} or {@link PostPersistDocumentListener}, and {@link PostLoadListener} or
 * {@link PostLoadDocumentListener}. A listener is called for an event only
 * with entities of the interface's type argument: a {@code PrePersistListener<Auditable>} sees {@code Auditable} entities
 * alone. A raw implementation sees every entity. That includes every lambda, whose class implements the raw interface
 * whatever its target type, so a lambda typed for a narrower entity fails in its bridge method's cast when it's given
 * another entity; the interfaces' javadoc tells users to type lambdas {@code Object}.
 *
 * @hidden
 * @morphia.internal
 * @since 3.0
 */
@MorphiaInternal
@SuppressWarnings({ "rawtypes", "unchecked" })
public final class TypedListeners {
    // each event's method, by listener class; an event the class doesn't handle is absent. throws MappingException for a
    // class that implements two interfaces for one event
    private static final ClassValue<Map<Class<? extends Annotation>, Handler>> HANDLERS = new ClassValue<>() {
        @Override
        protected Map<Class<? extends Annotation>, Handler> computeValue(Class<?> type) {
            Map<Class<? extends Annotation>, Handler> handlers = new HashMap<>();
            accept(handlers, type, PrePersist.class, PrePersistListener.class, false);
            accept(handlers, type, PostPersist.class, PostPersistListener.class, false);
            accept(handlers, type, PostPersist.class, PostPersistDocumentListener.class, true);
            accept(handlers, type, PreLoad.class, PreLoadListener.class, true);
            accept(handlers, type, PostLoad.class, PostLoadListener.class, false);
            accept(handlers, type, PostLoad.class, PostLoadDocumentListener.class, true);
            return Map.copyOf(handlers);
        }
    };

    private TypedListeners() {
    }

    /**
     * @param listenerType a listener class
     * @param event        the lifecycle event annotation
     * @return true if the class implements the event's listener interface
     */
    public static boolean handles(Class<?> listenerType, Class<? extends Annotation> event) {
        return HANDLERS.get(listenerType).containsKey(event);
    }

    /**
     * @param listenerType a listener class
     * @param event        the lifecycle event annotation
     * @param entityType   an entity type
     * @return true if the class's method for the event may be called for entities of the type or of one of its subtypes
     */
    public static boolean handles(Class<?> listenerType, Class<? extends Annotation> event, Class<?> entityType) {
        Handler handler = HANDLERS.get(listenerType).get(event);
        return handler != null && handler.mayAccept(entityType);
    }

    /**
     * @param listenerType a listener class
     * @param event        the lifecycle event annotation
     * @return true if the class's method for the event takes the {@code Document}: it implements {@link PreLoadListener},
     *         {@link PostPersistDocumentListener} or {@link PostLoadDocumentListener} for the event
     */
    public static boolean usesDocument(Class<?> listenerType, Class<? extends Annotation> event) {
        Handler handler = HANDLERS.get(listenerType).get(event);
        return handler != null && handler.document();
    }

    /**
     * @param listenerType a listener class
     * @param event        the lifecycle event annotation
     * @param entityType   an entity type
     * @return true if the class's method for the event takes the {@code Document} and may be called for entities of the
     *         type or of one of its subtypes
     */
    public static boolean usesDocument(Class<?> listenerType, Class<? extends Annotation> event, Class<?> entityType) {
        Handler handler = HANDLERS.get(listenerType).get(event);
        return handler != null && handler.document() && handler.mayAccept(entityType);
    }

    /**
     * @param listenerType a listener class
     * @return true if the class implements any of the per-event listener interfaces
     */
    public static boolean handlesAny(Class<?> listenerType) {
        return !HANDLERS.get(listenerType).isEmpty();
    }

    /**
     * Calls the listener's method for the event if it implements the event's interface and accepts the entity.
     *
     * @param listener  the listener
     * @param event     the lifecycle event annotation
     * @param entity    the entity
     * @param document  the document, passed only to a method that takes one, and null when no method for the event does
     * @param datastore the datastore
     */
    public static void invoke(Object listener, Class<? extends Annotation> event, Object entity, @Nullable Document document,
            Datastore datastore) {
        Handler handler = HANDLERS.get(listener.getClass()).get(event);
        if (handler == null || !handler.entityType().isInstance(entity)) {
            return;
        }
        if (event.equals(PrePersist.class)) {
            ((PrePersistListener) listener).prePersist(entity, datastore);
        } else if (event.equals(PostPersist.class)) {
            if (handler.document()) {
                ((PostPersistDocumentListener) listener).postPersist(entity, document, datastore);
            } else {
                ((PostPersistListener) listener).postPersist(entity, datastore);
            }
        } else if (event.equals(PreLoad.class)) {
            ((PreLoadListener) listener).preLoad(entity, document, datastore);
        } else if (event.equals(PostLoad.class)) {
            if (handler.document()) {
                ((PostLoadDocumentListener) listener).postLoad(entity, document, datastore);
            } else {
                ((PostLoadListener) listener).postLoad(entity, datastore);
            }
        }
    }

    private static void accept(Map<Class<? extends Annotation>, Handler> handlers, Class<?> listenerType,
            Class<? extends Annotation> event, Class<?> eventInterface, boolean document) {
        if (eventInterface.isAssignableFrom(listenerType)) {
            Handler other = handlers.get(event);
            if (other != null) {
                throw new MappingException(Sofia.listenerBothInterfaces(listenerType.getName(),
                        other.eventInterface().getSimpleName(), eventInterface.getSimpleName()));
            }
            Type argument = typeArgument(listenerType, eventInterface, Map.of());
            handlers.put(event, new Handler(eventInterface, argument != null ? erase(argument) : Object.class, document));
        }
    }

    /**
     * Finds what {@code type} binds the event interface's type parameter to, following superclasses and superinterfaces.
     *
     * @return the type argument, or null if the interface is implemented raw
     */
    @Nullable
    private static Type typeArgument(Type type, Class<?> eventInterface, Map<TypeVariable<?>, Type> bindings) {
        Class<?> raw;
        Map<TypeVariable<?>, Type> resolved = new HashMap<>();
        if (type instanceof ParameterizedType parameterized) {
            raw = (Class<?>) parameterized.getRawType();
            bind(parameterized, bindings, resolved);
        } else if (type instanceof Class<?> c) {
            raw = c;
        } else {
            return null;
        }
        if (raw.equals(eventInterface)) {
            return resolved.get(eventInterface.getTypeParameters()[0]);
        }
        for (Type parent : raw.getGenericInterfaces()) {
            Type argument = typeArgument(parent, eventInterface, resolved);
            if (argument != null) {
                return argument;
            }
        }
        Type superclass = raw.getGenericSuperclass();
        return superclass != null ? typeArgument(superclass, eventInterface, resolved) : null;
    }

    /**
     * Binds a parameterized type's type parameters to its arguments, and those of its enclosing types: an inner class of
     * {@code Outer<Plain>} may implement {@code PrePersistListener<E>} with {@code Outer}'s {@code E}.
     */
    private static void bind(ParameterizedType type, Map<TypeVariable<?>, Type> bindings, Map<TypeVariable<?>, Type> resolved) {
        if (type.getOwnerType() instanceof ParameterizedType owner) {
            bind(owner, bindings, resolved);
        }
        TypeVariable<?>[] parameters = ((Class<?>) type.getRawType()).getTypeParameters();
        Type[] arguments = type.getActualTypeArguments();
        for (int i = 0; i < parameters.length; i++) {
            Type argument = arguments[i];
            resolved.put(parameters[i], argument instanceof TypeVariable<?> variable && bindings.containsKey(variable)
                    ? bindings.get(variable)
                    : argument);
        }
    }

    /**
     * A listener class's method for an event.
     *
     * @param eventInterface the interface it implements for the event
     * @param entityType     the entities it accepts
     * @param document       whether it takes the {@code Document}
     */
    private record Handler(Class<?> eventInterface, Class<?> entityType, boolean document) {
        /**
         * @return true if the method may be called for entities of the type: they're all accepted, or a subtype is
         */
        boolean mayAccept(Class<?> type) {
            return entityType.isAssignableFrom(type) || type.isAssignableFrom(entityType);
        }
    }

    private static Class<?> erase(Type type) {
        if (type instanceof Class<?> c) {
            return c;
        } else if (type instanceof ParameterizedType parameterized) {
            return (Class<?>) parameterized.getRawType();
        } else if (type instanceof TypeVariable<?> variable) {
            return erase(variable.getBounds()[0]);
        } else if (type instanceof WildcardType wildcard) {
            return erase(wildcard.getUpperBounds()[0]);
        }
        return Object.class;
    }
}
