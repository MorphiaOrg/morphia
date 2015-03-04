package org.mongodb.morphia.query;


import org.mongodb.morphia.Key;

import java.util.List;

/**
 * The results of a query.  These results aren't materialized until a method on this interface is called.
 * @param <T>
 */
public interface QueryResults<T> extends Iterable<T> {
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
     * <p>Execute the query and get the results (as a {@code List<T>}).</p> <p>  This method is provided as a convenience; {@code List<T>
     * results = new ArrayList<T>; for(T ent : fetch()) results.add(ent); return results;} </p>
     */
    List<T> asList();

    /**
     * Execute the query and get the results (as a {@code List<Key<T>>})  This method is provided as a convenience;
     */
    List<Key<T>> asKeyList();

    /**
     * Execute the query and get the results.  This method is provided for orthogonality; Query.fetch().iterator() is identical to
     * Query.iterator().
     */
    MorphiaIterator<T, T> fetch();

    /**
     * Execute the query and get only the ids of the results.  This is more efficient than fetching the actual results (transfers less
     * data).
     */
    MorphiaIterator<T, T> fetchEmptyEntities();

    /**
     * Execute the query and get the keys for the objects. @see fetchEmptyEntities
     */
    MorphiaKeyIterator<T> fetchKeys();

    /**
     * <p>Count the total number of values in the result, <strong>ignoring <em>limit</em> and <em>offset</em>.</p>
     */
    long countAll();

    /**
     * Calls {@code tail(true);}
     *
     * @return an Iterator.
     * @see #tail(boolean)
     */
    MorphiaIterator<T, T> tail();

    /**
     * Returns an tailing iterator over a set of elements of type T. If awaitData is true, this iterator blocks on hasNext() until new data
     * is avail (or some amount of time has passed). Note that if no data is available at all, hasNext() might return immediately. You
     * should wrap tail calls in a loop if you want this to be blocking.
     *
     * @return an Iterator.
     */
    MorphiaIterator<T, T> tail(boolean awaitData);
}
