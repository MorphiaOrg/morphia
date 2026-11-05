package dev.morphia;

import dev.morphia.annotations.PostLoad;

/**
 * A listener for the {@link PostLoad} event, called after an entity is decoded. It takes no {@code Document}, so Morphia
 * can decode an entity without building one for it.
 * <p>
 * It's called only for entities of type {@code T}: a {@code PostLoadListener<Auditable>} sees {@code Auditable} entities alone, and a
 * {@code PostLoadListener<Object>} sees every entity.
 *
 * @param <T> the type of the entities this listener accepts
 * @since 3.0
 */
public interface PostLoadListener<T> extends EntityListener<T> {
    /**
     * @param entity    the entity that was loaded
     * @param datastore the Datastore being used
     */
    void postLoad(T entity, Datastore datastore);
}
