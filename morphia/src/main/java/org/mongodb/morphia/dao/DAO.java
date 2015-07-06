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
     * @return the total count
     */
    long count();

    /**
     * @param key   The key to search with
     * @param value the value to look for
     * @return the count which match criteria {key:value}
     */
    long count(String key, Object value);

    /**
     * @param query the query to use when counting
     * @return the count which match the criteria
     */
    long count(Query<T> query);

    /**
     * Starts a query for this DAO entities type
     *
     * @return the query
     */
    Query<T> createQuery();

    /**
     * Starts a update-operations def for this DAO entities type
     *
     * @return a new empty UpdateOperations instance
     */
    UpdateOperations<T> createUpdateOperations();

    /**
     * Deletes an entity
     *
     * @param entity the entity to delete
     * @return the results of the deletion
     * @see WriteResult
     */
    WriteResult delete(T entity);

    /**
     * Deletes an entity
     *
     * @param entity the entity to delete
     * @param wc     the WriteConcern to use when deleting
     * @return the results of the deletion
     * @see WriteConcern
     * @see WriteResult
     */
    WriteResult delete(T entity, WriteConcern wc);

    /**
     * Delete the entity by id value
     *
     * @param id the ID of the document to delete
     * @return the results of the deletion
     * @see WriteResult
     */
    WriteResult deleteById(K id);

    /**
     * Delete the entity matching a query
     *
     * @param query the query to use when finding the documents to delete
     * @return the results of the deletion
     * @see WriteResult
     */
    WriteResult deleteByQuery(Query<T> query);

    /**
     * ensures indexed for this DAO
     */
    void ensureIndexes();

    /**
     * checks for entities which match criteria {key:value}
     *
     * @param key   the key to query
     * @param value the value to search for
     * @return true if a document is found with a key matching the value
     */
    boolean exists(String key, Object value);

    /**
     * checks for entities which match the criteria
     *
     * @param query the query to use when finding the documents
     * @return true if a document is found matching the query
     */
    boolean exists(Query<T> query);

    /**
     * Finds all the documents in the collection mapped by the entity class
     *
     * @return the entities
     * @see #getEntityClass()
     */
    QueryResults<T> find();

    /**
     * Finds entities matching a query
     *
     * @param query the query to use when finding the documents
     * @return the entities which match the criteria
     */
    QueryResults<T> find(Query<T> query);

    /**
     * Finds the entities Ts
     *
     * @return the list of IDs
     */
    List<K> findIds();

    /**
     * Finds the entities Key<T> by the criteria {key:value}
     *
     * @param key   the key to query
     * @param value the value to search for
     * @return the list of IDs for documents matching the query
     */
    List<K> findIds(String key, Object value);

    /**
     * Finds the entities Ts by the criteria {key:value}
     *
     * @param query the query to use when finding the documents
     * @return the list of IDs for documents matching the query
     */
    List<K> findIds(Query<T> query);

    /**
     * Finds the first entity matching the query.
     *
     * @param key   the key to query
     * @param value the value to search for
     * @return the entity which match criteria {key:value}
     */
    T findOne(String key, Object value);

    /**
     * Finds the first entity matching the query.
     *
     * @param query the query to use when finding the documents
     * @return the entity which match the criteria
     */
    T findOne(Query<T> query);

    /**
     * Finds the first entity's ID
     *
     * @return the Key of the first entity
     */
    Key<T> findOneId();

    /**
     * Finds the first entity's ID matching a query
     *
     * @param key   the key to query
     * @param value the value to search for
     * @return the Key of the first entity
     */
    Key<T> findOneId(String key, Object value);

    /**
     * Finds the first entity's ID
     *
     * @param query the query to use when finding the documents
     * @return the Key of the first entity
     */
    Key<T> findOneId(Query<T> query);

    /**
     * Loads the entity by id value
     *
     * @param id the ID to search for
     * @return the entity with the given ID or null if no document in the database has the given ID
     */
    T get(K id);

    /**
     * @return the collection mapped by the entity class
     * @see #getEntityClass()
     */
    DBCollection getCollection();

    /**
     * @return the underlying datastore
     */
    Datastore getDatastore();

    /**
     * The type of entities for this DAO
     *
     * @return the entity class
     */
    Class<T> getEntityClass();

    /**
     * Saves the entity; either inserting or overriding the existing document
     *
     * @param entity the entity to save
     * @return the key of the entity
     */
    Key<T> save(T entity);

    /**
     * Saves the entity; either inserting or overriding the existing document
     *
     * @param entity the entity to save
     * @param wc     the WriteConcern to use when saving
     * @return the key of the entity
     * @see WriteConcern
     */
    Key<T> save(T entity, WriteConcern wc);

    /**
     * Updates all entities matched by the constraints with the modifiers supplied.
     *
     * @param query the query used to match the documents to update
     * @param ops   the update operations to perform
     * @return the results of the updates
     */
    UpdateResults update(Query<T> query, UpdateOperations<T> ops);

    /**
     * Updates the first entity matched by the constraints with the modifiers supplied.
     *
     * @param query the query used to match the document to update
     * @param ops   the update operations to perform
     * @return the results of the update
     */
    UpdateResults updateFirst(Query<T> query, UpdateOperations<T> ops);
}
