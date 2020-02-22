package dev.morphia.query;


import com.mongodb.client.result.DeleteResult;
import dev.morphia.DeleteOptions;
import dev.morphia.FindAndModifyOptions;
import dev.morphia.query.experimental.filters.Filter;
import dev.morphia.query.internal.MorphiaCursor;
import dev.morphia.query.internal.MorphiaKeyCursor;
import dev.morphia.sofia.Sofia;
import org.bson.Document;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static dev.morphia.query.MorphiaQuery.legacyOperation;


/**
 * @param <T> The java type to query against
 */
public interface Query<T> extends CriteriaContainer {
    /**
     * Creates a container to hold 'and' clauses
     *
     * @param criteria the clauses to 'and' together
     * @return the container
     */
    @Deprecated(since = "2.0", forRemoval = true)
    default CriteriaContainer and(final Criteria... criteria) {
        return legacyOperation();
    }

    /**
     * Creates a criteria to apply against a field
     *
     * @param field the field
     * @return the FieldEnd to define the criteria
     */
    @Deprecated(since = "2.0", forRemoval = true)
    default FieldEnd<? extends CriteriaContainer> criteria(final String field) {
        return legacyOperation();
    }

    /**
     * Adds filters to this query.  This operation is cumulative.
     *
     * @param filters the filters to add
     * @return this
     */
    default Query<T> filter(Filter... filters) {
        throw new UnsupportedOperationException(Sofia.notAvailableInLegacy());
    }

    /**
     * Turns off validation (for all calls made after)
     *
     * @return this
     */
    Query<T> disableValidation();

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
    default Map<String, Object> explain() {
        return explain(new FindOptions());
    }

    /**
     * Provides information on the query plan. The query plan is the plan the server uses to find the matches for a query. This information
     * may be useful when optimizing a query.
     *
     * @param options the options to apply to the explain operation
     * @return Map describing the process used to return the query results.
     * @mongodb.driver.manual reference/operator/meta/explain/ explain
     * @since 1.3
     */
    Map<String, Object> explain(FindOptions options);

    /**
     * Allows the use of aggregation expressions within the query language.
     * @param expression the expression to include
     * @return this
     * @since 2.0
     */
    //    Query<T> expr(Expression expression);

    /**
     * Creates a container to hold 'or' clauses
     *
     * @param criteria the clauses to 'or' together
     * @return the container
     */
    @Deprecated(since = "2.0", forRemoval = true)
    default CriteriaContainer or(final Criteria... criteria) {
        return legacyOperation();
    }

    /**
     * Fluent query interface: {@code createQuery(Ent.class).field("count").greaterThan(7)...}
     *
     * @param name the field
     * @return the FieldEnd to define the criteria
     */
    @Deprecated(since = "2.0", forRemoval = true)
    default FieldEnd<? extends Query<T>> field(final String name) {
        return legacyOperation();
    }

    /**
     * Create a filter based on the specified condition and value. </p>
     *
     * <p><b>Note</b>: Property is in the form of "name op" ("age &gt;").
     * <p/>
     * <p>Valid operators are ["=", "==","!=", "&lt;&gt;", "&gt;", "&lt;", "&gt;=", "&lt;=", "in", "nin", "all", "size", "exists"] </p>
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
    @Deprecated(since = "2.0", forRemoval = true)
    default Query<T> filter(final String condition, final Object value) {
        return legacyOperation();
    }

    /**
     * This is only intended for migration of legacy uses of UpdateOperations
     *
     * @param operations the prebuilt operations
     * @return the Modify instance
     * @morphia.internal
     * @since 2.0
     * @deprecated
     */
    @Deprecated(since = "2.0", forRemoval = true)
    default Modify<T> modify(final UpdateOperations<T> operations) {
        return legacyOperation();
    }

    /**
     * Sorts based on a metadata (defines return order). Example:
     * {@code order(Meta.textScore())}  ({textScore : { $meta: "textScore" }})
     *
     * @param sort the sort order to apply
     * @return this
     */
    @Deprecated(since = "2.0", forRemoval = true)
    default Query<T> order(final Meta sort) {
        return legacyOperation();
    }

    /**
     * Sorts based on a specified sort keys (defines return order).
     *
     * @param sorts the sort order to apply
     * @return this
     */
    @Deprecated(since = "2.0", forRemoval = true)
    default Query<T> order(final Sort... sorts) {
        return legacyOperation();
    }

    /**
     * @return the document form of this query
     * @morphia.internal
     */
    Document toDocument();

    /**
     * Adds a field to the projection clause.  Passing true for include will include the field in the results.  Projected fields must all
     * be inclusions or exclusions.  You can not include and exclude fields at the same time with the exception of the _id field.  The
     * _id field is always included unless explicitly suppressed.
     *
     * @param field   the field to project
     * @param include true to include the field in the results
     * @return this
     * @see <a href="https://docs.mongodb.com/manual/tutorial/project-fields-from-query-results/">Project Fields to Return from Query</a>
     * @deprecated use {@link FindOptions#projection()}
     */
    @Deprecated(since = "2.0", forRemoval = true)
    default Query<T> project(final String field, final boolean include) {
        return legacyOperation();
    }

    /**
     * Adds an sliced array field to a projection.
     *
     * @param field the field to project
     * @param slice the options for projecting an array field
     * @return this
     * @mongodb.driver.manual /reference/operator/projection/slice/ $slice
     * @see <a href="https://docs.mongodb.com/manual/tutorial/project-fields-from-query-results/">Project Fields to Return from Query</a>
     * @deprecated use {@link FindOptions#projection()}
     */
    @Deprecated(since = "2.0", forRemoval = true)
    default Query<T> project(final String field, final ArraySlice slice) {
        return legacyOperation();
    }

    /**
     * Adds a metadata field to a projection.
     *
     * @param meta the metadata option for projecting
     * @return this
     * @mongodb.driver.manual reference/operator/projection/meta/ $meta
     * @see <a href="https://docs.mongodb.com/manual/tutorial/project-fields-from-query-results/">Project Fields to Return from Query</a>
     * @deprecated use {@link FindOptions#projection()}
     */
    @Deprecated(since = "2.0", forRemoval = true)
    default Query<T> project(final Meta meta) {
        return legacyOperation();
    }

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

    /**
     * Limit the query using this javascript block; only one per query
     *
     * @param js the javascript block to apply
     * @return this
     */
    Query<T> where(String js);

    /**
     * Execute the query and get the results (as a {@code MorphiaCursor<Key<T>>})
     *
     * @return the keys of the documents returned by this query
     */
    MorphiaKeyCursor<T> keys();

    /**
     * Execute the query and get the results (as a {@code MorphiaCursor<Key<T>>})
     *
     * @param options the options to apply to the find operation
     * @return the keys of the documents returned by this query
     * @since 1.4
     */
    MorphiaKeyCursor<T> keys(FindOptions options);

    /**
     * Count the total number of values in the result, ignoring limit and offset
     *
     * @return the count
     * @since 1.3
     */
    long count();

    /**
     * Count the total number of values in the result, ignoring limit and offset
     *
     * @param options the options to apply to the count operation
     * @return the count
     * @since 1.3
     */
    long count(CountOptions options);

    /**
     * Execute the query and get the results.
     * <p>
     * *note* the return type of this will change in 2.0.
     *
     * @return a MorphiaCursor
     * @see #execute(FindOptions)
     * @since 1.4
     * @deprecated use {@link #execute()}
     */
    @Deprecated(since = "2.0", forRemoval = true)
    default MorphiaCursor<T> find() {
        return execute();
    }

    /**
     * Execute the query and get the results.
     * <p>
     * *note* the return type of this will change in 2.0.
     *
     * @return a MorphiaCursor
     * @see #execute(FindOptions)
     * @since 1.4
     */
    MorphiaCursor<T> execute();

    /**
     * Execute the query and get the results.
     *
     * @param options the options to apply to the find operation
     * @return a MorphiaCursor
     * @since 1.4
     * @deprecated use {@link #execute(FindOptions)}
     */
    @Deprecated(since = "2.0", forRemoval = true)
    default MorphiaCursor<T> find(FindOptions options) {
        return execute(options);
    }

    /**
     * Execute the query and get the results.
     *
     * @param options the options to apply to the find operation
     * @return a MorphiaCursor
     * @since 1.4
     */
    MorphiaCursor<T> execute(FindOptions options);

    /**
     * Gets the first entity in the result set.  Obeys the {@link Query} offset value.
     *
     * @return the only instance in the result, or null if the result set is empty.
     * @since 1.5
     */
    T first();

    /**
     * Gets the first entity in the result set.  Obeys the {@link Query} offset value.
     *
     * @param options the options to apply to the find operation
     * @return the only instance in the result, or null if the result set is empty.
     * @since 1.5
     */
    T first(FindOptions options);

    /**
     * Deletes an entity from the database and returns it.
     *
     * @return the deleted entity
     */
    default T delete() {
        return delete(new FindAndDeleteOptions());
    }

    /**
     * Deletes an entity from the database and returns it.
     *
     * @param options the options to apply
     * @return the deleted entity
     */
    T delete(FindAndDeleteOptions options);

    /**
     * Deletes an entity from the database and returns it.
     *
     * @param options the options to apply
     * @return the deleted entity
     * @deprecated use {@link #delete(FindAndDeleteOptions)}
     */
    @Deprecated(since = "2.0", forRemoval = true)
    default T delete(FindAndModifyOptions options) {
        return delete(new FindAndDeleteOptions()
                          .writeConcern(options.getWriteConcern())
                          .collation(options.getCollation())
                          .maxTime(options.getMaxTime(TimeUnit.MILLISECONDS), TimeUnit.MILLISECONDS)
                          .sort(options.getSort())
                          .projection(options.getProjection()));

    }

    /**
     * Create a modify operation based on this query
     *
     * @return the modify operation
     */
    Modify<T> modify();

    /**
     * Limits the fields retrieved to those of the query type -- dangerous with interfaces and abstract classes
     *
     * @return this
     * @deprecated use {@link FindOptions#projection()}
     */
    @Deprecated(since = "2.0", forRemoval = true)
    default Query<T> retrieveKnownFields() {
        return legacyOperation();
    }

    /**
     * Deletes elements matching this query
     *
     * @return the results
     * @see DeleteOptions
     */
    default DeleteResult remove() {
        return remove(new DeleteOptions());
    }

    /**
     * Deletes documents matching this query.  Optionally deleting the first or all matched documents.
     *
     * @param options the options to apply
     * @return the results
     */
    DeleteResult remove(DeleteOptions options);

    /**
     * Creates an update operation based on this query
     *
     * @return the update operation
     */
    Update<T> update();

    /**
     * @param operations the prebuilt operations
     * @return the Updates instance
     * @morphia.internal
     * @since 2.0
     * @deprecated
     */
    @Deprecated(since = "2.0", forRemoval = true)
    default Update<T> update(final UpdateOperations operations) {
        return legacyOperation();
    }
}
