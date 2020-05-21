package dev.morphia;


import com.mongodb.DBObject;
import dev.morphia.annotations.PostLoad;
import dev.morphia.annotations.PostPersist;
import dev.morphia.annotations.PreLoad;
import dev.morphia.annotations.PreSave;
import dev.morphia.mapping.Mapper;


/**
 * Interface for intercepting @Entity lifecycle events
 */
public interface EntityInterceptor {
    /**
     * @param ent    the entity being processed
     * @param dbObj  the DBObject form of the entity
     * @param mapper the Mapper being used
     * @see PostLoad
     */
    void postLoad(Object ent, DBObject dbObj, Mapper mapper);

    /**
     * @param ent    the entity being processed
     * @param dbObj  the DBObject form of the entity
     * @param mapper the Mapper being used
     * @see PostPersist
     */
    void postPersist(Object ent, DBObject dbObj, Mapper mapper);

    /**
     * @param ent    the entity being processed
     * @param dbObj  the DBObject form of the entity
     * @param mapper the Mapper being used
     * @see PreLoad
     */
    void preLoad(Object ent, DBObject dbObj, Mapper mapper);

    /**
     * @param ent    the entity being processed
     * @param dbObj  the DBObject form of the entity
     * @param mapper the Mapper being used
     * @see PostPersist
     */
    void prePersist(Object ent, DBObject dbObj, Mapper mapper);

    /**
     * @param ent    the entity being processed
     * @param dbObj  the DBObject form of the entity
     * @param mapper the Mapper being used
     * @see PreSave
     * @deprecated removed in 2.0
     */
    void preSave(Object ent, DBObject dbObj, Mapper mapper);
}
