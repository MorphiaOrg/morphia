package org.mongodb.morphia;


import org.mongodb.morphia.dao.BasicDAO;
import com.mongodb.Mongo;


/**
 * @deprecated use org.mongodb.morphia.dao.BasicDAO
 */
public class DAO<T, K> extends BasicDAO<T, K> {
  public DAO(final Class<T> entityClass, final Mongo mongo, final Morphia morphia, final String dbName) {
    super(entityClass, mongo, morphia, dbName);
  }

  public DAO(final Class<T> entityClass, final Datastore ds) {
    super(entityClass, ds);
  }
}