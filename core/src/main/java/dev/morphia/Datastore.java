package dev.morphia;

import java.util.List;

import com.mongodb.ClientSessionOptions;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.lang.Nullable;

import dev.morphia.aggregation.Aggregation;
import dev.morphia.annotations.internal.MorphiaExperimental;
import dev.morphia.annotations.internal.MorphiaInternal;
import dev.morphia.query.Query;
import dev.morphia.transactions.MorphiaSession;
import dev.morphia.transactions.MorphiaTransaction;

import org.bson.Document;

/**
 * Datastore interface to get/delete/save objects
 */
@SuppressWarnings({ "UnusedReturnValue", "unused", "removal" })
public interface Datastore {
    /**
     * Returns a new query bound to the kind (a specific {@link MongoCollection})
     *
     * @param source The collection aggregation against
     * @return the aggregation pipeline
     * @since 2.0
     */
    Aggregation<Document> aggregate(String source);

    /**
     * Returns a new query bound to the kind (a specific {@link MongoCollection})
     *
     * @param source The class to create aggregation against
     * @param <T>    the source type
     * @return the aggregation pipeline
     * @since 2.0
     */
    <T> Aggregation<T> aggregate(Class<T> source);

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
     * Find instances of a type
     *
     * @param type the class to use for mapping the results
     * @param <T>  the type to query
     * @return the query
     */
    <T> Query<T> find(Class<T> type);

    /**
     * Find instances of a type using a native query. This method is intended as an aid when copying queries from external sources such
     * as the shell or Compass whose structure is already in json form.
     *
     * @param type        the class to use for mapping the results
     * @param nativeQuery the full query structure to use for this Query
     * @param <T>         the type to query
     * @return the query
     * @morphia.experimental
     * @since 2.3
     */
    @MorphiaExperimental
    <T> Query<T> find(Class<T> type, Document nativeQuery);

    /**
     * @param type the type look up
     * @param <T>  the class type
     * @return the collection mapped for this class
     * @morphia.internal
     * @hidden
     * @since 2.3
     */
    @MorphiaInternal
    <T> MongoCollection<T> getCollection(Class<T> type);

    /**
     * @return the MongoDatabase used by this DataStore
     * @morphia.internal
     * @hidden
     * @since 1.5
     */
    @MorphiaInternal
    MongoDatabase getDatabase();

    /**
     * Inserts an entity in to the mapped collection.
     *
     * @param entity the entity to insert
     * @param <T>    the type of the entity
     */
    default <T> void insert(T entity) {
        insert(entity, new InsertOneOptions());
    }

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
     * @return the new merged entity. NOTE: this is a database fetch.
     */
    <T> T merge(T entity);

    /**
     * Work as if you did an update with each field in the entity doing a $set; Only at the top level of the entity.
     *
     * @param entity  the entity to merge back in to the database
     * @param options the options to apply
     * @param <T>     the type of the entity
     * @return the new merged entity. NOTE: this is a database fetch.
     * @since 2.0
     */
    <T> T merge(T entity, InsertOneOptions options);

    /**
     * Returns a new query based on the example object
     *
     * @param example the example entity to use when creating the query
     * @param <T>     the type of the entity
     * @return the query
     */
    <T> Query<T> queryByExample(T example);

    /**
     * Refreshes an existing entity to its current state in the database. Essentially, any existing mapped state is replaced by the
     * latest persisted state while preserving the entity's reference and object identity.
     *
     * @param entity the entity to refresh
     * @param <T>    the entity type
     * @since 2.0
     */
    <T> void refresh(T entity);

    /**
     * Replaces a document in the database
     *
     * @param entity the entity to replace
     * @param <T>    the type of the entity
     * @return the replaced entity
     * @since 2.3
     */
    default <T> T replace(T entity) {
        return replace(entity, new ReplaceOptions());
    }

    /**
     * Replaces a document in the database
     *
     * @param entity  the entity to replace
     * @param options the options to apply to the replace operation
     * @param <T>     the type of the entity
     * @return the replaced entity
     * @since 2.3
     */
    <T> T replace(T entity, ReplaceOptions options);

    /**
     * Replaces a list of documents in the database
     *
     * @param entities the entities to replace
     * @param <T>      the type of the entity
     * @return the saved entities
     * @since 2.3
     */
    default <T> List<T> replace(List<T> entities) {
        return replace(entities, new ReplaceOptions());
    }

    /**
     * Replaces a list of documents in the database
     *
     * @param entities the entities to replace
     * @param <T>      the type of the entity
     * @param options  the options to apply to the replace operation
     * @return the saved entities
     * @since 2.3
     */
    <T> List<T> replace(List<T> entities, ReplaceOptions options);

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
     */
    <T> T save(T entity, InsertOneOptions options);

    /**
     * Starts a new session on the server.
     *
     * @return the new session reference
     * @since 2.0
     */
    MorphiaSession startSession();

    /**
     * Starts a new session on the server.
     *
     * @param options the options to apply
     * @return the new session reference
     * @since 2.0
     */
    MorphiaSession startSession(ClientSessionOptions options);

    /**
     * @param transaction the transaction wrapper
     * @param <T>         the return type
     * @return the return value
     * @since 2.0
     */
    @Nullable
    <T> T withTransaction(MorphiaTransaction<T> transaction);

    /**
     * @param <T>         the return type
     * @param options     the session options to apply
     * @param transaction the transaction wrapper
     * @return the return value
     * @since 2.0
     */
    @Nullable
    <T> T withTransaction(ClientSessionOptions options, MorphiaTransaction<T> transaction);
}
