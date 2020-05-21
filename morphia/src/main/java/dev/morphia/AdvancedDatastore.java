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

import java.util.List;

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
     * @morphia.internal
     * @see DBDecoderFactory
     * @deprecated unused in 2.0 and will be removed
     */
    @Deprecated
    DBDecoderFactory getDecoderFact();

    /**
     * Sets the DBDecoderFactory to use in this Datastore
     *
     * @param fact the DBDecoderFactory to use
     * @morphia.internal
     * @deprecated unused in 2.0 and will be removed
     */
    @Deprecated
    void setDecoderFact(DBDecoderFactory fact);

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
     * @deprecated this feature is being removed.  no replacement is planned.  see issue #1331
     */
    @Deprecated
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
     * Deletes an entity of the given type T, with the given {@code id}, from the collection with the name in the {@code kind} param.
     * Validates the {@code id}, checking it's the correct type for an ID for entities of type {@code T}. The entity type {@code clazz} is
     * used only for validation, not for filtering, therefore if you have entities of different types in the same collection ({@code
     * kind}),
     * this method will delete any entity with the given {@code id}, regardless of its type.
     *
     * @param kind  the collection name
     * @param clazz the Class of the entity to delete
     * @param id    the value of the ID
     * @param <T>   the entity type
     * @param <V>   is the type of the ID, for example ObjectId
     * @return the result of this delete operation.
     * @morphia.inline
     * @deprecated use {@link #find(String, Class)} and {@link #delete(Query)} instead
     */
    @Deprecated
    <T, V> WriteResult delete(String kind, Class<T> clazz, V id);

    /**
     * Deletes an entity of the given type T, with the given {@code id}, from the collection with the name in the {@code kind} param.
     * Validates the {@code id}, checking it's the correct type for an ID for entities of type {@code T}. The entity type {@code clazz} is
     * used only for validation, not for filtering, therefore if you have entities of different types in the same collection ({@code
     * kind}),
     * this method will delete any entity with the given {@code id}, regardless of its type.
     *
     * @param kind    the collection name
     * @param clazz   the Class of the entity to delete
     * @param id      the value of the ID
     * @param options the options to use when deleting
     * @param <T>     the entity type
     * @param <V>     is the type of the ID, for example ObjectId
     * @return the result of this delete operation.
     * @morphia.inline
     * @since 1.3
     * @deprecated use {@link #find(String, Class)} and {@link #delete(Query, DeleteOptions)} instead
     */
    @Deprecated
    <T, V> WriteResult delete(String kind, Class<T> clazz, V id, DeleteOptions options);

    /**
     * Deletes an entity of the given type T, with the given {@code id}, from the collection with the name in the {@code kind} param.
     * Validates the {@code id}, checking it's the correct type for an ID for entities of type {@code T}. The entity type {@code clazz} is
     * used only for validation, not for filtering, therefore if you have entities of different types in the same collection ({@code
     * kind}),
     * this method will delete any entity with the given {@code id}, regardless of its type.
     *
     * @param kind  the collection name
     * @param clazz the Class of the entity to delete
     * @param id    the value of the ID
     * @param wc    the WriteConcern for this operation
     * @param <T>   the entity type
     * @param <V>   is the type of the ID, for example ObjectId
     * @return the result of this delete operation.
     * @deprecated use {@link #find(String, Class)} and {@link #delete(Query, DeleteOptions)} instead
     */
    @Deprecated
    <T, V> WriteResult delete(String kind, Class<T> clazz, V id, WriteConcern wc);

    /**
     * Ensures (creating if necessary) the index including the field(s) + directions on the given collection name; eg fields = "field1,
     * -field2" ({field1:1, field2:-1})
     *
     * @param collection the collection to update
     * @param clazz      the class from which to get the index definitions
     * @param fields     the fields to index
     * @param <T>        the type to index
     * @see MongoCollection#createIndex(org.bson.conversions.Bson, com.mongodb.client.model.IndexOptions)
     * @see #ensureIndexes(String, Class)
     * @deprecated This method uses the legacy approach for defining indexes.  Switch to using annotations on entity classes or the
     * methods in the Java driver itself.
     */
    @Deprecated
    <T> void ensureIndex(String collection, Class<T> clazz, String fields);

    /**
     * Ensures (creating if necessary) the index including the field(s) + directions on the given collection name; eg fields = "field1,
     * -field2" ({field1:1, field2:-1})
     *
     * @param collection       the collection to update
     * @param clazz            the class from which to get the index definitions
     * @param name             the name of the index to create
     * @param fields           the fields to index
     * @param unique           true if the index should enforce uniqueness on the fields indexed
     * @param dropDupsOnCreate This value is no longer supported.  Tells the unique index to drop duplicates silently when creating;
     *                         only the first will be kept
     * @param <T>              the type to index
     * @see MongoCollection#createIndex(org.bson.conversions.Bson, com.mongodb.client.model.IndexOptions)
     * @see #ensureIndexes(String, Class)
     * @deprecated This method uses the legacy approach for defining indexes.  Switch to using annotations on entity classes or the
     * methods in the Java driver itself.
     */
    @Deprecated
    <T> void ensureIndex(String collection, Class<T> clazz, String name,
                         String fields, boolean unique, boolean dropDupsOnCreate);

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
     * Ensures (creating if necessary) the indexes found during class mapping (using {@code @Indexed, @Indexes)} on the given collection
     * name, possibly in the background
     *
     * @param collection the collection to update
     * @param clazz      the class from which to get the index definitions
     * @param background if true, the index will be built in the background.  If false, the method will block until the index is created.
     * @param <T>        the type to index
     * @deprecated use {@link #ensureIndexes(String, Class)} instead and specify background true or false in the annotations
     */
    @Deprecated
    <T> void ensureIndexes(String collection, Class<T> clazz, boolean background);

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
     * Find all instances by type in a different collection than what is mapped on the class given skipping some documents and returning a
     * fixed number of the remaining.
     *
     * @param collection the collection to query against
     * @param clazz      the class to use for mapping the results
     * @param property   the document property to query against
     * @param value      the value to check for
     * @param offset     the number of results to skip
     * @param size       the maximum number of results to return
     * @param <T>        the type to query
     * @param <V>        the type to filter value
     * @return the query
     * @morphia.inline
     * @deprecated use {@link #find(String, Class)} and apply the options to the {@link com.mongodb.client.model.FindOptions} parameter
     */
    @Deprecated
    <T, V> Query<T> find(String collection, Class<T> clazz, String property, V value, int offset, int size);

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
     * Finds an entity in the named collection whose id matches the value given.
     *
     * @param collection the collection to query
     * @param clazz      the class to use for mapping
     * @param id         the ID to query
     * @param <T>        the type to fetch
     * @param <V>        the type of the ID
     * @return the entity with the id.  May be null.
     * @morphia.inline
     * @deprecated use {@link #find(String, Class)} instead
     */
    @Deprecated
    <T, V> T get(String collection, Class<T> clazz, V id);

    /**
     * Gets the count this collection
     *
     * @param collection the collection to count
     * @return the collection size
     * @morphia.inline
     * @deprecated use {@link Query#count()} instead
     */
    @Deprecated
    long getCount(String collection);

    /**
     * Inserts an entity in to the mapped collection.
     *
     * <i>The return type will change in 2.0</i>
     *
     * @param entity the entity to insert
     * @param <T>    the type of the entity
     * @return the new key of the inserted entity
     */
    <T> Key<T> insert(T entity);

    /**
     * Inserts an entity in to the mapped collection.
     *
     * @param entity the entity to insert
     * @param wc     the WriteConcern to use when inserting
     * @param <T>    the type of the entity
     * @return the new key of the inserted entity
     * @morphia.inline
     * @see WriteConcern
     * @deprecated use {@link #insert(Object, InsertOptions)}
     */
    @Deprecated
    <T> Key<T> insert(T entity, WriteConcern wc);

    /**
     * Inserts an entity in to the mapped collection.
     *
     * <i>The return type will change in 2.0</i>
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
     * <i>The return type will change in 2.0</i>
     *
     * @param entities the entities to insert
     * @param <T>      the type of the entities
     * @return the new keys of the inserted entities
     * @deprecated use {@link #insert(List)}
     */
    @Deprecated
    <T> Iterable<Key<T>> insert(Iterable<T> entities);

    /**
     * Inserts entities in to the mapped collection.
     *
     * <i>The return type will change in 2.0</i>
     *
     * @param entities the entities to insert
     * @param <T>      the type of the entities
     * @return the new keys of the inserted entities
     * @since 1.6
     */
    <T> Iterable<Key<T>> insert(List<T> entities);

    /**
     * Inserts entities in to the mapped collection.
     *
     * @param entities the entities to insert
     * @param wc       the WriteConcern to use when inserting
     * @param <T>      the type of the entity
     * @return the new keys of the inserted entities
     * @deprecated use {@link #insert(List, InsertOptions)}
     */
    @Deprecated
    <T> Iterable<Key<T>> insert(Iterable<T> entities, WriteConcern wc);

    /**
     * Inserts entities in to the mapped collection.
     *
     * <i>The return type will change in 2.0</i>
     *
     * @param entities the entities to insert
     * @param options  the options to apply to the insert operation
     * @param <T>      the type of the entity
     * @return the new keys of the inserted entities
     * @since 1.3
     * @deprecated use {@link #insert(List, InsertOptions)}
     */
    @Deprecated
    <T> Iterable<Key<T>> insert(Iterable<T> entities, InsertOptions options);

    /**
     * Inserts entities in to the mapped collection.
     *
     * <i>The return type will change in 2.0</i>
     *
     * @param entities the entities to insert
     * @param options  the options to apply to the insert operation
     * @param <T>      the type of the entity
     * @return the new keys of the inserted entities
     * @since 1.6
     */
    <T> Iterable<Key<T>> insert(List<T> entities, InsertOptions options);

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
     * Inserts an entity in to the named collection.
     *
     * @param collection the collection to update
     * @param entities   the entities to insert
     * @param wc         the WriteConcern to use when inserting
     * @param <T>        the type of the entity
     * @return the new keys of the inserted entities
     * @see WriteConcern
     * @deprecated use {@link #insert(String, Iterable, InsertOptions)} instead
     */
    @Deprecated
    <T> Iterable<Key<T>> insert(String collection, Iterable<T> entities, WriteConcern wc);

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
     * @param wc         the WriteConcern to use when inserting
     * @param <T>        the type of the entity
     * @return the new key of the inserted entity
     * @deprecated use {@link #save(String, Object, InsertOptions)} instead
     */
    @Deprecated
    <T> Key<T> save(String collection, T entity, WriteConcern wc);

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
