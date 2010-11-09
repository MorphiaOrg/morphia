package com.google.code.morphia;

import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.List;

import com.google.code.morphia.query.Query;
import com.google.code.morphia.query.QueryResults;
import com.google.code.morphia.query.UpdateOperations;
import com.mongodb.DBCollection;
import com.mongodb.Mongo;
import com.mongodb.WriteConcern;

/**
 * @author Olafur Gauti Gudmundsson
 * @author Scott Hernandez
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
public class DAO<T, K extends Serializable> {
	
	protected Class<T> entityClazz;
	protected DatastoreImpl ds;
	
	public DAO(Class<T> entityClass, Mongo mongo, Morphia morphia, String dbName) {
		initDS(mongo, morphia, dbName);
		initType(entityClass);
	}
	
	public DAO(Class<T> entityClass, Datastore ds) {
		this.ds = (DatastoreImpl) ds;
		initType(entityClass);
	}
	
	/**
	 * <p> Only calls this from your derived class when you explicitly declare the generic types with concrete classes </p>
	 * <p>
	 * {@code class MyDao extends DAO<MyEntity, String>}
	 * </p>
	 * */
	protected DAO(Mongo mongo, Morphia morphia, String dbName) {
		initDS(mongo, morphia, dbName);
		initType(((Class<T>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0]));
	}
	
	protected DAO(Datastore ds) {
		this.ds = (DatastoreImpl) ds;
		initType(((Class<T>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0]));
	}
	
	protected void initType(Class<T> type) {
		this.entityClazz = type;
		ds.getMapper().addMappedClass(type);
	}
	
	protected void initDS(Mongo mon, Morphia mor, String db) {
		ds = new DatastoreImpl(mor, mon, db);
	}
	
	/**
	 * Converts from a List<Key> to their id values
	 * 
	 * @param keys
	 * @return
	 */
	protected List<?> keysToIds(List<Key<T>> keys) {
		ArrayList ids = new ArrayList(keys.size() * 2);
		for (Key<T> key : keys)
			ids.add(key.getId());
		return ids;
	}
	
	/** The underlying collection for this DAO */
	protected DBCollection collection() {
		return ds.getCollection(entityClazz);
	}

	
	/** Starts a query for this DAO entities type*/
	public Query<T> createQuery() {
		return ds.createQuery(entityClazz);
	}
	
	/** Starts a update-operations def for this DAO entities type*/
	public UpdateOperations<T> createUpdateOperations() {
		return ds.createUpdateOperations(entityClazz);
	}

	/** The type of entities for this DAO*/
	public Class<T> getEntityClass() {
		return entityClazz;
	}
	
	/** Saves the entity; either inserting or overriding the existing document */
	public void save(T entity) {
		ds.save(entity);
	}
	
	/** Saves the entity; either inserting or overriding the existing document */
	public void save(T entity, WriteConcern wc) {
		ds.save(entity, wc);
	}
	/** Updates the first entity matched by the constraints with the modifiers supplied.*/
	public void updateFirst(Query q, UpdateOperations ops) {
		ds.updateFirst(q, ops);
	}
	
	/** Updates all entities matched by the constraints with the modifiers supplied.*/
	public void update(Query q, UpdateOperations ops) {
		ds.update(q, ops);
	}
	
	/** Deletes the entity */
	public void delete(T entity) {
		ds.delete(entity);
	}

	/** Deletes the entity */
	public void delete(T entity, WriteConcern wc) {
		ds.delete(entity, wc);
	}

	/** Delete the entity by id value */
	public void deleteById(K id) {
		ds.delete(entityClazz, id);
	}
	
	/** Saves the entities given the query*/
	public void deleteByQuery(Query q) {
		ds.delete(q);
	}
	
	/** Loads the entity by id value*/
	public T get(K id) {
		return ds.get(entityClazz, id);
	}
	
	/** Finds the entities Key<T> by the criteria {key:value}*/
	public List<T> findIds(String key, Object value) {
		return (List<T>) keysToIds(ds.find(entityClazz, key, value).asKeyList());
	}
	
	/** Finds the entities Key<T>s*/
	public List<Key<T>> findIds() {
		return (List<Key<T>>) keysToIds(ds.find(entityClazz).asKeyList());
	}
	
	/** Finds the entities Key<T>s by the criteria {key:value}*/
	public List<Key<T>> findIds(Query<T> q) {
		return (List<Key<T>>) keysToIds(q.asKeyList());
	}
	
	/** checks for entities which match criteria {key:value}*/
	public boolean exists(String key, Object value) {
		return exists(ds.find(entityClazz, key, value));
	}
	
	/** checks for entities which match the criteria*/
	public boolean exists(Query<T> q) {
		return ds.getCount(q) > 0;
	}
	
	/** returns the total count*/
	public long count() {
		return ds.getCount(entityClazz);
	}
	
	/** returns the count which match criteria {key:value}*/
	public long count(String key, Object value) {
		return count(ds.find(entityClazz, key, value));
	}
	
	/** returns the count which match the criteria*/
	public long count(Query<T> q) {
		return ds.getCount(q);
	}
	
	/** returns the entity which match criteria {key:value}*/
	public T findOne(String key, Object value) {
		return ds.find(entityClazz, key, value).get();
	}
	
	/** returns the entity which match the criteria */
	public T findOne(Query<T> q) {
		return q.get();
	}
	
	/** returns the entities */
	public QueryResults<T> find() {
		return createQuery();
	}
	
	/** returns the entities which match the criteria */
	public QueryResults<T> find(Query<T> q) {
		return q;
	}
	
	/** drops the collection */
	public void dropCollection() {
		ds.getCollection(entityClazz).drop();
	}
	
	/** returns the underlying datastore */
	public Datastore getDatastore() {
		return this.ds;
	}
}
