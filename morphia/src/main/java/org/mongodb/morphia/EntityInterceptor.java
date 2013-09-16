package org.mongodb.morphia;


import org.mongodb.morphia.annotations.PostLoad;
import org.mongodb.morphia.annotations.PostPersist;
import org.mongodb.morphia.annotations.PreLoad;
import org.mongodb.morphia.annotations.PrePersist;
import org.mongodb.morphia.annotations.PreSave;
import org.mongodb.morphia.mapping.Mapper;
import com.mongodb.DBObject;


/**
 * Interface for intercepting @Entity lifecycle events
 */
public interface EntityInterceptor {
  /**
   * see {@link PrePersist}
   */
  void prePersist(Object ent, DBObject dbObj, Mapper mapper);

  /**
   * see {@link PreSave}
   */
  void preSave(Object ent, DBObject dbObj, Mapper mapper);

  /**
   * see {@link PostPersist}
   */
  void postPersist(Object ent, DBObject dbObj, Mapper mapper);

  /**
   * see {@link PreLoad}
   */
  void preLoad(Object ent, DBObject dbObj, Mapper mapper);

  /**
   * see {@link PostLoad}
   */
  void postLoad(Object ent, DBObject dbObj, Mapper mapper);
}
