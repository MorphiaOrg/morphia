package com.google.code.morphia;

import com.google.code.morphia.utils.IndexDirection;
import com.mongodb.DB;
import com.mongodb.DBRef;
import com.mongodb.Mongo;
/**
 * Datastore interface to get/delete/save objects
 * @author Scott Hernandez
 */
public interface Datastore extends DatastoreSimple {	
	/** Creates a reference to the entity (using the current DB -can be null-, the collectionName, and id) */
	DBRef createRef(Object entity);
	/** Creates a reference to the entity (using the current DB -can be null-, the collectionName, and id) */
	DBRef createRef(Object clazzOrEntity, Object id);

	/** Find the given entity (by collectionName/id); think of this as refresh */
	<T> T get(Object entityOrRef);

	/** Deletes the given entity (by id) */
	<T> T get(Object clazzOrEntity, Object id);

	/** Deletes the given entity (by id) */
	<T> Query<T> get(Object clazzOrEntity, Object[] ids);

	/** Find all instances by collectionName */
	<T> Query<T> find(Object clazzOrEntity);

	/** Deletes the given entity (by id) */
	<T> void delete(Object clazzOrEntity, Object id);
	
	/** Gets the count of items returned by this query; same as {@code query.countAll()}*/
	<T> long getCount(Query<T> query);

	/** Ensures (creating if necessary) the index and direction */
	void ensureIndex(Object clazzOrEntity, String name, IndexDirection dir);
	
	/** The instance this Datastore is using */
	Morphia getMorphia();
	/** The instance this Datastore is using */
	Mongo getMongo();
	/** The instance this Datastore is using */
	DB getDB();
}