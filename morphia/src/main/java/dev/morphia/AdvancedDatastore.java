package dev.morphia;

import com.mongodb.DBObject;
import com.mongodb.DBRef;
import dev.morphia.aggregation.AggregationPipeline;
import dev.morphia.query.Query;
import dev.morphia.query.UpdateOperations;
import org.bson.Document;

import java.util.List;

/**
 * This interface exposes advanced {@link Datastore} features, like interacting with Document and low-level options. It implements matching
 * methods from the {@code Datastore} interface but with a specified kind (collection name), or raw types (Document).
 *
 * @author ScottHernandez
 */
public interface AdvancedDatastore extends Datastore {

    /**
     * Returns an {@link AggregationPipeline} bound to the given collection and class.
     *
     * @param collection the collection to query
     * @param clazz      The class to create aggregation against
     * @return the aggregation pipeline
     */
    AggregationPipeline createAggregation(String collection, Class<?> clazz);

    /**
     * @param <T>        The type of the entity
     * @param collection the collection to query
     * @param clazz      the class of objects to be returned
     * @return Query for the specified class clazz
     */
    <T> Query<T> createQuery(String collection, Class<T> clazz);

    /**
     * @param <T>   The type of the entity
     * @param clazz the class of objects to be returned
     * @param q     the query which will be passed to a {@link dev.morphia.query.QueryFactory}
     * @return Query for the specified class clazz
     */
    <T> Query<T> createQuery(Class<T> clazz, Document q);

    /**
     * Creates a reference to the entity (using the current DB -can be null-, the collectionName, and id)
     *
     * @param clazz The type of the entity
     * @param id    The ID value of the entity
     * @param <T>   The type of the entity
     * @param <V>   The type of the ID value
     * @return the DBRef for the entity
     */
    <T, V> DBRef createRef(Class<T> clazz, V id);

    /**
     * Creates a reference to the entity (using the current DB -can be null-, the collectionName, and id)
     *
     * @param <T>    The type of the entity
     * @param entity the entity to create a DBRef for
     * @return the DBRef for the entity
     */
    <T> DBRef createRef(T entity);

    /**
     * Creates an UpdateOperations instance for the given type.
     *
     * @param <T>  The type of the entity
     * @param type The type of the entity
     * @param ops  The operations to perform
     * @return the UpdateOperations instance
     * @deprecated use {@link Query#update()} instead
     */
    @Deprecated(since = "2.0", forRemoval = true)
    default <T> UpdateOperations<T> createUpdateOperations(Class<T> type, DBObject ops) {
        return createUpdateOperations(type, new Document(ops.toMap()));
    }

    /**
     * Creates an UpdateOperations instance for the given type.
     *
     * @param <T>  The type of the entity
     * @param type The type of the entity
     * @param ops  The operations to perform
     * @return the UpdateOperations instance
     * @deprecated use {@link Query#update()} instead
     */
    @Deprecated(since = "2.0", forRemoval = true)
    <T> UpdateOperations<T> createUpdateOperations(Class<T> type, Document ops);

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
     * @since 1.3
     * @deprecated use {@link #insert(List, InsertManyOptions)} instead
     * @morphia.inline
     */
    @Deprecated(since = "2.0", forRemoval = true)
    default <T> void insert(T entity, InsertOptions options) {
        insert(entity, options.toInsertOneOptions());
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
     * Inserts entities in to the mapped collection.
     *
     * @param entities the entities to insert
     * @param <T>      the type of the entities
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
     * Inserts entities in to the mapped collection.
     *
     * @param entities the entities to insert
     * @param options  the options to apply to the insert operation
     * @param <T>      the type of the entity
     * @morphia.inline
     * @since 1.3
     * @deprecated use {@link #insert(List, InsertManyOptions)} instead
     */
    @Deprecated(since = "2.0", forRemoval = true)
    default <T> void insert(List<T> entities, InsertOptions options) {
        insert(entities, options.toInsertManyOptions());
    }

    /**
     * Returns a new query based on the example object
     *
     * @param collection the collection to query
     * @param example    the example entity to use when building the query
     * @param <T>        the type of the entity
     * @return the query
     */
    <T> Query<T> queryByExample(String collection, T example);
}
