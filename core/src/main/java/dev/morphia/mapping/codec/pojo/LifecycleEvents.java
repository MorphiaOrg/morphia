package dev.morphia.mapping.codec.pojo;

import java.lang.annotation.Annotation;

import dev.morphia.EntityListener;
import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.lifecycle.TypedListeners;

/**
 * Whether a listener for an entity's lifecycle events needs the entity's {@code Document}. An entity's own listeners and
 * the configured global ones are fixed once the mapper exists, so a lifecycle codec decides when it's built.
 */
final class LifecycleEvents {
    private LifecycleEvents() {
    }

    /**
     * @param model  the entity's model
     * @param mapper the mapper
     * @param events the lifecycle events of one direction, encoding or decoding
     * @return true if one of the model's own listeners, or a configured global listener whose method applies to the
     *         model's entities, takes the {@code Document} for any of the events
     */
    @SafeVarargs
    static boolean needDocument(EntityModel model, Mapper mapper, Class<? extends Annotation>... events) {
        for (Class<? extends Annotation> event : events) {
            if (model.lifecycleUsesDocument(event)) {
                return true;
            }
            for (EntityListener<?> listener : mapper.getGlobalListeners()) {
                if (TypedListeners.usesDocument(listener.getClass(), event, model.getType())) {
                    return true;
                }
            }
        }
        return false;
    }
}
