package dev.morphia;

import java.util.List;
import java.util.Objects;

import com.mongodb.ClientSessionOptions;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.lang.Nullable;

import dev.morphia.aggregation.Aggregation;
import dev.morphia.aggregation.AggregationOptions;
import dev.morphia.annotations.internal.MorphiaExperimental;
import dev.morphia.annotations.internal.MorphiaInternal;
import dev.morphia.query.FindOptions;
import dev.morphia.query.Query;
import dev.morphia.sofia.Sofia;
import dev.morphia.transactions.MorphiaSession;
import dev.morphia.transactions.MorphiaTransaction;

import org.bson.Document;

/**
 * Datastore interface to get/delete/save objects
 */
@SuppressWarnings({ "UnusedReturnValue", "unused", "removal" })
public interface Datastore {
    /**
     * Returns a new aggregation bound without an initial type. Calls to this method define the collection to be used via the options
     * passed.
     *
     * @return the aggregation pipeline
     * @since 3.0
     * @see AggregationOptions#collection(String)
     */
    default Aggregation<Document> aggregate(AggregationOptions options) {
        Objects.requireNonNull(options.collection(), Sofia.aggregationCollectionName());
        return aggregate(Document.class, Document.class, options);
    }

    /**
     * Returns a new query bound to the kind (a specific {@link MongoCollection})
     *
     * @param source The class to create aggregation against
     * @param <S>    the source type
     * @return the aggregation pipeline
     * @since 2.0
     */
    default <S> Aggregation<S> aggregate(Class<S> source) {
        return aggregate(source, source, new AggregationOptions());
    }

    /**
     * Returns a new aggregation bound to the kind (a specific {@link MongoCollection})
     *
     * @param source The class to create aggregation against
     * @param <S>    the source type
     * @return the aggregation pipeline
     * @since 3.0
     */
    default <S> Aggregation<S> aggregate(Class<S> source, AggregationOptions options) {
        return aggregate(source, source, options);
    }

    /**
     * Returns a new query bound to the kind (a specific {@link MongoCollection})
     *
     * @param source The class to create aggregation against
     * @param <S>    the source type
     * @param <T>    the target type
     * @return the aggregation pipeline
     * @since 3.0
     */
    default <S, T> Aggregation<T> aggregate(Class<S> source, Class<T> target) {
        return aggregate(source, target, new AggregationOptions());
    }

    /**
     * Returns a new aggregation bound to the kind (a specific {@link MongoCollection}). To specify an alternate collection as the
     * source collection use {@link AggregationOptions#collection(String)}.
     *
     * @param source The source type to aggregate. May be null if an alternate collection is being used.
     * @param <S>    the source type
     * @param <T>    the target type
     * @return the aggregation pipeline
     * @since 3.0
     */
    <S, T> Aggregation<T> aggregate(@Nullable Class<S> source, Class<T> target, AggregationOptions options);

    /**
     * Deletes the given entity (by @Id)
     *
     * @param <T>    the type to delete
     * @param entity the entity to delete
     * @return results of the deletion
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
    default <T> Query<T> find(Class<T> type) {
        return find(type, new FindOptions());
    }

    /**
     * Find instances of a type
     *
     * @param type    the class to use for mapping the results
     * @param options the options to apply to the query
     * @param <T>     the type to query
     * @return the query
     * @since 2.5
     */
    <T> Query<T> find(Class<T> type, FindOptions options);

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
     * Find instances of a type using a native query. This method is intended as an aid when copying queries from external sources such
     * as the shell or Compass whose structure is already in json form.
     *
     * @param <T>         the type to query
     * @param type        the class to use for mapping the results
     * @param nativeQuery the full query structure to use for this Query
     * @param options     the options to apply
     * @return the query
     * @morphia.experimental
     * @since 2.3
     */
    @MorphiaExperimental
    <T> Query<T> find(Class<T> type, Document nativeQuery, FindOptions options);

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
     * Shards any collections with sharding definitions.
     *
     * @morphia.experimental
     * @since 2.3
     */
    @MorphiaExperimental
    void shardCollections();

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
