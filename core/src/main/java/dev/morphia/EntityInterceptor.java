package dev.morphia;


import dev.morphia.annotations.PostLoad;
import dev.morphia.annotations.PostPersist;
import dev.morphia.annotations.PreLoad;
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
     */
    default void postLoad(Object ent, Document document, Mapper mapper) {
    }

    /**
     * @param ent      the entity being processed
     * @param document the Document form of the entity
     * @param mapper   the Mapper being used
     * @see PostPersist
     */
    default void postPersist(Object ent, Document document, Mapper mapper) {
    }

    /**
     * @param ent      the entity being processed
     * @param document the Document form of the entity
     * @param mapper   the Mapper being used
     * @see PreLoad
     */
    default void preLoad(Object ent, Document document, Mapper mapper) {
    }

    /**
     * @param ent      the entity being processed
     * @param document the Document form of the entity
     * @param mapper   the Mapper being used
     * @see PostPersist
     */
    default void prePersist(Object ent, Document document, Mapper mapper) {
    }
}
