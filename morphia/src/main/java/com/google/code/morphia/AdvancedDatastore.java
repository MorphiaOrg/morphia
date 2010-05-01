package com.google.code.morphia;

import com.google.code.morphia.query.Query;
import com.mongodb.DBRef;

/**
 * <p>
 * This interface exposes advanced {@link Datastore} features.
 * 	
 *  <ul>
 * 		<li>Implements matching methods from the {@code Datastore} but with a specified kind (collection name). </li>
 * 	</ul>
 * </p>
 * @author ScottHernandez
 */
public interface AdvancedDatastore extends Datastore {
	/** Creates a reference to the entity (using the current DB -can be null-, the collectionName, and id) */
	<T,V> DBRef createRef(Class<T> clazz, V id);
	/** Creates a reference to the entity (using the current DB -can be null-, the collectionName, and id) */
	<T> DBRef createRef(T entity);

	/** Find the given entity (by collectionName/id);*/
	<T> T get(Class<T> clazz, DBRef ref);

	/** Gets the count this kind*/
	long getCount(String kind);
	<T,V> T get(String kind, Class<T> clazz, V id);
	<T> Query<T> find(String kind, Class<T> clazz);
	<T,V> Query<T> find(String kind, Class<T> clazz, String property, V value, int offset, int size);
	<T> Key<T> save(String kind, T entity);
	<T> void delete(String kind, T id);
}
