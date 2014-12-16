package org.mongodb.morphia;

import com.mongodb.DBDecoderFactory;
import com.mongodb.DBObject;
import com.mongodb.DBRef;
import com.mongodb.ReadPreference;
import com.mongodb.WriteConcern;
import com.mongodb.WriteResult;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;

/**
 * This interface exposes advanced {@link Datastore} features, like interacting with DBObject and low-level options. It implements matching
 * methods from the {@code Datastore} interface but with a specified kind (collection name), or raw types (DBObject).
 *
 * @author ScottHernandez
 */
public interface AdvancedDatastore extends Datastore {

    /**
     * @param readPreference Uses the supplied ReadPreference for the check.  If readPreference is null the preference is taken from the
     *                       annotation or uses the default preference.
     * @see #exists(Object)
     */
    Key<?> exists(Object keyOrEntity, ReadPreference readPreference);

    /**
     * Creates a reference to the entity (using the current DB -can be null-, the collectionName, and id)
     */
    <T, V> DBRef createRef(Class<T> clazz, V id);

    /**
     * Creates a reference to the entity (using the current DB -can be null-, the collectionName, and id)
     */
    <T> DBRef createRef(T entity);

    /**
     * Find the given entity (by collectionName/id);
     */
    <T> T get(Class<T> clazz, DBRef ref);

    /**
     * Gets the count this kind
     */
    long getCount(String kind);

    <T, V> T get(String kind, Class<T> clazz, V id);

    <T> Query<T> find(String kind, Class<T> clazz);

    <T, V> Query<T> find(String kind, Class<T> clazz, String property, V value, int offset, int size);

    <T> Key<T> save(String kind, T entity);

    <T> Key<T> save(String kind, T entity, WriteConcern wc);

    /**
     * Deletes an entity of the given type T, with the given {@code id}, from the collection with the name in the {@code kind} param.
     * Validates the {@code id}, checking it's the correct type for an ID for entities of type {@code T}. The entity type {@code clazz} is
     * used only for validation, not for filtering, therefore if you have entities of different types in the same collection ({@code kind}),
     * this method will delete any entity with the given {@code id}, regardless of its type.
     *
     * @param kind  the collection name
     * @param clazz the Class of the entity to delete
     * @param id    the value of the ID
     * @param <T>   the entity type
     * @param <V>   is the type of the ID, for example ObjectId
     * @return the result of this delete operation.
     */
    <T, V> WriteResult delete(String kind, Class<T> clazz, V id);

    /**
     * Deletes an entity of the given type T, with the given {@code id}, from the collection with the name in the {@code kind} param.
     * Validates the {@code id}, checking it's the correct type for an ID for entities of type {@code T}. The entity type {@code clazz} is
     * used only for validation, not for filtering, therefore if you have entities of different types in the same collection ({@code kind}),
     * this method will delete any entity with the given {@code id}, regardless of its type.
     *
     * @param kind  the collection name
     * @param clazz the Class of the entity to delete
     * @param id    the value of the ID
     * @param wc    the WriteConcern for this operation
     * @param <T>   the entity type
     * @param <V>   is the type of the ID, for example ObjectId
     * @return the result of this delete operation.
     */
    <T, V> WriteResult delete(String kind, Class<T> clazz, V id, WriteConcern wc);

    <T> Key<T> insert(String kind, T entity);

    <T> Key<T> insert(T entity);

    <T> Key<T> insert(T entity, WriteConcern wc);

    <T> Iterable<Key<T>> insert(T... entities);

    <T> Iterable<Key<T>> insert(Iterable<T> entities, WriteConcern wc);

    <T> Iterable<Key<T>> insert(String kind, Iterable<T> entities);

    <T> Iterable<Key<T>> insert(String kind, Iterable<T> entities, WriteConcern wc);

    /**
     * @param kind  the name of the collection that should be queried
     * @param clazz the class of objects to be returned
     * @return Query for the specified class clazz
     */
    <T> Query<T> createQuery(String kind, Class<T> clazz);

    /**
   * 
     * @param kind the class of objects to be returned
     * @param q    the query which will be passed to a {@link org.mongodb.morphia.query.QueryFactory}
     * @return Query for the specified class clazz
     */
    <T> Query<T> createQuery(Class<T> kind, DBObject q);

    <T> Query<T> createQuery(String kind, Class<T> clazz, DBObject q);

    /**
     * Returns a new query based on the example object
     */
    <T> Query<T> queryByExample(String kind, T example);


    <T> UpdateOperations<T> createUpdateOperations(Class<T> kind, DBObject ops);

    DBDecoderFactory setDecoderFact(DBDecoderFactory fact);

    DBDecoderFactory getDecoderFact();

    /**
     * Ensures (creating if necessary) the indexes found during class mapping (using {@code @Indexed, @Indexes)} on the given collection
     * name.
     */
    <T> void ensureIndexes(String collName, Class<T> clazz);

    /**
     * Ensures (creating if necessary) the indexes found during class mapping (using {@code @Indexed, @Indexes)} on the given collection
     * name, possibly in the background
     */
    <T> void ensureIndexes(String collName, Class<T> clazz, boolean background);

    /**
     * Ensures (creating if necessary) the index including the field(s) + directions on the given collection name; eg fields = "field1,
     * -field2" ({field1:1, field2:-1})
     */
    <T> void ensureIndex(String collName, Class<T> clazz, String fields);

    /**
     * Ensures (creating if necessary) the index including the field(s) + directions on the given collection name; eg fields = "field1,
     * -field2" ({field1:1, field2:-1})
     */
    <T> void ensureIndex(String collName, Class<T> clazz, String name,
                         String fields, boolean unique, boolean dropDupsOnCreate);

}