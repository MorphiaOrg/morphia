package dev.morphia;

import dev.morphia.annotations.PrePersist;

/**
 * A listener for the {@link PrePersist} event, called before an entity is encoded. It takes no {@code Document}, so Morphia
 * can encode an entity without building one for it.
 * <p>
 * It's called only for entities of type {@code T}: a {@code PrePersistListener<Auditable>} sees {@code Auditable} entities alone, and a
 * {@code PrePersistListener<Object>} sees every entity.
 * <p>
 * A lambda can't carry its type argument: its class implements the raw interface, so it sees every entity, and a lambda
 * typed for a narrower entity fails with a {@code ClassCastException} when another entity is saved or loaded. Type a
 * lambda {@code PrePersistListener<Object>} and check the entity's type in its body, or use a named or anonymous class, which
 * keeps its type argument.
 *
 * @param <T> the type of the entities this listener accepts
 * @since 3.0
 */
public interface PrePersistListener<T> extends EntityListener<T> {
    /**
     * @param entity    the entity being saved
     * @param datastore the Datastore being used
     */
    void prePersist(T entity, Datastore datastore);
}
