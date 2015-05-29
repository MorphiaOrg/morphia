package org.mongodb.morphia.query;


import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.ReadPreference;
import org.bson.types.CodeWScope;

import java.util.Map;
import java.util.concurrent.TimeUnit;


/**
 * @param <T> The java type to query against
 * @author Scott Hernandez
 */
public interface Query<T> extends QueryResults<T>, Cloneable {
    CriteriaContainer and(Criteria... criteria);

    /**
     * Batch-size of the fetched result (cursor).
     *
     * @param value must be >= 0.  A value of 0 indicates the server default.
     */
    Query<T> batchSize(int value);

    /**
     * Creates and returns a copy of this {@link Query}.
     */
    Query<T> cloneQuery();

    /**
     * This makes it possible to attach a comment to a query. Because these comments propagate to the profile log, adding comments can make
     * your profile data much easier to interpret and trace.
     *
     * @param comment the comment to add
     * @return the Query to enable chaining of commands
     * @see <a href="http://docs.mongodb.org/manual/reference/operator/meta/comment/#op._S_comment">
     * http://docs.mongodb.org/manual/reference/operator/meta/comment/#op._S_comment</a>
     */
    Query<T> comment(String comment);

    /**
     * Criteria builder interface
     */
    FieldEnd<? extends CriteriaContainerImpl> criteria(String field);

    /**
     * Disables cursor timeout on server.
     */
    Query<T> disableCursorTimeout();

    /**
     * Disable snapshotted mode (default mode). This will be faster but changes made during the cursor may cause duplicates. *
     */
    Query<T> disableSnapshotMode();

    /**
     * Turns off validation (for all calls made after)
     */
    Query<T> disableValidation();

    /**
     * Enables cursor timeout on server.
     */
    Query<T> enableCursorTimeout();

    /**
     * Enabled snapshotted mode where duplicate results (which may be updated during the lifetime of the cursor) will not be returned. Not
     * compatible with order/sort and hint. *
     */
    Query<T> enableSnapshotMode();

    /**
     * Turns on validation (for all calls made after); by default validation is on
     */
    Query<T> enableValidation();

    /**
     * Provides information on the query plan. The query plan is the plan the server uses to find the matches for a query. This information
     * may be useful when optimizing a query.
     *
     * @return Map describing the process used to return the query results.
     * @see <a href="http://docs.mongodb.org/manual/reference/operator/meta/explain/"> http://docs.mongodb
     * .org/manual/reference/operator/meta/explain/</a>
     */
    Map<String, Object> explain();

    /**
     * Fluent query interface: {@code createQuery(Ent.class).field("count").greaterThan(7)...}
     */
    FieldEnd<? extends Query<T>> field(String field);

    /**
     * <p>Create a filter based on the specified condition and value. </p> <p><b>Note</b>: Property is in the form of "name op" ("age
     * >").</p> <p>Valid operators are ["=", "==","!=", "<>", ">", "<", ">=", "<=", "in", "nin", "all", "size", "exists"] </p>
     * <p>Examples:</p>
     * <p/>
     * <ul> <li>{@code filter("yearsOfOperation >", 5)}</li> <li>{@code filter("rooms.maxBeds >=", 2)}</li> <li>{@code
     * filter("rooms.bathrooms exists", 1)}</li> <li>{@code filter("stars in", new Long[]{3, 4}) //3 and 4 stars (midrange?)}</li>
     * <li>{@code filter("quantity mod", new Long[]{4, 0}) // customers ordered in packs of 4)}</li> <li>{@code filter("age >=", age)}</li>
     * <li>{@code filter("age =", age)}</li> <li>{@code filter("age", age)} (if no operator, = is assumed)</li> <li>{@code filter("age !=",
     * age)}</li> <li>{@code filter("age in", ageList)}</li> <li>{@code filter("customers.loyaltyYears in", yearsList)}</li> </ul>
     * <p/>
     * <p>You can filter on id properties <strong>if</strong> this query is restricted to a Class<T>.
     */
    Query<T> filter(String condition, Object value);

    /**
     * Returns the batch size
     *
     * @see #batchSize(int)
     */
    int getBatchSize();

    /**
     * Returns the {@link DBCollection} of the {@link Query}.
     */
    DBCollection getCollection();

    /**
     * Returns the entity {@link Class}.
     */
    Class<T> getEntityClass();

    /**
     * Returns the Mongo fields {@link DBObject}.
     */
    DBObject getFieldsObject();

    /**
     * Returns the limit
     *
     * @see #limit(int)
     */
    int getLimit();

    /**
     * Returns the offset.
     *
     * @see #offset(int)
     */
    int getOffset();

    /**
     * Returns the Mongo query {@link DBObject}.
     */
    DBObject getQueryObject();

    /**
     * Returns the Mongo sort {@link DBObject}.
     */
    DBObject getSortObject();

    /**
     * Hints as to which index should be used.
     */
    Query<T> hintIndex(String idxName);

    /**
     * Limit the fetched result set to a certain number of values.
     *
     * @param value must be >= 0.  A value of 0 indicates no limit.  For values < 0, use {@link #batchSize(int)} which is the preferred
     *              method
     */
    Query<T> limit(int value);

    /**
     * <p> Specify the inclusive lower bound for a specific index in order to constrain the results of this query. <p/> You can chain
     * key/value pairs to build a constraint for a compound index. For instance: </p> <p> {@code query.lowerIndexBound(new
     * BasicDBObject("a", 1).append("b", 2)); } </p> <p> to build a constraint on index {@code {"a", "b"}} </p>
     *
     * @param lowerBound The inclusive lower bound.
     * @see <a href="http://docs.mongodb.org/manual/reference/operator/meta/min/"> http://docs.mongodb
     * .org/manual/reference/operator/meta/min/</a>
     */
    Query<T> lowerIndexBound(DBObject lowerBound);

    /**
     * Constrains the query to only scan the specified number of documents when fulfilling the query.
     *
     * @param value must be > 0.  A value < 0 indicates no limit
     * @see <a href="http://docs.mongodb.org/manual/reference/operator/meta/maxScan/#op._S_maxScan">
     * http://docs.mongodb.org/manual/reference/operator/meta/maxScan/#op._S_maxScan</a>
     */
    Query<T> maxScan(int value);

    /**
     * Specifies a time limit for executing the query. Requires server version 2.6 or above.
     *
     * @param maxTime     must be > 0.  A value < 0 indicates no limit
     * @param maxTimeUnit
     */
    Query<T> maxTime(long maxTime, TimeUnit maxTimeUnit);

    /**
     * Starts the query results at a particular zero-based offset.
     *
     * @param value must be >= 0
     */
    Query<T> offset(int value);

    CriteriaContainer or(Criteria... criteria);

    /**
     * <p>Sorts based on a property (defines return order).  Examples:</p>
     * <p/>
     * <ul> <li>{@code order("age")}</li> <li>{@code order("-age")} (descending order)</li> <li>{@code order("age, date")}</li> <li>{@code
     * order("age,-date")} (age ascending, date descending)</li> </ul>
     */
    Query<T> order(String condition);

    /**
     * Route query to non-primary node
     *
     * @see ReadPreference#secondary()
     * @see ReadPreference#secondaryPreferred()
     * @deprecated use #useReadPreference(ReadPreference) instead
     */
    Query<T> queryNonPrimary();

    /**
     * Route query to primary node
     *
     * @see ReadPreference#primary()
     * @deprecated use #useReadPreference(ReadPreference)
     */
    Query<T> queryPrimaryOnly();

    /**
     * Limits the fields retrieved to those of the query type -- dangerous with interfaces and abstract classes
     */
    Query<T> retrieveKnownFields();

    /**
     * Limits the fields retrieved
     */
    Query<T> retrievedFields(boolean include, String... fields);

    /**
     * Only return the index field or fields for the results of the query. If $returnKey is set to true and the query does not use an index
     * to perform the read operation, the returned documents will not contain any fields
     *
     * @return the Query to enable chaining of commands
     * @see <a href="http://docs.mongodb.org/manual/reference/operator/meta/returnKey/#op._S_returnKey">Return Key</a>
     */
    Query<T> returnKey();

    /**
     * Perform a text search on the content of the fields indexed with a text index..
     *
     * @param text the text to search for
     * @return the Query to enable chaining of commands
     * @see <a href="http://docs.mongodb.org/manual/reference/operator/query/text/">Text Search</a>
     */
    Query<T> search(String text);

    /**
     * Perform a text search on the content of the fields indexed with a text index..
     *
     * @param text     the text to search for
     * @param language the language to use during the search
     * @return the Query to enable chaining of commands
     * @see <a href="http://docs.mongodb.org/manual/reference/operator/query/text/">Text Search</a>
     */
    Query<T> search(String text, String language);

    /**
     * <p>Generates a string that consistently and uniquely specifies this query.  There is no way to convert this string back into a query
     * and there is no guarantee that the string will be consistent across versions.</p> <p/> <p>In particular, this value is useful as a
     * key for a simple memcache query cache.</p>
     */
    String toString();

    /**
     * <p> Specify the exclusive upper bound for a specific index in order to constrain the results of this query. <p/> You can chain
     * key/value pairs to build a constraint for a compound index. For instance: </p> <p> {@code query.upperIndexBound(new
     * BasicDBObject("a", 1).append("b", 2)); } </p> <p> to build a constraint on index {@code {"a", "b"}} </p>
     *
     * @param upperBound The exclusive upper bound.
     * @see <a href="http://docs.mongodb.org/manual/reference/operator/meta/max/"> http://docs.mongodb
     * .org/manual/reference/operator/meta/max/</a>
     */
    Query<T> upperIndexBound(DBObject upperBound);

    /**
     * Route query ReadPreference
     */
    Query<T> useReadPreference(ReadPreference readPref);

    /**
     * Limit the query using this javascript block; only one per query
     */
    Query<T> where(String js);

    /**
     * Limit the query using this javascript block; only one per query
     */
    Query<T> where(CodeWScope js);
}
