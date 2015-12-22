package org.mongodb.morphia.query;


import com.mongodb.Bytes;
import org.mongodb.morphia.Key;

import java.util.List;

/**
 * The results of a query.  These results aren't materialized until a method on this interface is called.
 *
 * @param <T>
 */
public interface QueryResults<T> extends Iterable<T> {
    /**
     * Execute the query and get the results (as a {@code List<Key<T>>})  This method is provided as a convenience;
     *
     * @return returns a List of the keys of the documents returned by a query
     */
    List<Key<T>> asKeyList();

    /**
     * Execute the query and get the results.
     *
     * @return returns a List of the documents returned by a query
     */
    List<T> asList();

    /**
     * Count the total number of values in the result, ignoring limit and offset
     *
     * @return the count
     */
    long countAll();

    /**
     * Execute the query and get the results.  This method is provided for orthogonality; Query.fetch().iterator() is identical to
     * Query.iterator().
     *
     * @return an Iterator of the results
     */
    MorphiaIterator<T, T> fetch();

    /**
     * Execute the query and get only the ids of the results.  This is more efficient than fetching the actual results (transfers less
     * data).
     *
     * @return an Iterator of the empty entities
     */
    MorphiaIterator<T, T> fetchEmptyEntities();

    /**
     * Execute the query and get the keys for the objects.
     *
     * @return the Key Iterator
     * @see #fetchEmptyEntities
     */
    MorphiaKeyIterator<T> fetchKeys();

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
     * @param awaitData passes the awaitData to the cursor
     * @return an Iterator.
     * @see Bytes#QUERYOPTION_AWAITDATA
     */
    MorphiaIterator<T, T> tail(boolean awaitData);
}
