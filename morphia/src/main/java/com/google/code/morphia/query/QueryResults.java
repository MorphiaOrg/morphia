package com.google.code.morphia.query;

import java.util.List;

import com.google.code.morphia.Key;

public interface QueryResults<T>  extends Iterable<T> {
	/**
	 * Gets the first entity in the result set.  Obeys the {@link Query} offset value.
	 * 
	 * @return the only instance in the result, or null if the result set is empty.
	 */
	T get();

	/**
	 * Get the key of the first entity in the result set.  Obeys the {@link Query} offset value.
	 * 
	 * @return the key of the first instance in the result, or null if the result set is empty.
	 */
	Key<T> getKey();
	
	/**
	 * <p>Execute the query and get the results (as a {@code List<T>}).</p>
	 * <p>  This method is provided as a convenience;
	 * {@code List<T> results = new ArrayList<T>; for(T ent : fetch()) results.add(ent); return results;}
	 * </p>
	 */
	List<T> asList();
	
	/**
	 * Execute the query and get the results (as a {@code List<Key<T>>})  This method is provided as a convenience;
	 */
	List<Key<T>> asKeyList();	

	/**
	 * Execute the query and get the results.  This method is provided for orthogonality;
	 * Query.fetch().iterator() is identical to Query.iterator().
	 */
	Iterable<T> fetch();
	
	/**
	 * Execute the query and get the ids of the results.  This is more efficient than
	 * fetching the actual results (transfers less data).
	 */
	Iterable<T> fetchIdsOnly();

	/**
	 * Execute the query and get the keys for the objects. @see fetchIdsOnly 
	 */
	Iterable<Key<T>> fetchKeys();
	
	/**
	 * <p>Count the total number of values in the result, <strong>ignoring <em>limit</em> and <em>offset</em>.</p>
	 */
	long countAll();
}
