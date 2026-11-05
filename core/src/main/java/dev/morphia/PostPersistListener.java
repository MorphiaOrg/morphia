package dev.morphia;

import dev.morphia.annotations.PostPersist;

/**
 * A listener for the {@link PostPersist} event, called after an entity is encoded. It takes no {@code Document}, so Morphia
 * can encode an entity without building one for it.
 * <p>
 * It's called only for entities of type {@code T}: a {@code PostPersistListener<Auditable>} sees {@code Auditable} entities alone, and a
 * {@code PostPersistListener<Object>} sees every entity.
 * <p>
 * A lambda can't carry its type argument: its class implements the raw interface, so it sees every entity, and a lambda
 * typed for a narrower entity fails with a {@code ClassCastException} when another entity is saved or loaded. Type a
 * lambda {@code PostPersistListener<Object>} and check the entity's type in its body, or use a named or anonymous class, which
 * keeps its type argument.
 *
 * @param <T> the type of the entities this listener accepts
 * @since 3.0
 */
public interface PostPersistListener<T> extends EntityListener<T> {
    /**
     * @param entity    the entity that was saved
     * @param datastore the Datastore being used
     */
    void postPersist(T entity, Datastore datastore);
}
