package dev.morphia;

import java.lang.annotation.Annotation;

import dev.morphia.annotations.PostLoad;
import dev.morphia.annotations.PostPersist;
import dev.morphia.annotations.PreLoad;
import dev.morphia.annotations.PrePersist;

import org.bson.Document;

public interface EntityListener<T> {
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
