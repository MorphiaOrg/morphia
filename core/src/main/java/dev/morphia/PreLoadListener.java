package dev.morphia;

import dev.morphia.annotations.PreLoad;

import org.bson.Document;

/**
 * A listener for the {@link PreLoad} event, called before an entity's properties are decoded. It's given the
 * {@code Document} being decoded, which it may inspect or change, so Morphia builds one for every entity it decodes while
 * such a listener is configured.
 * <p>
 * It's called only for entities of type {@code T}: a {@code PreLoadListener<Auditable>} sees {@code Auditable} entities alone, and a
 * {@code PreLoadListener<Object>} sees every entity.
 * <p>
 * A lambda can't carry its type argument: its class implements the raw interface, so it sees every entity, and a lambda
 * typed for a narrower entity fails with a {@code ClassCastException} when another entity is saved or loaded. Type a
 * lambda {@code PreLoadListener<Object>} and check the entity's type in its body, or use a named or anonymous class, which
 * keeps its type argument.
 *
 * @param <T> the type of the entities this listener accepts
 * @since 3.0
 */
public interface PreLoadListener<T> extends EntityListener<T> {
    /**
     * @param entity    the entity being loaded, before its properties are set
     * @param document  the document being decoded
     * @param datastore the Datastore being used
     */
    void preLoad(T entity, Document document, Datastore datastore);
}
