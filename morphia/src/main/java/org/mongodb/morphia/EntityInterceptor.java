package org.mongodb.morphia;


import com.mongodb.DBObject;
import org.mongodb.morphia.annotations.PostLoad;
import org.mongodb.morphia.annotations.PostPersist;
import org.mongodb.morphia.annotations.PreLoad;
import org.mongodb.morphia.annotations.PreSave;
import org.mongodb.morphia.mapping.Mapper;


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
     */
    void preSave(Object ent, DBObject dbObj, Mapper mapper);
}
