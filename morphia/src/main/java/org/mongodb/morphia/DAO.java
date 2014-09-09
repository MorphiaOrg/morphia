package org.mongodb.morphia;


import com.mongodb.MongoClient;
import org.mongodb.morphia.dao.BasicDAO;


/**
 * @deprecated use org.mongodb.morphia.dao.BasicDAO
 */
public class DAO<T, K> extends BasicDAO<T, K> {
  public DAO(final Class<T> entityClass, final MongoClient mongoClient, final Morphia morphia, final String dbName) {
    super(entityClass, mongoClient, morphia, dbName);
  }

  public DAO(final Class<T> entityClass, final Datastore ds) {
    super(entityClass, ds);
  }
}