package dev.morphia.mapping.codec.pojo;

import java.lang.annotation.Annotation;
import java.util.List;

import dev.morphia.EntityListener;
import dev.morphia.mapping.Mapper;

/**
 * Which lifecycle events an entity takes part in, and whether any listener for them needs the entity's {@code Document}.
 * An entity's own listeners are fixed once it's mapped; the mapper's global listeners are read as they are, since one
 * can be added at any time.
 */
final class LifecycleEvents {
    private LifecycleEvents() {
    }

    /**
     * @return true if the model or a global listener handles either event
     */
    static boolean handles(EntityModel model, Mapper mapper, Class<? extends Annotation> first, Class<? extends Annotation> second) {
        if (model.hasLifecycle(first) || model.hasLifecycle(second)) {
            return true;
        }
        List<EntityListener<?>> listeners = mapper.getListeners();
        // indexed: this runs for every value of every entity on the lifecycle codecs, and an iterator would allocate
        for (int i = 0; i < listeners.size(); i++) {
            EntityListener<?> listener = listeners.get(i);
            if (listener.hasAnnotation(first) || listener.hasAnnotation(second)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return true if a listener of the model, or a global listener, takes the {@code Document} for either event
     */
    static boolean needDocument(EntityModel model, Mapper mapper, Class<? extends Annotation> first,
            Class<? extends Annotation> second) {
        if (model.lifecycleUsesDocument(first) || model.lifecycleUsesDocument(second)) {
            return true;
        }
        List<EntityListener<?>> listeners = mapper.getListeners();
        for (int i = 0; i < listeners.size(); i++) {
            EntityListener<?> listener = listeners.get(i);
            if (listener.hasAnnotation(first) && listener.usesDocument(first)
                    || listener.hasAnnotation(second) && listener.usesDocument(second)) {
                return true;
            }
        }
        return false;
    }
}
