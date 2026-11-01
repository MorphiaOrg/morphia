package dev.morphia.mapping.codec.pojo;

import java.lang.annotation.Annotation;
import java.util.List;

import dev.morphia.EntityListener;
import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.lifecycle.EntityListenerAdapter;

/**
 * Which lifecycle events an entity takes part in, and whether a listener for them needs the entity's {@code Document}.
 * An entity's own listeners and the configured global ones are fixed once the mapper exists, so a codec decides when
 * it's built. Listeners added with the deprecated {@code Mapper.addInterceptor} keep their old behavior: any of them puts
 * every entity on the lifecycle codecs, and they always get a {@code Document}.
 */
final class LifecycleEvents {
    private LifecycleEvents() {
    }

    /**
     * @return true if the model, a configured global listener, or any interceptor handles either event
     */
    static boolean handles(EntityModel model, Mapper mapper, Class<? extends Annotation> first, Class<? extends Annotation> second) {
        if (model.hasLifecycle(first) || model.hasLifecycle(second) || !mapper.getListeners().isEmpty()) {
            return true;
        }
        for (EntityListener<?> listener : mapper.getGlobalListeners()) {
            if (listener.hasAnnotation(first) || listener.hasAnnotation(second)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return true if a listener of the model, or a configured global listener, takes the {@code Document} for either
     *         event. Interceptors aren't counted: they can be added at any time, so the codecs check for them as they run.
     */
    static boolean needDocument(EntityModel model, Mapper mapper, Class<? extends Annotation> first,
            Class<? extends Annotation> second) {
        if (model.lifecycleUsesDocument(first) || model.lifecycleUsesDocument(second)) {
            return true;
        }
        List<EntityListener<?>> globals = mapper.getGlobalListeners();
        for (int i = 0; i < globals.size(); i++) {
            EntityListener<?> listener = globals.get(i);
            if (usesDocument(listener, first) || usesDocument(listener, second)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return true if the listener handles the event and takes the {@code Document} for it. A listener Morphia wraps, as
     *         it does an {@code @EntityListeners} class or a configured global one, says so in its methods' parameters;
     *         any other implementation of {@code EntityListener} is assumed to.
     */
    static boolean usesDocument(EntityListener<?> listener, Class<? extends Annotation> event) {
        return listener.hasAnnotation(event)
                && (!(listener instanceof EntityListenerAdapter adapter) || adapter.usesDocument(event));
    }
}
