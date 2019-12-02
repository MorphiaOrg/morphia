package dev.morphia;


import com.mongodb.ClientSessionOptions;
import com.mongodb.DBCollection;
import com.mongodb.WriteConcern;
import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.ReturnDocument;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import dev.morphia.aggregation.AggregationPipeline;
import dev.morphia.annotations.Indexed;
import dev.morphia.annotations.Indexes;
import dev.morphia.annotations.Text;
import dev.morphia.annotations.Validation;
import dev.morphia.mapping.Mapper;
import dev.morphia.query.FindAndDeleteOptions;
import dev.morphia.query.Query;
import dev.morphia.query.QueryFactory;
import dev.morphia.query.QueryImpl;
import dev.morphia.query.UpdateOperations;
import dev.morphia.query.UpdateOpsImpl;
import dev.morphia.transactions.experimental.MorphiaTransaction;

import java.util.ArrayList;
import java.util.List;


/**
 * Datastore interface to get/delete/save objects
 */
public interface Datastore {
    /**
     * Returns the session this datastore is attached to or null if none is attached.
     *
     * @return the session
     * @since 2.0
     */
    default ClientSession getSession() {
        return null;
    }

    /**
     * @param transaction the transaction wrapper
     * @param <T>         the return type
     * @return the return value
     * @since 2.0
     * @morphia.experimental
     */
    <T> T withTransaction(MorphiaTransaction<T> transaction);

    /**
     * @param transaction the transaction wrapper
     * @param options     the session options to apply
     * @param <T>         the return type
     * @return the return value
     * @since 2.0
     * @morphia.experimental
     */
    <T> T withTransaction(MorphiaTransaction<T> transaction, final ClientSessionOptions options);

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
     * @param type The collection to query
     * @param <T>  the type of the query
     * @return the query
     * @deprecated use {@link #find(Class)}
     */
    @Deprecated(since = "2.0", forRemoval = true)
    default <T> Query<T> createQuery(Class<T> type) {
        return find(type);
    }

    /**
     * Find all instances by type
     *
     * @param clazz the class to use for mapping the results
     * @param <T>   the type to query
     * @return the query
     */
    <T> Query<T> find(Class<T> clazz);

    /**
     * The builder for all update operations
     *
     * @param clazz the type to update
     * @param <T>   the type to update
     * @return the new UpdateOperations instance
     * @deprecated use {@link Query#update()} instead
     */
    @Deprecated(since = "2.0", forRemoval = true)
    default <T> UpdateOperations<T> createUpdateOperations(Class<T> clazz) {
        return new UpdateOpsImpl<>(clazz, getMapper());
    }

    /**
     * @return the Mapper used by this Datastore
     * @morphia.internal
     * @since 1.5
     */
    Mapper getMapper();

    /**
     * Deletes entities based on the query
     *
     * @param <T>   the type to delete
     * @param query the query to use when finding documents to delete
     * @return results of the delete
     * @deprecated use {@link Query#remove()} instead
     */
    @Deprecated(since = "2.0", forRemoval = true)
    default <T> DeleteResult delete(Query<T> query) {
        return query.remove(new DeleteOptions());
    }

    /**
     * Deletes entities based on the query
     *
     * @param <T>     the type to delete
     * @param query   the query to use when finding documents to delete
     * @param options the options to apply to the delete
     * @return results of the delete
     * @since 1.3
     * @deprecated use {@link Query#remove(DeleteOptions)} instead
     */
    @Deprecated(since = "2.0", forRemoval = true)
    default <T> DeleteResult delete(Query<T> query, DeleteOptions options) {
        return query.remove(options);
    }

    /**
     * Deletes the given entity (by @Id)
     *
     * @param <T>    the type to delete
     * @param entity the entity to delete
     * @return results of the delete
     */
    <T> DeleteResult delete(T entity);

    /**
     * Deletes the given entity (by @Id), with the WriteConcern
     *
     * @param <T>     the type to delete
     * @param entity  the entity to delete
     * @param options the options to use when deleting
     * @return results of the delete
     * @since 1.3
     */
    <T> DeleteResult delete(T entity, DeleteOptions options);

    /**
     * ensure capped collections for {@code Entity}(s)
     */
    void ensureCaps();

    /**
     * Process any {@link Validation} annotations for document validation.
     *
     * @mongodb.driver.manual core/document-validation/
     * @since 1.3
     */
    void enableDocumentValidation();

    /**
     * Ensures (creating if necessary) the indexes found during class mapping
     *
     * @see Indexes
     * @see Indexed
     * @see Text
     */
    void ensureIndexes();

    /**
     * Ensures (creating if necessary) the indexes found during class mapping
     *
     * @param clazz the class from which to get the index definitions
     * @param <T>   the type to index
     * @see Indexes
     * @see Indexed
     * @see Text
     */
    <T> void ensureIndexes(Class<T> clazz);

    /**
     * Deletes the given entities based on the query (first item only).
     *
     * @param query the query to use when finding entities to delete
     * @param <T>   the type to query
     * @return the deleted Entity
     * @deprecated use {@link Query#delete()} instead
     */
    @Deprecated(since = "2.0", forRemoval = true)
    default <T> T findAndDelete(Query<T> query) {
        return query.delete();
    }

    /**
     * Deletes the given entities based on the query (first item only).
     *
     * @param query   the query to use when finding entities to delete
     * @param options the options to apply to the delete
     * @param <T>     the type to query
     * @return the deleted Entity
     * @since 1.3
     * @deprecated use {@link Query#delete(FindAndDeleteOptions)} instead
     */
    @Deprecated(since = "2.0", forRemoval = true)
    default <T> T findAndDelete(Query<T> query, FindAndModifyOptions options) {
        return query.delete(options);
    }

    /**
     * Find the first Entity from the Query, and modify it.
     *
     * @param query      the query to use when finding entities to update
     * @param operations the updates to apply to the matched documents
     * @param <T>        the type to query
     * @return The modified Entity (the result of the update)
     * @deprecated use {@link Query#modify()} instead
     */
    @Deprecated(since = "2.0", forRemoval = true)
    default <T> T findAndModify(Query<T> query, UpdateOperations<T> operations) {
        return query.modify(operations).execute(new FindAndModifyOptions()
                                                    .returnDocument(ReturnDocument.AFTER));
    }

    /**
     * Find the given entities (by id); shorthand for {@code find("_id in", ids)}
     *
     * @param clazz the class to use for mapping
     * @param ids   the IDs to query
     * @param <T>   the type to fetch
     * @param <V>   the type of the ID
     * @return the query to find the entities
     * @morphia.inline
     * @deprecated use {@link Query} instead.
     */
    @Deprecated
    default <T, V> Query<T> get(Class<T> clazz, Iterable<V> ids) {
        return find(clazz).filter("_id in", ids);
    }

    /**
     * Find the given entity (by collectionName/id); think of this as refresh
     *
     * @param entity The entity to search for
     * @param <T>    the type to fetch
     * @return the matched entity.  may be null.
     * @morphia.inline
     * @deprecated use {@link Query} instead
     */
    @Deprecated
    default <T> T get(T entity) {
        return (T) find(entity.getClass()).filter("_id", getMapper().getId(entity)).first();
    }

    /**
     * Find the given entity (by collectionName/id);
     *
     * @param clazz the class to use for mapping
     * @param key   the key search with
     * @param <T>   the type to fetch
     * @return the matched entity.  may be null.
     * @deprecated use a {@link Query} instead
     */
    @Deprecated
    <T> T getByKey(Class<T> clazz, Key<T> key);

    /**
     * Find the given entities (by id), verifying they are of the correct type; shorthand for {@code find("_id in", ids)}
     *
     * @param clazz the class to use for mapping
     * @param keys  the keys to search with
     * @param <T>   the type to fetch
     * @return the matched entities.  may be null.
     * @deprecated use a {@link Query} instead
     */
    @Deprecated
    <T> List<T> getByKeys(Class<T> clazz, Iterable<Key<T>> keys);

    /**
     * Find the given entities (by id); shorthand for {@code find("_id in", ids)}
     *
     * @param keys the keys to search with
     * @param <T>  the type to fetch
     * @return the matched entities.  may be null.
     * @deprecated use a {@link Query} instead
     */
    @Deprecated
    <T> List<T> getByKeys(Iterable<Key<T>> keys);

    /**
     * @param clazz the class to use for mapping
     * @param <T>   the type of the collection
     * @return the mapped collection for the collection
     * @morphia.internal
     */
    <T> MongoCollection<T> getCollection(Class<T> clazz);

    /**
     * @return the MongoDatabase used by this DataStore
     * @morphia.internal
     * @since 1.5
     */
    MongoDatabase getDatabase();

    /**
     * Creates a (type-safe) reference to the entity; if stored this will become a {@link com.mongodb.DBRef}
     *
     * @param entity the entity whose key is to be returned
     * @param <T>    the type of the entity
     * @return the Key
     */
    <T> Key<T> getKey(T entity);

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
     * Work as if you did an update with each field in the entity doing a $set; Only at the top level of the entity.
     *
     * @param entity the entity to merge back in to the database
     * @param <T>    the type of the entity
     * @return the new merged entity.  NOTE:  this is a database fetch.
     */
    <T> T merge(T entity);

    /**
     * Work as if you did an update with each field in the entity doing a $set; Only at the top level of the entity.
     *
     * @param entity  the entity to merge back in to the database
     * @param options the options to apply
     * @param <T>     the type of the entity
     * @return the new merged entity.  NOTE:  this is a database fetch.
     * @since 2.0
     */
    <T> T merge(T entity, InsertOneOptions options);

    /**
     * Work as if you did an update with each field in the entity doing a $set; Only at the top level of the entity.
     *
     * @param entity the entity to merge back in to the database
     * @param <T>    the type of the entity
     * @param wc     the WriteConcern to use
     */
    @Deprecated(since = "2.0", forRemoval = true)
    <T> void merge(T entity, WriteConcern wc);

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
     * @deprecated
     */
    @Deprecated(since = "2.0", forRemoval = true)
    default <T> void save(Iterable<T> entities) {
        List<T> list = new ArrayList<>();
        entities.forEach(e -> list.add(e));
        save(list);
    }

    /**
     * Saves the entities (Objects) and updates the @Id field
     *
     * @param entities the entities to save
     * @param <T>      the type of the entity
     * @return the saved entities
     */
    default <T> List<T> save(List<T> entities) {
        return save(entities, new InsertManyOptions());
    }

    /**
     * Saves the entities (Objects) and updates the @Id field
     *
     * @param entities the entities to save
     * @param <T>      the type of the entity
     * @param options  the options to apply to the save operation
     * @return the saved entities
     * @deprecated use {@link #save(List, InsertManyOptions)} instead
     */
    @Deprecated(since = "2.0", forRemoval = true)
    default <T> List<T> save(Iterable<T> entities, InsertOptions options) {
        List<T> list = new ArrayList<>();
        entities.forEach(e -> list.add(e));
        return save(list, options.toInsertManyOptions());
    }

    /**
     * Saves the entities (Objects) and updates the @Id field
     *
     * @param entities the entities to save
     * @param <T>      the type of the entity
     * @param options  the options to apply to the save operation
     * @return the saved entities
     * @since 2.0
     */
    <T> List<T> save(List<T> entities, InsertManyOptions options);

    /**
     * Saves an entity (Object) and updates the @Id field
     *
     * @param entity the entity to save
     * @param <T>    the type of the entity
     * @return the saved entity
     */
    <T> T save(T entity);

    /**
     * Saves an entity (Object) and updates the @Id field
     *
     * @param entity  the entity to save
     * @param options the options to apply to the save operation
     * @param <T>     the type of the entity
     * @return the saved entity
     * @deprecated use {@link #save(T, InsertOneOptions)} instead
     */
    @Deprecated(since = "2.0", forRemoval = true)
    default <T> T save(T entity, InsertOptions options) {
        return save(entity, options.toInsertOneOptions());
    }

    /**
     * Saves an entity (Object) and updates the @Id field
     *
     * @param entity  the entity to save
     * @param options the options to apply to the save operation
     * @param <T>     the type of the entity
     * @return the saved entity
     */
    <T> T save(T entity, InsertOneOptions options);

    /**
     * Updates all entities found with the operations; this is an atomic operation per entity
     *
     * @param query      the query used to match the documents to update
     * @param operations the update operations to perform
     * @param <T>        the type of the entity
     * @return the results of the updates
     * @deprecated use {@link Query#update()} instead.  Please note the default has changed from multi- to single- document updates.
     */
    @Deprecated(since = "2.0", forRemoval = true)
    default <T> UpdateResult update(Query<T> query, UpdateOperations<T> operations) {
        return query.update(operations).execute(new UpdateOptions()
                                                    .upsert(false)
                                                    .multi(true)
                                                    .writeConcern(getMapper().getWriteConcern(((QueryImpl) query).getEntityClass())));
    }

    /**
     * Updates all entities found with the operations; this is an atomic operation per entity
     *
     * @param <T>        the type of the entity
     * @param query      the query used to match the documents to update
     * @param operations the update operations to perform
     * @param options    the options to apply to the update
     * @return the results of the updates
     * @since 1.3
     * @deprecated use {@link Query#update()} instead
     */
    @Deprecated(since = "2.0", forRemoval = true)
    default <T> UpdateResult update(Query<T> query, UpdateOperations<T> operations, UpdateOptions options) {
        return query.update(operations).execute(options);
    }
}
