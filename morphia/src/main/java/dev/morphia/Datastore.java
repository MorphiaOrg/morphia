package dev.morphia;

import com.mongodb.ClientSessionOptions;
import com.mongodb.DBCollection;
import com.mongodb.WriteConcern;
import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.ReturnDocument;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import dev.morphia.aggregation.experimental.Aggregation;
import dev.morphia.annotations.Indexed;
import dev.morphia.annotations.Indexes;
import dev.morphia.annotations.Text;
import dev.morphia.annotations.Validation;
import dev.morphia.experimental.MorphiaSession;
import dev.morphia.internal.SessionConfigurable;
import dev.morphia.mapping.Mapper;
import dev.morphia.query.FindAndDeleteOptions;
import dev.morphia.query.FindOptions;
import dev.morphia.query.Query;
import dev.morphia.query.QueryFactory;
import dev.morphia.transactions.experimental.MorphiaTransaction;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static dev.morphia.query.experimental.filters.Filters.eq;
import static dev.morphia.query.experimental.filters.Filters.in;

/**
 * Datastore interface to get/delete/save objects
 */
@SuppressWarnings({"unchecked", "UnusedReturnValue", "unused"})
public interface Datastore {
    /**
     * Returns a new query bound to the kind (a specific {@link DBCollection})
     *
     * @param source The collection aggregation against
     * @return the aggregation pipeline
     * @morphia.experimental
     * @since 2.0
     */
    Aggregation<Document> aggregate(String source);

    /**
     * Returns a new query bound to the kind (a specific {@link DBCollection})
     *
     * @param source The class to create aggregation against
     * @param <T>    the source type
     * @return the aggregation pipeline
     * @morphia.experimental
     * @since 2.0
     */
    <T> Aggregation<T> aggregate(Class<T> source);

    /**
     * Returns a new query bound to the kind (a specific {@link DBCollection})
     *
     * @param source The class to create aggregation against
     * @return the aggregation pipeline
     * @deprecated use {@link #aggregate(Class)} instead
     */
    @SuppressWarnings("removal")
    @Deprecated(since = "2.0", forRemoval = true)
    dev.morphia.aggregation.AggregationPipeline createAggregation(Class<?> source);

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
     * The builder for all update operations
     *
     * @param clazz the type to update
     * @param <T>   the type to update
     * @return the new UpdateOperations instance
     * @deprecated use {@link Query#update()} instead
     */
    @SuppressWarnings("removal")
    @Deprecated(since = "2.0", forRemoval = true)
    default <T> dev.morphia.query.UpdateOperations<T> createUpdateOperations(Class<T> clazz) {
        return new dev.morphia.query.UpdateOpsImpl<>(clazz, getMapper());
    }

    /**
     * Deletes entities based on the query
     *
     * @param <T>   the type to delete
     * @param query the query to use when finding documents to delete
     * @return results of the delete
     * @deprecated use {@link Query#delete()} instead
     */
    @Deprecated(since = "2.0", forRemoval = true)
    default <T> DeleteResult delete(Query<T> query) {
        return query.delete(new DeleteOptions());
    }

    /**
     * Deletes entities based on the query
     *
     * @param <T>     the type to delete
     * @param query   the query to use when finding documents to delete
     * @param options the options to apply to the delete
     * @return results of the delete
     * @since 1.3
     * @deprecated use {@link Query#delete(DeleteOptions)} instead
     */
    @Deprecated(since = "2.0", forRemoval = true)
    default <T> DeleteResult delete(Query<T> query, DeleteOptions options) {
        return query.delete(options);
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
     * Process any {@link Validation} annotations for document validation.
     *
     * @mongodb.driver.manual core/document-validation/
     * @since 1.3
     */
    void enableDocumentValidation();

    /**
     * ensure capped collections for {@code Entity}(s)
     */
    void ensureCaps();

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
     * Find all instances by type
     *
     * @param type the class to use for mapping the results
     * @param <T>  the type to query
     * @return the query
     */
    <T> Query<T> find(Class<T> type);

    /**
     * Find all instances by type from an alternate collection
     *
     * @param collection the actual collection to query.  This overrides any mapped on collection on type.
     * @param type       the class to use for mapping the results
     * @param <T>        the type to query
     * @return the query
     */
    <T> Query<T> find(String collection, Class<T> type);

    /**
     * Find all instances by type in a different collection than what is mapped on the class given.
     *
     * @param collection the collection to query against
     * @param <T>        the type to query
     * @return the query
     * @morphia.internal
     */
    <T> Query<T> find(String collection);

    /**
     * Deletes the given entities based on the query (first item only).
     *
     * @param query the query to use when finding entities to delete
     * @param <T>   the type to query
     * @return the deleted Entity
     * @deprecated use {@link Query#findAndDelete()} instead
     */
    @Deprecated(since = "2.0", forRemoval = true)
    default <T> T findAndDelete(Query<T> query) {
        return query.findAndDelete();
    }

    /**
     * Deletes the given entities based on the query (first item only).
     *
     * @param query   the query to use when finding entities to delete
     * @param options the options to apply to the delete
     * @param <T>     the type to query
     * @return the deleted Entity
     * @since 1.3
     * @deprecated use {@link Query#findAndDelete(FindAndDeleteOptions)} instead
     */
    @SuppressWarnings("removal")
    @Deprecated(since = "2.0", forRemoval = true)
    default <T> T findAndDelete(Query<T> query, FindAndModifyOptions options) {
        return query.findAndDelete(new FindAndDeleteOptions()
                                       .writeConcern(options.getWriteConcern())
                                       .collation(options.getCollation())
                                       .maxTime(options.getMaxTime(TimeUnit.MILLISECONDS), TimeUnit.MILLISECONDS)
                                       .sort(options.getSort())
                                       .projection(options.getProjection()));
    }

    /**
     * Find the first Entity from the Query, and modify it.
     *
     * @param query      the query to use when finding entities to update
     * @param operations the updates to apply to the matched documents
     * @param options    the options to apply
     * @param <T>        the type to query
     * @return The modified Entity (the result of the update)
     * @deprecated use {@link Query#modify()} instead
     */
    @SuppressWarnings("removal")
    @Deprecated(since = "2.0", forRemoval = true)
    default <T> T findAndModify(Query<T> query, dev.morphia.query.UpdateOperations<T> operations, FindAndModifyOptions options) {
        return query.modify(operations).execute(options);
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
    @SuppressWarnings("removal")
    @Deprecated(since = "2.0", forRemoval = true)
    default <T> T findAndModify(Query<T> query, dev.morphia.query.UpdateOperations<T> operations) {
        return query.modify(operations).execute(new ModifyOptions()
                                                    .returnDocument(ReturnDocument.AFTER));
    }

    /**
     * @param configurable the configurable
     * @return any session found first on the configurable then on this
     * @morphia.internal
     * @since 2.0
     */
    ClientSession findSession(SessionConfigurable<?> configurable);

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
    @Deprecated(since = "2.0", forRemoval = true)
    default <T, V> Query<T> get(Class<T> clazz, Iterable<V> ids) {
        return find(clazz).filter(in("_id", ids));
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
    @Deprecated(since = "2.0", forRemoval = true)
    default <T> T get(T entity) {
        return (T) find(entity.getClass()).filter(eq("_id", getMapper().getId(entity))).first();
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
    @Deprecated(since = "2.0", forRemoval = true)
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
    @Deprecated(since = "2.0", forRemoval = true)
    <T> List<T> getByKeys(Class<T> clazz, Iterable<Key<T>> keys);

    /**
     * Find the given entities (by id); shorthand for {@code find("_id in", ids)}
     *
     * @param keys the keys to search with
     * @param <T>  the type to fetch
     * @return the matched entities.  may be null.
     * @deprecated use a {@link Query} instead
     */
    @Deprecated(since = "2.0", forRemoval = true)
    <T> List<T> getByKeys(Iterable<Key<T>> keys);

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
    @Deprecated(since = "2.0", forRemoval = true)
    <T> Key<T> getKey(T entity);

    /**
     * @param options the options used when requesting logging
     * @return the logged query
     * @morphia.internal
     * @since 2.0
     */
    String getLoggedQuery(FindOptions options);

    /**
     * @return the Mapper used by this Datastore
     * @morphia.internal
     * @since 1.5
     */
    Mapper getMapper();

    /**
     * @return the current {@link QueryFactory}.
     * @morphia.internal
     * @see QueryFactory
     */
    QueryFactory getQueryFactory();

    /**
     * Replaces the current {@link QueryFactory} with the given value.
     *
     * @param queryFactory the QueryFactory to use
     * @morphia.internal
     * @see QueryFactory
     */
    void setQueryFactory(QueryFactory queryFactory);

    /**
     * Returns the session this datastore is attached to or null if none is attached.
     *
     * @return the session
     * @morphia.experimental
     * @since 2.0
     */
    default ClientSession getSession() {
        return null;
    }

    /**
     * Inserts an entity in to the mapped collection.
     *
     * @param entity the entity to insert
     * @param <T>    the type of the entity
     */
    <T> void insert(T entity);

    /**
     * Inserts an entity in to the mapped collection.
     *
     * @param entity  the entity to insert
     * @param options the options to apply to the insert operation
     * @param <T>     the type of the entity
     * @since 2.0
     */
    <T> void insert(T entity, InsertOneOptions options);

    /**
     * Inserts a List of entities in to the mapped collection.
     *
     * @param entities the entities to insert
     * @param <T>      the type of the entity
     * @since 2.0
     */
    default <T> void insert(List<T> entities) {
        insert(entities, new InsertManyOptions());
    }

    /**
     * Inserts entities in to the mapped collection.
     *
     * @param entities the entities to insert
     * @param options  the options to apply to the insert operation
     * @param <T>      the type of the entity
     * @since 2.0
     */
    <T> void insert(List<T> entities, InsertManyOptions options);

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
     * @deprecated use {@link #merge(Object, InsertOneOptions)} instead
     */
    @Deprecated(since = "2.0", forRemoval = true)
    default <T> void merge(T entity, WriteConcern wc) {
        merge(entity, new InsertOneOptions().writeConcern(wc));
    }

    /**
     * Returns a new query based on the example object
     *
     * @param example the example entity to use when creating the query
     * @param <T>     the type of the entity
     * @return the query
     */
    <T> Query<T> queryByExample(T example);

    /**
     * Refreshes an existing entity to its current state in the database.  Essentially, any existing mapped state is replaced by the
     * latest persisted state while preserving the entity's reference and object identity.
     *
     * @param entity the entity to refresh
     * @param <T>    the entity type
     * @morphia.experimental
     * @since 2.0
     */
    <T> void refresh(T entity);

    /**
     * Saves the entities (Objects) and updates the @Id field
     *
     * @param <T>      the type of the entity
     * @param entities the entities to save
     * @return the list of updated entities
     * @deprecated use {@link #save(List)} instead
     */
    @Deprecated(since = "2.0", forRemoval = true)
    default <T> List<T> save(Iterable<T> entities) {
        List<T> list = new ArrayList<>();
        entities.forEach(list::add);
        return save(list);
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
     * @since 2.0
     */
    <T> List<T> save(List<T> entities, InsertManyOptions options);

    /**
     * Saves the entities (Objects) and updates the @Id field
     *
     * @param entities the entities to save
     * @param <T>      the type of the entity
     * @param options  the options to apply to the save operation
     * @return the saved entities
     * @deprecated use {@link #save(List, InsertManyOptions)} instead
     */
    @SuppressWarnings("removal")
    @Deprecated(since = "2.0", forRemoval = true)
    default <T> List<T> save(Iterable<T> entities, InsertOptions options) {
        List<T> list = new ArrayList<>();
        entities.forEach(list::add);
        return save(list, options.toInsertManyOptions());
    }

    /**
     * Saves an entity (Object) and updates the @Id field
     *
     * @param entity the entity to save
     * @param <T>    the type of the entity
     * @return the saved entity
     */
    default <T> T save(T entity) {
        return save(entity, new InsertOneOptions());
    }

    /**
     * Saves an entity (Object) and updates the @Id field
     *
     * @param entity  the entity to save
     * @param options the options to apply to the save operation
     * @param <T>     the type of the entity
     * @return the saved entity
     * @deprecated use {@link #save(Object, InsertOneOptions)} instead
     */
    @SuppressWarnings("removal")
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
     * Starts a new session on the server.
     *
     * @return the new session reference
     * @morphia.experimental
     * @since 2.0
     */
    MorphiaSession startSession();

    /**
     * Starts a new session on the server.
     *
     * @param options the options to apply
     * @return the new session reference
     * @morphia.experimental
     * @since 2.0
     */
    MorphiaSession startSession(ClientSessionOptions options);

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
    @SuppressWarnings("removal")
    @Deprecated(since = "2.0", forRemoval = true)
    default <T> UpdateResult update(Query<T> query, dev.morphia.query.UpdateOperations<T> operations, UpdateOptions options) {
        return query.update(operations).execute(options);
    }

    /**
     * Updates all entities found with the operations; this is an atomic operation per entity
     *
     * @param query      the query used to match the documents to update
     * @param operations the update operations to perform
     * @param <T>        the type of the entity
     * @return the results of the updates
     * @deprecated use {@link Query#update()} instead.  Please note the default has changed from multi- to single- document updates.
     */
    @SuppressWarnings("removal")
    @Deprecated(since = "2.0", forRemoval = true)
    default <T> UpdateResult update(Query<T> query, dev.morphia.query.UpdateOperations<T> operations) {
        return query.update(operations).execute(new UpdateOptions()
                                                    .upsert(false)
                                                    .multi(true)
                                                    .writeConcern(getMapper().getWriteConcern(query.getEntityClass())));
    }

    /**
     * @param transaction the transaction wrapper
     * @param <T>         the return type
     * @return the return value
     * @morphia.experimental
     * @since 2.0
     */
    <T> T withTransaction(MorphiaTransaction<T> transaction);

    /**
     * @param <T>         the return type
     * @param options     the session options to apply
     * @param transaction the transaction wrapper
     * @return the return value
     * @morphia.experimental
     * @since 2.0
     */
    <T> T withTransaction(ClientSessionOptions options, MorphiaTransaction<T> transaction);
}
