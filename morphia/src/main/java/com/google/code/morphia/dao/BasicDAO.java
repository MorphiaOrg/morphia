package com.google.code.morphia.dao;


import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.List;

import com.google.code.morphia.Datastore;
import com.google.code.morphia.DatastoreImpl;
import com.google.code.morphia.Key;
import com.google.code.morphia.Morphia;
import com.google.code.morphia.query.Query;
import com.google.code.morphia.query.QueryResults;
import com.google.code.morphia.query.UpdateOperations;
import com.google.code.morphia.query.UpdateResults;
import com.mongodb.DBCollection;
import com.mongodb.MongoClient;
import com.mongodb.WriteConcern;
import com.mongodb.WriteResult;


/**
 * @author Olafur Gauti Gudmundsson
 * @author Scott Hernandez
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
public class BasicDAO<T, K> implements DAO<T, K> {

  protected Class<T>      entityClazz;
  protected DatastoreImpl ds;

  public BasicDAO(final Class<T> entityClass, final MongoClient mongo, final Morphia morphia, final String dbName) {
    initDS(mongo, morphia, dbName);
    initType(entityClass);
  }

  public BasicDAO(final Class<T> entityClass, final Datastore ds) {
    this.ds = (DatastoreImpl) ds;
    initType(entityClass);
  }

  /**
   * <p> Only calls this from your derived class when you explicitly declare the generic types with concrete classes </p> <p> {@code class
   * MyDao extends DAO<MyEntity, String>} </p>
   */
  protected BasicDAO(final MongoClient mongo, final Morphia morphia, final String dbName) {
    initDS(mongo, morphia, dbName);
    initType(((Class<T>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0]));
  }

  protected BasicDAO(final Datastore ds) {
    this.ds = (DatastoreImpl) ds;
    initType(((Class<T>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0]));
  }

  protected void initType(final Class<T> type) {
    entityClazz = type;
    ds.getMapper().addMappedClass(type);
  }

  protected void initDS(final MongoClient mon, final Morphia mor, final String db) {
    ds = new DatastoreImpl(mor, mon, db);
  }

  /**
   * Converts from a List<Key> to their id values
   */
  protected List<?> keysToIds(final List<Key<T>> keys) {
    final ArrayList ids = new ArrayList(keys.size() * 2);
    for (final Key<T> key : keys) {
      ids.add(key.getId());
    }
    return ids;
  }

  /**
   * The underlying collection for this DAO
   */
  public DBCollection getCollection() {
    return ds.getCollection(entityClazz);
  }


  /* (non-Javadoc)
   * @see com.google.code.morphia.DAO#createQuery()
   */
  public Query<T> createQuery() {
    return ds.createQuery(entityClazz);
  }

  /* (non-Javadoc)
   * @see com.google.code.morphia.DAO#createUpdateOperations()
   */
  public UpdateOperations<T> createUpdateOperations() {
    return ds.createUpdateOperations(entityClazz);
  }

  /* (non-Javadoc)
   * @see com.google.code.morphia.DAO#getEntityClass()
   */
  public Class<T> getEntityClass() {
    return entityClazz;
  }

  /* (non-Javadoc)
   * @see com.google.code.morphia.DAO#save(T)
   */
  public Key<T> save(final T entity) {
    return ds.save(entity);
  }

  /* (non-Javadoc)
   * @see com.google.code.morphia.DAO#save(T, com.mongodb.WriteConcern)
   */
  public Key<T> save(final T entity, final WriteConcern wc) {
    return ds.save(entity, wc);
  }

  /* (non-Javadoc)
   * @see com.google.code.morphia.DAO#updateFirst(com.google.code.morphia.query.Query, com.google.code.morphia.query.UpdateOperations)
   */
  public UpdateResults<T> updateFirst(final Query<T> q, final UpdateOperations<T> ops) {
    return ds.updateFirst(q, ops);
  }

  /* (non-Javadoc)
   * @see com.google.code.morphia.DAO#update(com.google.code.morphia.query.Query, com.google.code.morphia.query.UpdateOperations)
   */
  public UpdateResults<T> update(final Query<T> q, final UpdateOperations<T> ops) {
    return ds.update(q, ops);
  }

  /* (non-Javadoc)
   * @see com.google.code.morphia.DAO#delete(T)
   */
  public WriteResult delete(final T entity) {
    return ds.delete(entity);
  }

  /* (non-Javadoc)
   * @see com.google.code.morphia.DAO#delete(T, com.mongodb.WriteConcern)
   */
  public WriteResult delete(final T entity, final WriteConcern wc) {
    return ds.delete(entity, wc);
  }

  /* (non-Javadoc)
   * @see com.google.code.morphia.DAO#deleteById(K)
   */
  public WriteResult deleteById(final K id) {
    return ds.delete(entityClazz, id);
  }

  /* (non-Javadoc)
   * @see com.google.code.morphia.DAO#deleteByQuery(com.google.code.morphia.query.Query)
   */
  public WriteResult deleteByQuery(final Query<T> q) {
    return ds.delete(q);
  }

  /* (non-Javadoc)
   * @see com.google.code.morphia.DAO#get(K)
   */
  public T get(final K id) {
    return ds.get(entityClazz, id);
  }

  /* (non-Javadoc)
   * @see com.google.code.morphia.DAO#findIds(java.lang.String, java.lang.Object)
   */
  public List<K> findIds(final String key, final Object value) {
    return (List<K>) keysToIds(ds.find(entityClazz, key, value).asKeyList());
  }

  /* (non-Javadoc)
   * @see com.google.code.morphia.DAO#findIds()
   */
  public List<K> findIds() {
    return (List<K>) keysToIds(ds.find(entityClazz).asKeyList());
  }

  /* (non-Javadoc)
   * @see com.google.code.morphia.DAO#findIds(com.google.code.morphia.query.Query)
   */
  public List<K> findIds(final Query<T> q) {
    return (List<K>) keysToIds(q.asKeyList());
  }

  /* (non-Javadoc)
   * @see com.google.code.morphia.DAO#exists(java.lang.String, java.lang.Object)
   */
  public boolean exists(final String key, final Object value) {
    return exists(ds.find(entityClazz, key, value));
  }

  /* (non-Javadoc)
   * @see com.google.code.morphia.DAO#exists(com.google.code.morphia.query.Query)
   */
  public boolean exists(final Query<T> q) {
    return ds.getCount(q) > 0;
  }

  /* (non-Javadoc)
   * @see com.google.code.morphia.DAO#count()
   */
  public long count() {
    return ds.getCount(entityClazz);
  }

  /* (non-Javadoc)
   * @see com.google.code.morphia.DAO#count(java.lang.String, java.lang.Object)
   */
  public long count(final String key, final Object value) {
    return count(ds.find(entityClazz, key, value));
  }

  /* (non-Javadoc)
   * @see com.google.code.morphia.DAO#count(com.google.code.morphia.query.Query)
   */
  public long count(final Query<T> q) {
    return ds.getCount(q);
  }

  /* (non-Javadoc)
   * @see com.google.code.morphia.DAO#findOne(java.lang.String, java.lang.Object)
   */
  public T findOne(final String key, final Object value) {
    return ds.find(entityClazz, key, value).get();
  }

  /* (non-Javadoc)
   * @see com.google.code.morphia.DAO#findOne(com.google.code.morphia.query.Query)
   */
  public T findOne(final Query<T> q) {
    return q.get();
  }

  /* (non-Javadoc)
   * @see com.google.code.morphia.DAO#find()
   */
  public QueryResults<T> find() {
    return createQuery();
  }

  /* (non-Javadoc)
   * @see com.google.code.morphia.DAO#find(com.google.code.morphia.query.Query)
   */
  public QueryResults<T> find(final Query<T> q) {
    return q;
  }

  /* (non-Javadoc)
   * @see com.google.code.morphia.DAO#getDatastore()
   */
  public Datastore getDatastore() {
    return ds;
  }

  public void ensureIndexes() {
    ds.ensureIndexes(entityClazz);
  }

}
