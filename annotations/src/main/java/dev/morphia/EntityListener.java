package dev.morphia;

/**
 * Marks a lifecycle listener. A listener implements one or more of the per-event interfaces, {@code PrePersistListener},
 * {@code PostPersistListener}, {@code PreLoadListener} and {@code PostLoadListener}, and Morphia calls it for those events
 * alone. A class listed in {@link dev.morphia.annotations.EntityListeners @EntityListeners} must implement at least one of them;
 * only an entity's own methods may be annotated with a lifecycle event annotation.
 *
 * @param <T> the type of the entities the listener accepts
 */
public interface EntityListener<T> {
}
