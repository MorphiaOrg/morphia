package com.google.code.morphia;


import com.google.code.morphia.dao.BasicDAO;
import com.mongodb.MongoClient;


/**
 * @deprecated use dao.BasicDAO
 */
public class DAO<T, K> extends BasicDAO<T, K> {
  public DAO(final Class<T> entityClass, final MongoClient mongo, final Morphia morphia, final String dbName) {
    super(entityClass, mongo, morphia, dbName);
  }

  public DAO(final Class<T> entityClass, final Datastore ds) {
    super(entityClass, ds);
  }
}