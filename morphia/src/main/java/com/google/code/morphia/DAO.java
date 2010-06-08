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

/**
 *
 * @author Olafur Gauti Gudmundsson
 */
@SuppressWarnings("unchecked")
public class DAO<T,K extends Serializable> {

    protected Class<T> entityClazz;
    protected DatastoreImpl ds;

    public DAO( Class<T> entityClass, Mongo mongo, Morphia morphia, String dbName ) {
    	initDS(mongo, morphia, dbName);
    	initType(entityClass);
    }
    /** 
     * <p>Only calls this from your derived class when you explicitly declare the generic types with concrete classes </p>
     * <p>{@code class MyDao extends DAO<MyEntity, String>}</p> 
     * */
    protected DAO( Mongo mongo, Morphia morphia, String dbName ) {
    	initDS(mongo, morphia, dbName);
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
     * @param keys
     * @return
     */
    protected List<?> keysToIds (List<Key<T>> keys){
    	ArrayList ids = new ArrayList(keys.size()*2);
    	for(Key<T> key : keys)
    		ids.add(key.getId());
    	return ids;
    }
    
    protected DBCollection collection() {
        return ds.getCollection(entityClazz);
    }

    public Query<T> createQuery() {
    	return ds.createQuery(entityClazz);
    }
    public UpdateOperations createUpdateOperations() {
    	return ds.createUpdateOperations();
    }
    
    public Class<T> getEntityClass() {
        return entityClazz;
    }

    public void save( T entity ) {
    	ds.save(entity);
    }

    /**
     * Updates the first object matched by the constraints with the modifiers supplied.
     */
    public void updateFirst( Query q, UpdateOperations ops) {
    	ds.updateFirst(q, ops);
    }

    /**
     * Updates all objects matched by the constraints with the modifiers supplied.
     */
    public void update( Query q, UpdateOperations ops) {
    	ds.update(q, ops);
    }

    public void delete( T entity ) {
    	ds.delete(entity);
    }

    public void deleteById( K id ) {
    	ds.delete(entityClazz, id);
    }

    public void deleteByQuery( Query q ) {
    	ds.delete(q);
    }

    public T get( K id ) {
    	return ds.get(entityClazz, id);
    }

    public List<K> findIds( String key, Object value ) {
    	return (List<K>) keysToIds(ds.find(entityClazz, key, value).asKeyList());
    }

    public List<K> findIds() {
    	return (List<K>) keysToIds(ds.find(entityClazz).asKeyList());
    }
 
    public List<K> findIds( Query<T> q ) {
    	return (List<K>) keysToIds(q.asKeyList());
    }

    public boolean exists(String key, Object value) {
        return exists(ds.find(entityClazz, key, value));
    }
    public boolean exists(Query<T> q) {
        return ds.getCount(q) > 0;
    }

    public long count() {
        return ds.getCount(entityClazz);
    }
    public long count(String key, Object value) {
        return count(ds.find(entityClazz, key, value));
    }
    public long count(Query<T> q) {
        return ds.getCount(q);
    }

    public T findOne(String key, Object value) {
        return ds.find(entityClazz, key, value).get();
    }
    public T findOne( Query<T> q ) {
        return q.get();
    }

    public QueryResults<T> find() {
        return ds.find(entityClazz);
    }

    public QueryResults<T> find( Query<T> q ) {
    	return q;
    }

    public void dropCollection() {
    	ds.getCollection(entityClazz).drop();
    }
}
