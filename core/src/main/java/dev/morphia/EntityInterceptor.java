package dev.morphia;


import dev.morphia.annotations.PostLoad;
import dev.morphia.annotations.PostPersist;
import dev.morphia.annotations.PreLoad;
import dev.morphia.annotations.PrePersist;
import dev.morphia.mapping.Mapper;
import org.bson.Document;


/**
 * Interface for intercepting @Entity lifecycle events
 */
@SuppressWarnings("unused")
public interface EntityInterceptor {
    /**
     * @param ent      the entity being processed
     * @param document the Document form of the entity
     * @param mapper   the Mapper being used
     * @see PostLoad
     * @deprecated use {@link #postPersist(Object, Document, Datastore)} instead and access the Mapper via {@link Datastore#getMapper()} if
     */
    @Deprecated(forRemoval = true)
    default void postLoad(Object ent, Document document, Mapper mapper) {
    }

    /**
     * @param ent       the entity being processed
     * @param document  the Document form of the entity
     * @param datastore the Datastore being used
     * @see PostLoad
     * @since 2.3
     */
    default void postLoad(Object ent, Document document, Datastore datastore) {
    }

    /**
     * @param ent      the entity being processed
     * @param document the Document form of the entity
     * @param mapper   the Mapper being used
     * @see PostPersist
     * @deprecated use {@link #postPersist(Object, Document, Datastore)} instead and access the Mapper via {@link Datastore#getMapper()} if
     */
    @Deprecated(forRemoval = true)
    default void postPersist(Object ent, Document document, Mapper mapper) {
    }

    /**
     * @param ent       the entity being processed
     * @param document  the Document form of the entity
     * @param datastore the Datastore being used
     * @see PostPersist
     * @since 2.3
     */
    default void postPersist(Object ent, Document document, Datastore datastore) {
    }

    /**
     * @param ent      the entity being processed
     * @param document the Document form of the entity
     * @param mapper   the Mapper being used
     * @see PreLoad
     * @deprecated use {@link #preLoad(Object, Document, Datastore)} instead and access the Mapper via {@link Datastore#getMapper()} if
     * necessary
     */
    @Deprecated(forRemoval = true)
    default void preLoad(Object ent, Document document, Mapper mapper) {
    }

    /**
     * @param ent       the entity being processed
     * @param document  the Document form of the entity
     * @param datastore the Datastore being used
     * @see PreLoad
     * @since 2.3
     */
    default void preLoad(Object ent, Document document, Datastore datastore) {
    }

    /**
     * @param ent      the entity being processed
     * @param document the Document form of the entity
     * @param mapper   the Mapper being used
     * @see PrePersist
     * @deprecated use {@link #prePersist(Object, Document, Datastore)} instead and access the Mapper via {@link Datastore#getMapper()} if
     * necessary
     */
    @Deprecated(forRemoval = true)
    default void prePersist(Object ent, Document document, Mapper mapper) {
    }

    /**
     * @param ent       the entity being processed
     * @param document  the Document form of the entity
     * @param datastore the Datastore being used
     * @see PrePersist
     * @since 2.3
     */
    default void prePersist(Object ent, Document document, Datastore datastore) {
    }
}
