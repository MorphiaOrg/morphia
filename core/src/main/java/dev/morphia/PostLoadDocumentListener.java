package dev.morphia;

import dev.morphia.annotations.PostLoad;

import org.bson.Document;

/**
 * A listener for the {@link PostLoad} event, called after an entity is decoded, that's also given the {@code Document} it was
 * decoded from: to read fields the entity doesn't map, or to compare the entity with what was stored. Morphia builds a
 * {@code Document} for every entity it decodes while such a listener applies, so implement {@link PostLoadListener}
 * instead unless the document is needed. A class may implement only one of the two.
 * <p>
 * It's called only for entities of type {@code T}: a {@code PostLoadDocumentListener<Auditable>} sees {@code Auditable}
 * entities alone, and a {@code PostLoadDocumentListener<Object>} sees every entity.
 * <p>
 * A lambda can't carry its type argument: its class implements the raw interface, so it sees every entity, and a lambda
 * typed for a narrower entity fails with a {@code ClassCastException} when another entity is saved or loaded. Type a
 * lambda {@code PostLoadDocumentListener<Object>} and check the entity's type in its body, or use a named or anonymous
 * class, which keeps its type argument.
 *
 * @param <T> the type of the entities this listener accepts
 * @since 3.0
 */
public interface PostLoadDocumentListener<T> extends EntityListener<T> {
    /**
     * @param entity    the entity that was loaded
     * @param document  the document it was decoded from
     * @param datastore the Datastore being used
     */
    void postLoad(T entity, Document document, Datastore datastore);
}
