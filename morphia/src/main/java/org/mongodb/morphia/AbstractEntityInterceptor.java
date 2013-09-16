package org.mongodb.morphia;


import org.mongodb.morphia.mapping.Mapper;
import com.mongodb.DBObject;


/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 */
public class AbstractEntityInterceptor implements EntityInterceptor {

  public void postLoad(final Object ent, final DBObject dbObj, final Mapper mapper) {
  }

  public void postPersist(final Object ent, final DBObject dbObj, final Mapper mapper) {
  }

  public void preLoad(final Object ent, final DBObject dbObj, final Mapper mapper) {
  }

  public void prePersist(final Object ent, final DBObject dbObj, final Mapper mapper) {
  }

  public void preSave(final Object ent, final DBObject dbObj, final Mapper mapper) {
  }
}
