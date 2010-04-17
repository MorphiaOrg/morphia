package com.google.code.morphia;

import com.google.code.morphia.query.Query;

/**
 * Implements matching methods from the {@code Datastore} but with a specified kind (collection name)
 * 
 * @author ScottHernandez
 */
public interface SuperDatastore extends Datastore {
	/** Gets the count this kind*/
	long getCount(String kind);
	<T,V> T get(String kind, Class<T> clazz, V id);
	<T> Query<T> find(String kind, Class<T> clazz);
	<T,V> Query<T> find(String kind, Class<T> clazz, String property, V value, int offset, int size);
	<T> Key<T> save(String kind, T entity);
	<T> void delete(String kind, T id);
}
