package dev.morphia;

import java.lang.annotation.Annotation;

import dev.morphia.annotations.PostLoad;
import dev.morphia.annotations.PostPersist;
import dev.morphia.annotations.PreLoad;
import dev.morphia.annotations.PrePersist;

import org.bson.Document;

/**
 * Defines a listener on an entity and default placeholders for the various types. The named methods below can be used for semantic
 * convenience or new methods maybe be defined if, e.g., you need multiple handlers of the same event. Any method defined on subtypes
 * must be annotated with a lifecycle event annotation. See the methods for each type below for details.
 *
 * @param <T> the listener type
 */
public interface EntityListener<T> {
    /**
     * This method checks for the presence of the given lifecycle event annotation on any of the methods of the type implementing this
     * interface.
     *
     * @param type the annotation type to check for
     * @return true if the annotation is found on a method on this type.
     */
    boolean hasAnnotation(Class<? extends Annotation> type);

    /**
     * @param entity    the entity being processed
     * @param document  the Document form of the entity
     * @param datastore the Datastore being used
     * @see PostLoad
     * @since 2.3
     */
    default void postLoad(T entity, Document document, Datastore datastore) {
    };

    /**
     * @param entity    the entity being processed
     * @param document  the Document form of the entity
     * @param datastore the Datastore being used
     * @see PostPersist
     * @since 2.3
     */
    default void postPersist(T entity, Document document, Datastore datastore) {
    };

    /**
     * @param entity    the entity being processed
     * @param document  the Document form of the entity
     * @param datastore the Datastore being used
     * @see PreLoad
     * @since 2.3
     */
    default void preLoad(T entity, Document document, Datastore datastore) {
    };

    /**
     * @param entity    the entity being processed
     * @param document  the Document form of the entity
     * @param datastore the Datastore being used
     * @see PrePersist
     * @since 2.3
     */
    default void prePersist(T entity, Document document, Datastore datastore) {
    };

}
