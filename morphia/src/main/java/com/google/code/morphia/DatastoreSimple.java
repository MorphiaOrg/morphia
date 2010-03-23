package com.google.code.morphia;

import com.google.code.morphia.utils.Key;

/**
 * Datastore interface to get/delete/save objects with a Long/String id
 * @author Scott Hernandez
 *
 */
public interface DatastoreSimple {
	/** Find the given entity (by id); shorthand for {@code findOne("_id =", ids)} */
	<T> T get(Object clazzOrEntity, long id);
	/** Find the given entity (by id); shorthand for {@code findOne("_id =", ids)} */
	<T> T get(Object clazzOrEntity, String id);

	/** Find the given entities (by id); shorthand for {@code find("_id in", ids)} */
	<T> Query<T> get(Object clazzOrEntity, long[] ids);
	/** Find the given entities (by id); shorthand for {@code find("_id in", ids)} */
	<T> Query<T> get(Object clazzOrEntity, String[] ids);

	/** Deletes the given entity (by id) */
	<T> void delete(T entity);
	/** Deletes the given entity (by id) */
	<T> void delete(Object clazzOrEntity, long id);
	/** Deletes the given entity (by id) */
	<T> void delete(Object clazzOrEntity, String id);
	/** Deletes the given entities (by id) */
	<T> void delete(Object clazzOrEntity, long[] ids);
	/** Deletes the given entities (by id) */
	<T> void delete(Object clazzOrEntity, String[] ids);

	/** Saves the entity (Object) and updates the @MongoID, @MondoCollectionName fields */
	<T> Key<T> save(T entity);
	/** Saves the entities (Objects) and updates the @MongoID, @MondoCollectionName fields */
	<T> Iterable<Key<T>> save(Iterable<T> entities);

	/** 
	 * <p>
	 * Find all instances by collectionName, and filter property.
	 * </p><p>
	 * This is the same as: {@code find(clazzOrEntity).filter(property, value); }
	 * </p>
	 */
	<T> Query<T> find(Object clazzOrEntity, String property, Object value);
	
	/** 
	 * <p>
	 * Find all instances by collectionName, and filter property.
	 * </p><p>
	 * This is the same as: {@code find(clazzOrEntity).filter(property, value).offset(offset).limit(size); }
	 * </p>
	 */
	<T> Query<T> find(Object clazzOrEntity, String property, Object value, int offset, int size);

	/** Gets the count of the CollectionName */
	<T> long getCount(Object clazzOrEntity);

	/** Ensures (creating if necessary) the indexes found during class mapping (using {@code @Indexed)}*/
	void ensureSuggestedIndexes(); 
}