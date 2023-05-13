package dev.morphia;

import java.lang.annotation.Annotation;

import dev.morphia.annotations.PostLoad;
import dev.morphia.annotations.PostPersist;
import dev.morphia.annotations.PreLoad;
import dev.morphia.annotations.PrePersist;
import dev.morphia.mapping.Mapper;

import org.bson.Document;

public interface EntityListener<T> {
    /**
     * @param entity   the entity being processed
     * @param document the Document form of the entity
     * @param mapper   the Mapper being used
     * @see PostLoad
     * @deprecated use {@link #postPersist(T, Document, Datastore)} instead and access the Mapper via {@link Datastore#getMapper()} if
     */
    @Deprecated(forRemoval = true)
    default void postLoad(T entity, Document document, Mapper mapper) {
    }

    boolean hasAnnotation(Class<? extends Annotation> type);

    /**
     * @param entity    the entity being processed
     * @param document  the Document form of the entity
     * @param datastore the Datastore being used
     * @see PostLoad
     * @since 2.3
     */
    default void postLoad(T entity, Document document, Datastore datastore) {
        postLoad(entity, document, datastore.getMapper());
    }

    /**
     * @param entity   the entity being processed
     * @param document the Document form of the entity
     * @param mapper   the Mapper being used
     * @see PostPersist
     * @deprecated use {@link #postPersist(Object, Document, Datastore)} instead and access the Mapper via {@link Datastore#getMapper()} if
     */
    @Deprecated(forRemoval = true)
    default void postPersist(T entity, Document document, Mapper mapper) {
    }

    /**
     * @param entity    the entity being processed
     * @param document  the Document form of the entity
     * @param datastore the Datastore being used
     * @see PostPersist
     * @since 2.3
     */
    default void postPersist(T entity, Document document, Datastore datastore) {
        postPersist(entity, document, datastore.getMapper());
    }

    /**
     * @param entity   the entity being processed
     * @param document the Document form of the entity
     * @param mapper   the Mapper being used
     * @see PreLoad
     * @deprecated use {@link #preLoad(T, Document, Datastore)} instead and access the Mapper via {@link Datastore#getMapper()} if
     *             necessary
     */
    @Deprecated(forRemoval = true)
    default void preLoad(T entity, Document document, Mapper mapper) {
    }

    /**
     * @param entity    the entity being processed
     * @param document  the Document form of the entity
     * @param datastore the Datastore being used
     * @see PreLoad
     * @since 2.3
     */
    default void preLoad(T entity, Document document, Datastore datastore) {
        preLoad(entity, document, datastore.getMapper());
    }

    /**
     * @param entity   the entity being processed
     * @param document the Document form of the entity
     * @param mapper   the Mapper being used
     * @see PrePersist
     * @deprecated use {@link #prePersist(T, Document, Datastore)} instead and access the Mapper via {@link Datastore#getMapper()} if
     *             necessary
     */
    @Deprecated(forRemoval = true)
    default void prePersist(T entity, Document document, Mapper mapper) {
    }

    /**
     * @param entity    the entity being processed
     * @param document  the Document form of the entity
     * @param datastore the Datastore being used
     * @see PrePersist
     * @since 2.3
     */
    default void prePersist(T entity, Document document, Datastore datastore) {
        prePersist(entity, document, datastore.getMapper());
    }

}
