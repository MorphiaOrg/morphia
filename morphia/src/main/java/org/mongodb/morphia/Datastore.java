package org.mongodb.morphia;


import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.MapReduceCommand;
import com.mongodb.MongoClient;
import com.mongodb.WriteConcern;
import com.mongodb.WriteResult;
import org.mongodb.morphia.aggregation.AggregationPipeline;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.QueryFactory;
import org.mongodb.morphia.query.UpdateOperations;
import org.mongodb.morphia.query.UpdateResults;

import java.util.List;
import java.util.Map;


/**
 * Datastore interface to get/delete/save objects
 *
 * @author Scott Hernandez
 */
public interface Datastore {
    /**
     * Returns a new query bound to the kind (a specific {@link DBCollection})
     *
     * @param source The class to create aggregation against
     * @return the aggregation pipeline
     */
    AggregationPipeline createAggregation(Class source);

    /**
     * Returns a new query bound to the collection (a specific {@link DBCollection})
     *
     * @param collection The collection to query
     * @param <T>        the type of the query
     * @return the query
     */
    <T> Query<T> createQuery(Class<T> collection);

    /**
     * The builder for all update operations
     *
     * @param clazz the type to update
     * @param <T>   the type to update
     * @return the new UpdateOperations instance
     */
    <T> UpdateOperations<T> createUpdateOperations(Class<T> clazz);

    /**
     * Deletes the given entity (by id)
     *
     * @param clazz the type to delete
     * @param id    the ID of the entity to delete
     * @param <T>   the type to delete
     * @param <V>   the type of the id
     * @return results of the delete
     */
    <T, V> WriteResult delete(Class<T> clazz, V id);

    /**
     * Deletes the given entities (by id)
     *
     * @param clazz the type to delete
     * @param ids   the IDs of the entity to delete
     * @param <T>   the type to delete
     * @param <V>   the type of the id
     * @return results of the delete
     */
    <T, V> WriteResult delete(Class<T> clazz, Iterable<V> ids);

    /**
     * Deletes entities based on the query
     *
     * @param query the query to use when finding documents to delete
     * @param <T>   the type to delete
     * @return results of the delete
     */
    <T> WriteResult delete(Query<T> query);

    /**
     * Deletes entities based on the query, with the WriteConcern
     *
     * @param query the query to use when finding documents to delete
     * @param wc    the WriteConcern to use when deleting
     * @param <T>   the type to delete
     * @return results of the delete
     */
    <T> WriteResult delete(Query<T> query, WriteConcern wc);

    /**
     * Deletes the given entity (by @Id)
     *
     * @param entity the entity to delete
     * @param <T>    the type to delete
     * @return results of the delete
     */
    <T> WriteResult delete(T entity);

    /**
     * Deletes the given entity (by @Id), with the WriteConcern
     *
     * @param entity the entity to delete
     * @param wc     the WriteConcern to use when deleting
     * @param <T>    the type to delete
     * @return results of the delete
     */
    <T> WriteResult delete(T entity, WriteConcern wc);

    /**
     * ensure capped DBCollections for {@code Entity}(s)
     */
    void ensureCaps();

    /**
     * Ensures (creating if necessary) the index including the field(s) + directions on the given collection name; eg fields = "field1,
     * -field2" ({field1:1, field2:-1})
     *
     * @param clazz  the class from which to get the index definitions
     * @param fields the fields to index
     * @param <T>    the type to index
     */
    <T> void ensureIndex(Class<T> clazz, String fields);

    /**
     * Ensures (creating if necessary) the index including the field(s) + directions on the given collection name; eg fields = "field1,
     * -field2" ({field1:1, field2:-1})
     *
     * @param clazz            the class from which to get the index definitions
     * @param name             the name of the index to create
     * @param fields           the fields to index
     * @param unique           true if the index should enforce uniqueness on the fields indexed
     * @param dropDupsOnCreate if unique is true and this is true, any documents with duplicated fields being indexed will be dropped.  If
     *                         this is false, index creation will fail.
     * @param <T>              the type to index
     */
    <T> void ensureIndex(Class<T> clazz, String name, String fields, boolean unique, boolean dropDupsOnCreate);

    /**
     * Ensures (creating if necessary) the indexes found during class mapping (using {@code @Indexed, @Indexes)}
     */
    void ensureIndexes();

    /**
     * Ensures (creating if necessary) the indexes found during class mapping (using {@code @Indexed, @Indexes)} on the given collection
     * name, possibly in the background
     *
     * @param background if true, the index will be built in the background.  If false, the method will block until the index is created.
     */
    void ensureIndexes(boolean background);

    /**
     * Ensures (creating if necessary) the indexes found during class mapping (using {@code @Indexed, @Indexes)}
     *
     * @param clazz the class from which to get the index definitions
     * @param <T>   the type to index
     */
    <T> void ensureIndexes(Class<T> clazz);

    /**
     * Ensures (creating if necessary) the indexes found during class mapping (using {@code @Indexed, @Indexes)}, possibly in the
     * background
     *
     * @param clazz      the class from which to get the index definitions
     * @param background if true, the index will be built in the background.  If false, the method will block until the index is created.
     * @param <T>        the type to index
     */
    <T> void ensureIndexes(Class<T> clazz, boolean background);

    /**
     * Does a query to check if the keyOrEntity exists in mongodb
     *
     * @param keyOrEntity the value to check for
     * @return the key if the entity exists
     */
    Key<?> exists(Object keyOrEntity);

    /**
     * Find all instances by type
     *
     * @param clazz the class to use for mapping the results
     * @param <T>   the type to query
     * @return the query
     */
    <T> Query<T> find(Class<T> clazz);

    /**
     * <p> Find all instances by collectionName, and filter property. </p><p> This is the same as: {@code find(clazzOrEntity).filter
     * (property, value); } </p>
     *
     * @param clazz    the class to use for mapping the results
     * @param property the document property to query against
     * @param value    the value to check for
     * @param <T>      the type to query
     * @param <V>      the type to filter value
     * @return the query
     */
    <T, V> Query<T> find(Class<T> clazz, String property, V value);

    /**
     * Find all instances by type in a different collection than what is mapped on the class given skipping some documents and returning a
     * fixed number of the remaining.
     *
     * @param clazz    the class to use for mapping the results
     * @param property the document property to query against
     * @param value    the value to check for
     * @param offset   the number of results to skip
     * @param size     the maximum number of results to return
     * @param <T>      the type to query
     * @param <V>      the type to filter value
     * @return the query
     */
    <T, V> Query<T> find(Class<T> clazz, String property, V value, int offset, int size);

    /**
     * Deletes the given entities based on the query (first item only).
     *
     * @param query the query to use when finding entities to delete
     * @param <T>   the type to query
     * @return the deleted Entity
     */
    <T> T findAndDelete(Query<T> query);

    /**
     * Find the first Entity from the Query, and modify it.
     *
     * @param query      the query to use when finding entities to update
     * @param operations the updates to apply to the matched documents
     * @param <T>        the type to query
     * @return The modified Entity (the result of the update)
     */
    <T> T findAndModify(Query<T> query, UpdateOperations<T> operations);

    /**
     * Find the first Entity from the Query, and modify it.
     *
     * @param query      the query to find the Entity with; You are not allowed to offset/skip in the query.
     * @param operations the updates to apply to the matched documents
     * @param oldVersion indicated the old version of the Entity should be returned
     * @param <T>        the type to query
     * @return The Entity (the result of the update if oldVersion is false)
     */
    <T> T findAndModify(Query<T> query, UpdateOperations<T> operations, boolean oldVersion);

    /**
     * Find the first Entity from the Query, and modify it.
     *
     * @param query           the query to find the Entity with; You are not allowed to offset/skip in the query.
     * @param operations      the updates to apply to the matched documents
     * @param oldVersion      indicated the old version of the Entity should be returned
     * @param createIfMissing if the query returns no results, then a new object will be created (sets upsert=true)
     * @param <T>             the type of the entity
     * @return The Entity (the result of the update if oldVersion is false)
     */
    <T> T findAndModify(Query<T> query, UpdateOperations<T> operations, boolean oldVersion, boolean createIfMissing);

    /**
     * Find the given entities (by id); shorthand for {@code find("_id in", ids)}
     *
     * @param clazz the class to use for mapping
     * @param ids   the IDs to query
     * @param <T>   the type to fetch
     * @param <V>   the type of the ID
     * @return the query to find the entities
     */
    <T, V> Query<T> get(Class<T> clazz, Iterable<V> ids);

    /**
     * Find the given entity (by id); shorthand for {@code find("_id ", id)}
     *
     * @param clazz the class to use for mapping
     * @param id    the ID to query
     * @param <T>   the type to fetch
     * @param <V>   the type of the ID
     * @return the matched entity.  may be null.
     */
    <T, V> T get(Class<T> clazz, V id);

    /**
     * Find the given entity (by collectionName/id); think of this as refresh
     *
     * @param entity The entity to search for
     * @param <T>    the type to fetch
     * @return the matched entity.  may be null.
     */
    <T> T get(T entity);

    /**
     * Find the given entity (by collectionName/id);
     *
     * @param clazz the class to use for mapping
     * @param key   the key search with
     * @param <T>   the type to fetch
     * @return the matched entity.  may be null.
     */
    <T> T getByKey(Class<T> clazz, Key<T> key);

    /**
     * Find the given entities (by id), verifying they are of the correct type; shorthand for {@code find("_id in", ids)}
     *
     * @param clazz the class to use for mapping
     * @param keys  the keys to search with
     * @param <T>   the type to fetch
     * @return the matched entities.  may be null.
     */
    <T> List<T> getByKeys(Class<T> clazz, Iterable<Key<T>> keys);

    /**
     * Find the given entities (by id); shorthand for {@code find("_id in", ids)}
     *
     * @param keys the keys to search with
     * @param <T>  the type to fetch
     * @return the matched entities.  may be null.
     */
    <T> List<T> getByKeys(Iterable<Key<T>> keys);

    /**
     * @param clazz the class to use for mapping
     * @return the mapped collection for the collection
     */
    DBCollection getCollection(Class<?> clazz);

    /**
     * Gets the count this kind ({@link DBCollection})
     *
     * @param entity The entity whose type to count
     * @param <T>    the type to count
     * @return the count
     */
    <T> long getCount(T entity);

    /**
     * Gets the count this kind ({@link DBCollection})
     *
     * @param clazz The clazz type to count
     * @param <T>   the type to count
     * @return the count
     */
    <T> long getCount(Class<T> clazz);

    /**
     * Gets the count of items returned by this query; same as {@code query.countAll()}
     *
     * @param query the query to filter the documents to count
     * @param <T>   the type to count
     * @return the count
     */
    <T> long getCount(Query<T> query);

    /**
     * @return the DB this Datastore uses
     */
    DB getDB();

    /**
     * @return the default WriteConcern used by this Datastore
     */
    WriteConcern getDefaultWriteConcern();

    /**
     * Sets the default WriteConcern for this Datastore
     *
     * @param wc the default WriteConcern to be used by this Datastore
     */
    void setDefaultWriteConcern(WriteConcern wc);

    /**
     * Creates a (type-safe) reference to the entity; if stored this will become a {@link com.mongodb.DBRef}
     *
     * @param entity the entity whose key is to be returned
     * @param <T>    the type of the entity
     * @return the Key
     */
    <T> Key<T> getKey(T entity);

    /**
     * Get the underlying MongoClient that allows connection to the MongoDB instance being used.
     *
     * @return the MongoClient being used by this datastore.
     */
    MongoClient getMongo();

    /**
     * @return the current {@link QueryFactory}.
     * @see QueryFactory
     */
    QueryFactory getQueryFactory();

    /**
     * Replaces the current {@link QueryFactory} with the given value.
     *
     * @param queryFactory the QueryFactory to use
     * @see QueryFactory
     */
    void setQueryFactory(QueryFactory queryFactory);

    /**
     * Runs a map/reduce job at the server; this should be used with a server version 1.7.4 or higher
     *
     * @param <T>         The type of resulting data
     * @param outputType  The type of resulting data; inline is not working yet
     * @param type        MapreduceType
     * @param q           The query (only the criteria, limit and sort will be used)
     * @param map         The map function, in javascript, as a string
     * @param reduce      The reduce function, in javascript, as a string
     * @param finalize    The finalize function, in javascript, as a string; can be null
     * @param scopeFields Each map entry will be a global variable in all the functions; can be null
     * @return counts and stuff
     */
    <T> MapreduceResults<T> mapReduce(MapreduceType type, Query q, String map, String reduce, String finalize,
                                      Map<String, Object> scopeFields, Class<T> outputType);

    /**
     * Runs a map/reduce job at the server; this should be used with a server version 1.7.4 or higher
     *
     * @param <T>         The type of resulting data
     * @param type        MapreduceType
     * @param q           The query (only the criteria, limit and sort will be used)
     * @param outputType  The type of resulting data; inline is not working yet
     * @param baseCommand The base command to fill in and send to the server
     * @return counts and stuff
     */
    <T> MapreduceResults<T> mapReduce(MapreduceType type, Query q, Class<T> outputType, MapReduceCommand baseCommand);

    /**
     * Work as if you did an update with each field in the entity doing a $set; Only at the top level of the entity.
     *
     * @param entity the entity to merge back in to the database
     * @param <T>    the type of the entity
     * @return the key of the entity
     */
    <T> Key<T> merge(T entity);

    /**
     * Work as if you did an update with each field in the entity doing a $set; Only at the top level of the entity.
     *
     * @param entity the entity to merge back in to the database
     * @param <T>    the type of the entity
     * @param wc     the WriteConcern to use
     * @return the key of the entity
     */
    <T> Key<T> merge(T entity, WriteConcern wc);

    /**
     * Returns a new query based on the example object
     *
     * @param example the example entity to use when creating the query
     * @param <T>     the type of the entity
     * @return the query
     */
    <T> Query<T> queryByExample(T example);

    /**
     * Saves the entities (Objects) and updates the @Id field
     *
     * @param entities the entities to save
     * @param <T>      the type of the entity
     * @return the keys of the entities
     */
    <T> Iterable<Key<T>> save(Iterable<T> entities);

    /**
     * Saves the entities (Objects) and updates the @Id field, with the WriteConcern
     *
     * @param entities the entities to save
     * @param <T>      the type of the entity
     * @param wc       the WriteConcern to use
     * @return the keys of the entities
     */
    <T> Iterable<Key<T>> save(Iterable<T> entities, WriteConcern wc);

    /**
     * Saves the entities (Objects) and updates the @Id field
     *
     * @param entities the entities to save
     * @param <T>      the type of the entity
     * @return the keys of the entities
     */
    <T> Iterable<Key<T>> save(T... entities);

    /**
     * Saves an entity (Object) and updates the @Id field
     *
     * @param entity the entity to save
     * @param <T>    the type of the entity
     * @return the keys of the entity
     */
    <T> Key<T> save(T entity);

    /**
     * Saves an entity (Object) and updates the @Id field, with the WriteConcern
     *
     * @param entity the entity to save
     * @param wc     the WriteConcern to use
     * @param <T>    the type of the entity
     * @return the keys of the entity
     */
    <T> Key<T> save(T entity, WriteConcern wc);

    /**
     * Updates an entity with the operations; this is an atomic operation
     *
     * @param entity     the entity to update
     * @param operations the update operations to perform
     * @param <T>        the type of the entity
     * @return the update results
     * @see UpdateResults
     */
    <T> UpdateResults update(T entity, UpdateOperations<T> operations);

    /**
     * Updates an entity with the operations; this is an atomic operation
     *
     * @param key        the key of entity to update
     * @param operations the update operations to perform
     * @param <T>        the type of the entity
     * @return the update results
     * @see UpdateResults
     */
    <T> UpdateResults update(Key<T> key, UpdateOperations<T> operations);

    /**
     * Updates all entities found with the operations; this is an atomic operation per entity
     *
     * @param query      the query used to match the documents to update
     * @param operations the update operations to perform
     * @param <T>        the type of the entity
     * @return the results of the updates
     */
    <T> UpdateResults update(Query<T> query, UpdateOperations<T> operations);

    /**
     * Updates all entities found with the operations, if nothing is found insert the update as an entity if "createIfMissing" is true;
     * this
     * is an atomic operation per entity
     *
     * @param query           the query used to match the documents to update
     * @param operations      the update operations to perform
     * @param createIfMissing if true, a document will be created if none can be found that match the query
     * @param <T>             the type of the entity
     * @return the results of the updates
     */
    <T> UpdateResults update(Query<T> query, UpdateOperations<T> operations, boolean createIfMissing);

    /**
     * Updates all entities found with the operations, if nothing is found insert the update as an entity if "createIfMissing" is true;
     * this
     * is an atomic operation per entity
     *
     * @param query           the query used to match the documents to update
     * @param operations      the update operations to perform
     * @param createIfMissing if true, a document will be created if none can be found that match the query
     * @param wc              the WriteConcern to use
     * @param <T>             the type of the entity
     * @return the results of the updates
     */
    <T> UpdateResults update(Query<T> query, UpdateOperations<T> operations, boolean createIfMissing, WriteConcern wc);

    /**
     * Updates the first entity found with the operations; this is an atomic operation
     *
     * @param query      the query used to match the document to update
     * @param operations the update operations to perform
     * @param <T>        the type of the entity
     * @return the results of the update
     */
    <T> UpdateResults updateFirst(Query<T> query, UpdateOperations<T> operations);

    /**
     * Updates the first entity found with the operations, if nothing is found insert the update as an entity if "createIfMissing" is true.
     *
     * @param query           the query used to match the documents to update
     * @param operations      the update operations to perform
     * @param createIfMissing if true, a document will be created if none can be found that match the query
     * @param <T>             the type of the entity
     * @return the results of the updates
     */
    <T> UpdateResults updateFirst(Query<T> query, UpdateOperations<T> operations, boolean createIfMissing);

    /**
     * Updates the first entity found with the operations, if nothing is found insert the update as an entity if "createIfMissing" is true.
     *
     * @param query           the query used to match the documents to update
     * @param operations      the update operations to perform
     * @param createIfMissing if true, a document will be created if none can be found that match the query
     * @param wc              the WriteConcern to use
     * @param <T>             the type of the entity
     * @return the results of the updates
     */
    <T> UpdateResults updateFirst(Query<T> query, UpdateOperations<T> operations, boolean createIfMissing, WriteConcern wc);

    /**
     * updates the first entity found using the entity as a template, if nothing is found insert the update as an entity if
     * "createIfMissing" is true.
     *
     * @param query           the query used to match the documents to update
     * @param entity          the entity whose state will be used as an update template for any matching documents
     * @param createIfMissing if true, a document will be created if none can be found that match the query
     * @param <T>             the type of the entity
     * @return the results of the updates
     */
    <T> UpdateResults updateFirst(Query<T> query, T entity, boolean createIfMissing);
}
