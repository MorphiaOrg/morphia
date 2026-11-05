package dev.morphia;

import dev.morphia.annotations.PostPersist;

import org.bson.Document;

/**
 * A listener for the {@link PostPersist} event, called after an entity is encoded and before it's written, that's also
 * given the encoded {@code Document}. It may change the document, to add fields or sign the encoded content, and what's
 * written includes those changes. Morphia builds a {@code Document} for every entity it encodes while such a listener
 * applies, so implement {@link PostPersistListener} instead unless the document is needed. A class may implement only one
 * of the two.
 * <p>
 * It's called only for entities of type {@code T}: a {@code PostPersistDocumentListener<Auditable>} sees
 * {@code Auditable} entities alone, and a {@code PostPersistDocumentListener<Object>} sees every entity.
 * <p>
 * A lambda can't carry its type argument: its class implements the raw interface, so it sees every entity, and a lambda
 * typed for a narrower entity fails with a {@code ClassCastException} when another entity is saved or loaded. Type a
 * lambda {@code PostPersistDocumentListener<Object>} and check the entity's type in its body, or use a named or anonymous
 * class, which keeps its type argument.
 *
 * @param <T> the type of the entities this listener accepts
 * @since 3.0
 */
public interface PostPersistDocumentListener<T> extends EntityListener<T> {
    /**
     * @param entity    the entity that was encoded
     * @param document  the encoded document, which is written once every listener has run
     * @param datastore the Datastore being used
     */
    void postPersist(T entity, Document document, Datastore datastore);
}
