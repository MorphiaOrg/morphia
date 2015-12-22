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
    /**
     * Creates a container to hold 'and' clauses
     *
     * @param criteria the clauses to 'and' together
     * @return the container
     */
    CriteriaContainer and(Criteria... criteria);

    /**
     * Batch-size of the fetched result (cursor).
     *
     * @param value must be >= 0.  A value of 0 indicates the server default.
     * @return this
     */
    Query<T> batchSize(int value);

    /**
     * Creates and returns a copy of this {@link Query}.
     *
     * @return this
     */
    Query<T> cloneQuery();

    /**
     * This makes it possible to attach a comment to a query. Because these comments propagate to the profile log, adding comments can make
     * your profile data much easier to interpret and trace.
     *
     * @param comment the comment to add
     * @return the Query to enable chaining of commands
     * @mongodb.driver.manual reference/operator/meta/comment $comment
     */
    Query<T> comment(String comment);

    /**
     * Creates a criteria to apply against a field
     *
     * @param field the field
     * @return the FieldEnd to define the criteria
     */
    FieldEnd<? extends CriteriaContainerImpl> criteria(String field);

    /**
     * Disables cursor timeout on server.
     *
     * @return this
     */
    Query<T> disableCursorTimeout();

    /**
     * Disable snapshotted mode (default mode). This will be faster but changes made during the cursor may cause duplicates. *
     *
     * @return this
     */
    Query<T> disableSnapshotMode();

    /**
     * Turns off validation (for all calls made after)
     *
     * @return this
     */
    Query<T> disableValidation();

    /**
     * Enables cursor timeout on server.
     *
     * @return this
     */
    Query<T> enableCursorTimeout();

    /**
     * Enabled snapshotted mode where duplicate results (which may be updated during the lifetime of the cursor) will not be returned. Not
     * compatible with order/sort and hint. *
     *
     * @return this
     */
    Query<T> enableSnapshotMode();

    /**
     * Turns on validation (for all calls made after); by default validation is on
     *
     * @return this
     */
    Query<T> enableValidation();

    /**
     * Provides information on the query plan. The query plan is the plan the server uses to find the matches for a query. This information
     * may be useful when optimizing a query.
     *
     * @return Map describing the process used to return the query results.
     * @mongodb.driver.manual reference/operator/meta/explain/ explain
     */
    Map<String, Object> explain();

    /**
     * Fluent query interface: {@code createQuery(Ent.class).field("count").greaterThan(7)...}
     *
     * @param field the field
     * @return the FieldEnd to define the criteria
     */
    FieldEnd<? extends Query<T>> field(String field);

    /**
     * Create a filter based on the specified condition and value. </p> <p><b>Note</b>: Property is in the form of "name op" ("age
     * >").
     * <p/>
     * <p>Valid operators are ["=", "==","!=", "<>", ">", "<", ">=", "<=", "in", "nin", "all", "size", "exists"] </p>
     * <p/>
     * <p>Examples:</p>
     * <p/>
     * <ul>
     * <li>{@code filter("yearsOfOperation >", 5)}</li>
     * <li>{@code filter("rooms.maxBeds >=", 2)}</li>
     * <li>{@code filter("rooms.bathrooms exists", 1)}</li>
     * <li>{@code filter("stars in", new Long[]{3, 4}) //3 and 4 stars (midrange?)}</li>
     * <li>{@code filter("quantity mod", new Long[]{4, 0}) // customers ordered in packs of 4)}</li>
     * <li>{@code filter("age >=", age)}</li>
     * <li>{@code filter("age =", age)}</li>
     * <li>{@code filter("age", age)} (if no operator, = is assumed)</li>
     * <li>{@code filter("age !=", age)}</li>
     * <li>{@code filter("age in", ageList)}</li>
     * <li>{@code filter("customers.loyaltyYears in", yearsList)}</li>
     * </ul>
     * <p/>
     * <p>You can filter on id properties <strong>if</strong> this query is restricted to a Class<T>.
     *
     * @param condition the condition to apply
     * @param value     the value to apply against
     * @return this
     */
    Query<T> filter(String condition, Object value);

    /**
     * @return the batch size
     * @see #batchSize(int)
     */
    int getBatchSize();

    /**
     * @return the {@link DBCollection} of the {@link Query}.
     */
    DBCollection getCollection();

    /**
     * @return the entity {@link Class}.
     */
    Class<T> getEntityClass();

    /**
     * @return the Mongo fields {@link DBObject}.
     */
    DBObject getFieldsObject();

    /**
     * @return the limit
     * @see #limit(int)
     */
    int getLimit();

    /**
     * @return the offset.
     * @see #offset(int)
     */
    int getOffset();

    /**
     * @return the Mongo query {@link DBObject}.
     */
    DBObject getQueryObject();

    /**
     * @return the Mongo sort {@link DBObject}.
     */
    DBObject getSortObject();

    /**
     * Hints as to which index should be used.
     *
     * @param idxName the index name to hint
     * @return this
     */
    Query<T> hintIndex(String idxName);

    /**
     * Limit the fetched result set to a certain number of values.
     *
     * @param value must be >= 0.  A value of 0 indicates no limit.  For values < 0, use {@link #batchSize(int)} which is the preferred
     *              method
     * @return this
     */
    Query<T> limit(int value);

    /**
     * <p> Specify the inclusive lower bound for a specific index in order to constrain the results of this query. <p/> You can chain
     * key/value pairs to build a constraint for a compound index. For instance: </p> <p> {@code query.lowerIndexBound(new
     * BasicDBObject("a", 1).append("b", 2)); } </p> <p> to build a constraint on index {@code {"a", "b"}} </p>
     *
     * @param lowerBound The inclusive lower bound.
     * @return this
     * @mongodb.driver.manual reference/operator/meta/min/ $min
     */
    Query<T> lowerIndexBound(DBObject lowerBound);

    /**
     * Constrains the query to only scan the specified number of documents when fulfilling the query.
     *
     * @param value must be > 0.  A value < 0 indicates no limit
     * @return this
     * @mongodb.driver.manual reference/operator/meta/maxScan/#op._S_maxScan $maxScan
     */
    Query<T> maxScan(int value);

    /**
     * Specifies a time limit for executing the query. Requires server version 2.6 or above.
     *
     * @param maxTime     must be > 0.  A value < 0 indicates no limit
     * @param maxTimeUnit the unit of time to use
     * @return this
     */
    Query<T> maxTime(long maxTime, TimeUnit maxTimeUnit);

    /**
     * Starts the query results at a particular zero-based offset.
     *
     * @param value must be >= 0
     * @return this
     */
    Query<T> offset(int value);

    /**
     * Creates a container to hold 'or' clauses
     *
     * @param criteria the clauses to 'or' together
     * @return the container
     */
    CriteriaContainer or(Criteria... criteria);

    /**
     * Sorts based on a property (defines return order).  Examples:
     * <p/>
     * <ul>
     * <li>{@code order("age")}</li>
     * <li>{@code order("-age")} (descending order)</li>
     * <li>{@code order("age, date")}</li>
     * <li>{@code order("age,-date")} (age ascending, date descending)</li>
     * </ul>
     *
     * @param sort the sort order to apply
     * @return this
     */
    Query<T> order(String sort);

    /**
     * Route query to non-primary node
     *
     * @return this
     * @see ReadPreference#secondary()
     * @see ReadPreference#secondaryPreferred()
     * @deprecated use #useReadPreference(ReadPreference) instead
     */
    Query<T> queryNonPrimary();

    /**
     * Route query to primary node
     *
     * @return this
     * @see ReadPreference#primary()
     * @deprecated use #useReadPreference(ReadPreference)
     */
    Query<T> queryPrimaryOnly();

    /**
     * Limits the fields retrieved to those of the query type -- dangerous with interfaces and abstract classes
     *
     * @return this
     */
    Query<T> retrieveKnownFields();

    /**
     * Limits the fields retrieved
     *
     * @param include true if the fields should be included in the results.  false to exclude them.
     * @param fields  the fields in question
     * @return this
     */
    Query<T> retrievedFields(boolean include, String... fields);

    /**
     * Only return the index field or fields for the results of the query. If $returnKey is set to true and the query does not use an index
     * to perform the read operation, the returned documents will not contain any fields
     *
     * @return the Query to enable chaining of commands
     * @mongodb.driver.manual reference/operator/meta/returnKey/#op._S_returnKey $returnKey
     */
    Query<T> returnKey();

    /**
     * Perform a text search on the content of the fields indexed with a text index..
     *
     * @param text the text to search for
     * @return the Query to enable chaining of commands
     * @mongodb.driver.manual reference/operator/query/text/ $text
     */
    Query<T> search(String text);

    /**
     * Perform a text search on the content of the fields indexed with a text index..
     *
     * @param text     the text to search for
     * @param language the language to use during the search
     * @return the Query to enable chaining of commands
     * @mongodb.driver.manual reference/operator/query/text/ $text
     */
    Query<T> search(String text, String language);

    @Override
    String toString();

    /**
     * <p> Specify the exclusive upper bound for a specific index in order to constrain the results of this query. <p/> You can chain
     * key/value pairs to build a constraint for a compound index. For instance: </p> <p> {@code query.upperIndexBound(new
     * BasicDBObject("a", 1).append("b", 2)); } </p> <p> to build a constraint on index {@code {"a", "b"}} </p>
     *
     * @param upperBound The exclusive upper bound.
     * @return this
     * @mongodb.driver.manual reference/operator/meta/max/ $max
     */
    Query<T> upperIndexBound(DBObject upperBound);

    /**
     * Updates the ReadPreference to use
     *
     * @param readPref the ReadPreference to use
     * @return this
     * @see ReadPreference
     */
    Query<T> useReadPreference(ReadPreference readPref);

    /**
     * Limit the query using this javascript block; only one per query
     *
     * @param js the javascript block to apply
     * @return this
     */
    Query<T> where(String js);

    /**
     * Limit the query using this javascript block; only one per query
     *
     * @param js the javascript block to apply
     * @return this
     */
    Query<T> where(CodeWScope js);
}
