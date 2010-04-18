package com.google.code.morphia.query;

import java.util.List;

import com.google.code.morphia.Key;

/**
 * @author Scott Hernandez
 */
public interface Query<T> extends Iterable<T> {
	/**
	 * <p>Create a filter based on the specified condition and value.
	 * </p><p>
	 * <b>Note</b>: Property is in the form of "name op" ("age >").
	 * </p><p>
	 * Valid operators are ["=", "==","!=", "<>", ">", "<", ">=", "<=", "in", "nin", "all", "size", "exists"]
	 * </p>
	 * <p>Examples:</p>
	 * 
	 * <ul>
	 * <li>{@code filter("yearsOfOperation >", 5)}</li>
	 * <li>{@code filter("rooms.maxBeds >=", 2)}</li>
	 * <li>{@code filter("rooms.bathrooms exists", 1)}</li>
	 * <li>{@code filter("stars in", new Long[]{3,4}) //3 and 4 stars (midrange?)}</li>
	 * <li>{@code filter("age >=", age)}</li>
	 * <li>{@code filter("age =", age)}</li>
	 * <li>{@code filter("age", age)} (if no operator, = is assumed)</li>
	 * <li>{@code filter("age !=", age)}</li>
	 * <li>{@code filter("age in", ageList)}</li>
	 * <li>{@code filter("customers.loyaltyYears in", yearsList)}</li>
	 * </ul>
	 * 
	 * <p>You can filter on id properties <strong>if</strong> this query is
	 * restricted to a Class<T>.
	 */
	Query<T> filter(String condition, Object value);
	
	/**
	 * <p>Sorts based on a property.  Examples:</p>
	 * 
	 * <ul>
	 * <li>{@code sort("age")}</li>
	 * <li>{@code sort("-age")} (descending sort)</li>
	 * <li>{@code sort("age,date")}</li>
	 * <li>{@code sort("age,-date")} (age ascending, date descending)</li>
	 * </ul>
	 */
	Query<T> order(String condition);
	
	/**
	 * Limit the fetched result set to a certain number of values.
	 * 
	 * @param value must be >= 0.  A value of 0 indicates no limit.
	 */
	Query<T> limit(int value);
	
	/**
	 * Starts the query results at a particular zero-based offset.
	 * 
	 * @param value must be >= 0
	 */
	Query<T> offset(int value);

	
	/**
	 * <p>Generates a string that consistently and uniquely specifies this query.  There
	 * is no way to convert this string back into a query and there is no guarantee that
	 * the string will be consistent across versions.</p>
	 * 
	 * <p>In particular, this value is useful as a key for a simple memcache query cache.</p> 
	 */
	String toString();
	
	/**
	 * Gets the first entity in the result set.  Obeys the offset value.
	 * 
	 * @return the only instance in the result, or null if the result set is empty.
	 */
	T get();
	
	/**
	 * Execute the query and get the results (as a {@code List<T>}  This method is provided as a convenience;
	 * {@code List<T> results = new ArrayList<T>; for(T ent : fetch()} results.add(ent); return results;}
	 */
	List<T> asList();
	
	/**
	 * Execute the query and get the results (as a {@code List<Key<T>>}  This method is provided as a convenience;
	 */
	List<Key<T>> asKeyList();	

	/**
	 * Get the key of the first entity in the result set.  Obeys the offset value.
	 * 
	 * @return the key of the first instance in the result, or null if the result set is empty.
	 */
	Key<T> getKey();
	
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
	
	/** <p>The instance of the Class being used for the query</p> **/
	Class<T> getType();

	FieldPart<T> field(String fieldExpr);
}