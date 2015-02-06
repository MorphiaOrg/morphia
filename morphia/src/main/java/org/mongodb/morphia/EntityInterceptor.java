package org.mongodb.morphia;


import com.mongodb.DBObject;
import org.mongodb.morphia.mapping.Mapper;


/**
 * Interface for intercepting @Entity lifecycle events
 */
public interface EntityInterceptor {
    /**
     * @see org.mongodb.morphia.annotations.PostPersist
     */
    void prePersist(Object ent, DBObject dbObj, Mapper mapper);

    /**
     * @see org.mongodb.morphia.annotations.PreSave
     */
    void preSave(Object ent, DBObject dbObj, Mapper mapper);

    /**
     * @see org.mongodb.morphia.annotations.PostPersist
     */
    void postPersist(Object ent, DBObject dbObj, Mapper mapper);

    /**
     * @see org.mongodb.morphia.annotations.PreLoad
     */
    void preLoad(Object ent, DBObject dbObj, Mapper mapper);

    /**
     * @see org.mongodb.morphia.annotations.PostLoad
     */
    void postLoad(Object ent, DBObject dbObj, Mapper mapper);
}
