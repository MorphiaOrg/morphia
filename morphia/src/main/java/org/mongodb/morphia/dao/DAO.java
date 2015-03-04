package org.mongodb.morphia.dao;


import com.mongodb.DBCollection;
import com.mongodb.WriteConcern;
import com.mongodb.WriteResult;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.Key;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.QueryResults;
import org.mongodb.morphia.query.UpdateOperations;
import org.mongodb.morphia.query.UpdateResults;

import java.util.List;


/**
 * Defines a basic interface for use in applications
 * 
 * @param <T> The Java type serviced by this DAO
 * @param <K> The Key type used by the entity
 */
public interface DAO<T, K> {
    /**
     * Starts a query for this DAO entities type
     */
    Query<T> createQuery();

    /**
     * Starts a update-operations def for this DAO entities type
     */
    UpdateOperations<T> createUpdateOperations();

    /**
     * The type of entities for this DAO
     */
    Class<T> getEntityClass();

    /**
     * Saves the entity; either inserting or overriding the existing document
     */
    Key<T> save(T entity);

    /**
     * Saves the entity; either inserting or overriding the existing document
     */
    Key<T> save(T entity, WriteConcern wc);

    /**
     * Updates the first entity matched by the constraints with the modifiers supplied.
     */
    UpdateResults updateFirst(Query<T> q, UpdateOperations<T> ops);

    /**
     * Updates all entities matched by the constraints with the modifiers supplied.
     */
    UpdateResults update(Query<T> q, UpdateOperations<T> ops);

    /**
     * Deletes the entity
     */
    WriteResult delete(T entity);

    /**
     * Deletes the entity
     */
    WriteResult delete(T entity, WriteConcern wc);

    /**
     * Delete the entity by id value
     */
    WriteResult deleteById(K id);

    /**
     * Saves the entities given the query
     */
    WriteResult deleteByQuery(Query<T> q);

    /**
     * Loads the entity by id value
     */
    T get(K id);

    /**
     * Finds the entities Ts
     */
    List<K> findIds();

    /**
     * Finds the entities Key<T> by the criteria {key:value}
     */
    List<K> findIds(String key, Object value);

    /**
     * Finds the entities Ts by the criteria {key:value}
     */
    List<K> findIds(Query<T> q);

    /**
     * Finds the first entity's ID
     */
    Key<T> findOneId();

    /**
     * Finds the first entity's ID
     */
    Key<T> findOneId(String key, Object value);

    /**
     * Finds the first entity's ID
     */
    Key<T> findOneId(Query<T> q);

    /**
     * checks for entities which match criteria {key:value}
     */
    boolean exists(String key, Object value);

    /**
     * checks for entities which match the criteria
     */
    boolean exists(Query<T> q);

    /**
     * returns the total count
     */
    long count();

    /**
     * returns the count which match criteria {key:value}
     */
    long count(String key, Object value);

    /**
     * returns the count which match the criteria
     */
    long count(Query<T> q);

    /**
     * returns the entity which match criteria {key:value}
     */
    T findOne(String key, Object value);

    /**
     * returns the entity which match the criteria
     */
    T findOne(Query<T> q);

    /**
     * returns the entities
     */
    QueryResults<T> find();

    /**
     * returns the entities which match the criteria
     */
    QueryResults<T> find(Query<T> q);

    /**
     * ensures indexed for this DAO
     */
    void ensureIndexes();

    /**
     * gets the collection
     */
    DBCollection getCollection();

    /**
     * returns the underlying datastore
     */
    Datastore getDatastore();
}