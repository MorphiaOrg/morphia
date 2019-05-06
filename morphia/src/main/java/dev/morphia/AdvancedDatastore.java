package dev.morphia;

import com.mongodb.DBDecoderFactory;
import com.mongodb.DBObject;
import com.mongodb.DBRef;
import com.mongodb.ReadPreference;
import com.mongodb.WriteConcern;
import com.mongodb.WriteResult;
import com.mongodb.client.MongoCollection;
import dev.morphia.aggregation.AggregationPipeline;
import dev.morphia.query.Query;
import dev.morphia.query.UpdateOperations;

/**
 * This interface exposes advanced {@link Datastore} features, like interacting with DBObject and low-level options. It implements matching
 * methods from the {@code Datastore} interface but with a specified kind (collection name), or raw types (DBObject).
 *
 * @author ScottHernandez
 */
public interface AdvancedDatastore extends Datastore {

    /**
     * Returns the DBDecoderFactory used by this Datastore
     *
     * @return the decoder factory
     * @see DBDecoderFactory
     * @morphia.internal
     */
    DBDecoderFactory getDecoderFact();

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
    <T> Query<T> createQuery(Class<T> clazz, DBObject q);

    /**
     * @param <T>        The type of the entity
     * @param collection the collection to query
     * @param clazz      the class of objects to be returned
     * @param q          the query which will be passed to a {@link dev.morphia.query.QueryFactory}
     * @return Query for the specified class clazz
     * @deprecated this feature is being removed.  no replacement is planned.  see issue #1331
     */
    @Deprecated
    <T> Query<T> createQuery(String collection, Class<T> clazz, DBObject q);

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
     */
    <T> UpdateOperations<T> createUpdateOperations(Class<T> type, DBObject ops);

    /**
     * Ensures (creating if necessary) the indexes found during class mapping (using {@code @Indexed, @Indexes)} on the given collection
     * name.
     *
     * @param collection the collection to update
     * @param clazz      the class from which to get the index definitions
     * @param <T>        the type to index
     * @deprecated this feature is being removed.  no replacement is planned.  see issue #1331
     */
    @Deprecated
    <T> void ensureIndexes(String collection, Class<T> clazz);

    /**
     * Checks that an entity exists for the given key or entity
     *
     * @param keyOrEntity    the value to check for
     * @param readPreference Uses the supplied ReadPreference for the check.  If readPreference is null the preference is taken from the
     *                       annotation or uses the default preference.
     * @return the key if the entity exists
     * @morphia.inline
     * @see #exists(Object)
     * @deprecated use {@link Query#first()} instead
     */
    @Deprecated
    Key<?> exists(Object keyOrEntity, ReadPreference readPreference);

    /**
     * Find all instances by type in a different collection than what is mapped on the class given.
     *
     * @param collection the collection to query against
     * @param clazz      the class to use for mapping the results
     * @param <T>        the type to query
     * @return the query
     * @deprecated this feature is being removed.  no replacement is planned.  see issue #1331
     */
    @Deprecated
    <T> Query<T> find(String collection, Class<T> clazz);

    /**
     * Find the given entity (by collectionName/id);
     *
     * @param clazz the class to use for mapping
     * @param ref   the DBRef to use when querying
     * @param <T>   the type to fetch
     * @return the entity referenced in the DBRef.  May be null.
     * @morphia.inline
     * @deprecated use {@link #find(Class)} instead
     */
    @Deprecated
    <T> T get(Class<T> clazz, DBRef ref);

    /**
     * Inserts an entity in to the mapped collection.
     *
     * @param entity the entity to insert
     * @param <T>    the type of the entity
     * @return the new key of the inserted entity
     */
    <T> Key<T> insert(T entity);

    /**
     * Inserts an entity in to the mapped collection.
     *
     * @param entity  the entity to insert
     * @param options the options to apply to the insert operation
     * @param <T>     the type of the entity
     * @return the new key of the inserted entity
     * @morphia.inline
     * @since 1.3
     */
    <T> Key<T> insert(T entity, InsertOptions options);

    /**
     * Inserts an entity in to the named collection.
     *
     * @param collection the collection to update
     * @param entity     the entity to insert
     * @param <T>        the type of the entity
     * @return the new key of the inserted entity
     * @deprecated this feature is being removed.  no replacement is planned.  see issue #1331
     */
    @Deprecated
    <T> Key<T> insert(String collection, T entity);

    /**
     * Inserts an entity in to the named collection.
     *
     * @param collection the collection to update
     * @param entity     the entity to insert
     * @param options    the options to apply to the insert operation
     * @param <T>        the type of the entity
     * @return the new key of the inserted entity
     * @morphia.inline
     * @since 1.3
     * @deprecated this feature is being removed.  no replacement is planned.  see issue #1331
     */
    @Deprecated
    <T> Key<T> insert(String collection, T entity, InsertOptions options);

    /**
     * Inserts entities in to the mapped collection.
     *
     * @param entities the entities to insert
     * @param <T>      the type of the entity
     * @return the new keys of the inserted entities
     * @morphia.inline
     * @deprecated use {@link #insert(Iterable)} instead
     */
    @Deprecated
    <T> Iterable<Key<T>> insert(T... entities);

    /**
     * Inserts entities in to the mapped collection.
     *
     * @param entities the entities to insert
     * @param <T>      the type of the entities
     * @return the new keys of the inserted entities
     */
    <T> Iterable<Key<T>> insert(Iterable<T> entities);

    /**
     * Inserts entities in to the mapped collection.
     *
     * @param entities the entities to insert
     * @param wc       the WriteConcern to use when inserting
     * @param <T>      the type of the entity
     * @return the new keys of the inserted entities
     * @deprecated use {@link #insert(Iterable, InsertOptions)}
     */
    @Deprecated
    <T> Iterable<Key<T>> insert(Iterable<T> entities, WriteConcern wc);

    /**
     * Inserts entities in to the mapped collection.
     *
     * @param entities the entities to insert
     * @param options  the options to apply to the insert operation
     * @param <T>      the type of the entity
     * @return the new keys of the inserted entities
     * @morphia.inline
     * @since 1.3
     */
    <T> Iterable<Key<T>> insert(Iterable<T> entities, InsertOptions options);

    /**
     * Inserts an entity in to the named collection.
     *
     * @param collection the collection to update
     * @param entities   the entities to insert
     * @param <T>        the type of the entity
     * @return the new keys of the inserted entities
     * @deprecated this feature is being removed.  no replacement is planned.  see issue #1331
     */
    @Deprecated
    <T> Iterable<Key<T>> insert(String collection, Iterable<T> entities);

    /**
     * Inserts entities in to the named collection.
     *
     * @param collection the collection to update
     * @param entities   the entities to insert
     * @param options    the options to apply to the insert operation
     * @param <T>        the type of the entity
     * @return the new keys of the inserted entities
     * @since 1.3
     * @deprecated this feature is being removed.  no replacement is planned.  see issue #1331
     */
    @Deprecated
    <T> Iterable<Key<T>> insert(String collection, Iterable<T> entities, InsertOptions options);

    /**
     * Returns a new query based on the example object
     *
     * @param collection the collection to query
     * @param example    the example entity to use when building the query
     * @param <T>        the type of the entity
     * @return the query
     */
    <T> Query<T> queryByExample(String collection, T example);

    /**
     * Saves an entity in to the named collection.
     *
     * @param collection the collection to update
     * @param entity     the entity to save
     * @param <T>        the type of the entity
     * @return the new key of the inserted entity
     * @deprecated this feature is being removed.  no replacement is planned.  see issue #1331
     */
    @Deprecated
    <T> Key<T> save(String collection, T entity);

    /**
     * Saves an entity in to the named collection.
     *
     * @param collection the collection to update
     * @param entity     the entity to save
     * @param options    the options to apply to the save operation
     * @param <T>        the type of the entity
     * @return the new key of the inserted entity
     * @deprecated this feature is being removed.  no replacement is planned.  see issue #1331
     */
    @Deprecated
    <T> Key<T> save(String collection, T entity, InsertOptions options);
}
