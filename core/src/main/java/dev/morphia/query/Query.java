package dev.morphia.query;


import com.mongodb.ExplainVerbosity;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.lang.Nullable;
import dev.morphia.DeleteOptions;
import dev.morphia.query.experimental.filters.Filter;
import dev.morphia.query.experimental.updates.UpdateOperator;
import dev.morphia.query.internal.MorphiaCursor;
import dev.morphia.query.internal.MorphiaKeyCursor;
import dev.morphia.sofia.Sofia;
import org.bson.Document;

import java.util.List;
import java.util.Map;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static dev.morphia.query.MorphiaQuery.legacyOperation;


/**
 * @param <T> The java type to query against
 */
@SuppressWarnings("removal")
public interface Query<T> extends CriteriaContainer, Iterable<T> {
    /**
     * Creates a container to hold 'and' clauses
     *
     * @param criteria the clauses to 'and' together
     * @return the container
     */
    @Deprecated(since = "2.0", forRemoval = true)
    default CriteriaContainer and(Criteria... criteria) {
        return legacyOperation();
    }

    /**
     * Creates a criteria to apply against a field
     *
     * @param field the field
     * @return the FieldEnd to define the criteria
     */
    @Deprecated(since = "2.0", forRemoval = true)
    default FieldEnd<? extends CriteriaContainer> criteria(String field) {
        return legacyOperation();
    }

    /**
     * Deletes elements matching this query
     *
     * @return the results
     * @see DeleteOptions
     */
    default DeleteResult delete() {
        return delete(new DeleteOptions());
    }

    /**
     * Adds filters to this query.  This operation is cumulative.
     *
     * @param filters the filters to add
     * @return this
     */
    default Query<T> filter(Filter... filters) {
        throw new UnsupportedOperationException(Sofia.modernOperation());
    }

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
     *
     * @return a MorphiaCursor
     * @see #iterator()
     */
    @Deprecated(since = "2.0", forRemoval = true)
    default MorphiaCursor<T> execute() {
        return legacyOperation();
    }

    /**
     * Deletes documents matching this query.  Optionally deleting the first or all matched documents.
     *
     * @param options the options to apply
     * @return the results
     */
    DeleteResult delete(DeleteOptions options);

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
     * Execute the query and get the results.
     *
     * @param options the options to apply to the find operation
     * @return a MorphiaCursor
     */
    @Deprecated(since = "2.0", forRemoval = true)
    default MorphiaCursor<T> execute(FindOptions options) {
        return legacyOperation();
    }

    /**
     * Execute the query and get the results.
     * <p>
     * *note* the return type of this will change in 2.0.
     *
     * @return a MorphiaCursor
     * @see #iterator(FindOptions)
     * @since 1.4
     * @deprecated use {@link #iterator()}
     */
    @Deprecated(since = "2.0", forRemoval = true)
    default MorphiaCursor<T> find() {
        return iterator();
    }

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
    default Map<String, Object> explain(FindOptions options) {
        return explain(options, null);
    }

    /**
     * Provides information on the query plan. The query plan is the plan the server uses to find the matches for a query. This information
     * may be useful when optimizing a query.
     *
     * @param options   the options to apply to the explain operation
     * @param verbosity the verbosity of the explanation
     * @return Map describing the process used to return the query results.
     * @mongodb.driver.manual reference/operator/meta/explain/ explain
     * @since 2.2
     */
    Map<String, Object> explain(FindOptions options, @Nullable ExplainVerbosity verbosity);

    /**
     * Fluent query interface: {@code createQuery(Ent.class).field("count").greaterThan(7)...}
     *
     * @param name the field
     * @return the FieldEnd to define the criteria
     */
    @Deprecated(since = "2.0", forRemoval = true)
    default FieldEnd<? extends Query<T>> field(String name) {
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
     * @deprecated use {@link #filter(Filter...)} instead
     */
    @Deprecated(since = "2.0", forRemoval = true)
    default Query<T> filter(String condition, Object value) {
        return legacyOperation();
    }

    /**
     * @return The query logged during the previous execution of this query
     * @since 2.3
     */
    String getLoggedQuery();

    /**
     * Deletes an entity from the database and returns it.
     *
     * @return the deleted entity
     */
    @Nullable
    default T findAndDelete() {
        return findAndDelete(new FindAndDeleteOptions());
    }

    /**
     * Execute the query and get the results.
     *
     * @param options the options to apply to the find operation
     * @return a MorphiaCursor
     * @since 1.4
     * @deprecated use {@link #iterator(FindOptions)}
     */
    @Deprecated(since = "2.0", forRemoval = true)
    default MorphiaCursor<T> find(FindOptions options) {
        return iterator(options);
    }

    /**
     * Execute the query and get the results.
     *
     * @return a MorphiaCursor
     * @see #iterator(FindOptions)
     * @since 2.0
     */
    @Override
    default MorphiaCursor<T> iterator() {
        return iterator(new FindOptions());
    }

    /**
     * Deletes an entity from the database and returns it.
     *
     * @param options the options to apply
     * @return the deleted entity
     */
    @Nullable
    T findAndDelete(FindAndDeleteOptions options);

    /**
     * Gets the first entity in the result set.  Obeys the {@link Query} offset value.
     *
     * @return the only instance in the result, or null if the result set is empty.
     * @since 1.5
     */
    @Nullable
    T first();

    /**
     * Gets the first entity in the result set.  Obeys the {@link Query} offset value.
     *
     * @param options the options to apply to the find operation
     * @return the only instance in the result, or null if the result set is empty.
     * @since 1.5
     */
    @Nullable
    T first(FindOptions options);

    /**
     * @return the entity {@link Class}.
     * @morphia.internal
     */
    Class<T> getEntityClass();

    /**
     * Create a modify operation based on this query
     *
     * @param first   the first and required update operator
     * @param updates lists the set of updates to apply
     * @return the modify operation
     */
    Modify<T> modify(UpdateOperator first, UpdateOperator... updates);

    /**
     * Execute the query and get the results.
     *
     * @param options the options to apply to the find operation
     * @return a MorphiaCursor
     * @since 2.0
     */
    MorphiaCursor<T> iterator(FindOptions options);

    /**
     * Execute the query and get the results (as a {@code MorphiaCursor<Key<T>>})
     *
     * @return the keys of the documents returned by this query
     * @deprecated use a project to retrieve only the ID values
     */
    @Deprecated(since = "2.0", forRemoval = true)
    MorphiaKeyCursor<T> keys();

    /**
     * Execute the query and get the results (as a {@code MorphiaCursor<Key<T>>})
     *
     * @param options the options to apply to the find operation
     * @return the keys of the documents returned by this query
     * @since 1.4
     */
    @Deprecated(since = "2.0", forRemoval = true)
    MorphiaKeyCursor<T> keys(FindOptions options);

    /**
     * Creates a container to hold 'or' clauses
     *
     * @param criteria the clauses to 'or' together
     * @return the container
     */
    @Deprecated(since = "2.0", forRemoval = true)
    default CriteriaContainer or(Criteria... criteria) {
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
    default Modify<T> modify(UpdateOperations<T> operations) {
        return legacyOperation();
    }

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
     * Perform a text search on the content of the fields indexed with a text index..
     *
     * @param text the text to search for
     * @return the Query to enable chaining of commands
     * @mongodb.driver.manual reference/operator/query/text/ $text
     * @deprecated use {@link dev.morphia.query.experimental.filters.Filters#text(String)} instead
     */
    @Deprecated(since = "2.0", forRemoval = true)
    Query<T> search(String text);

    /**
     * Perform a text search on the content of the fields indexed with a text index..
     *
     * @param text     the text to search for
     * @param language the language to use during the search
     * @return the Query to enable chaining of commands
     * @mongodb.driver.manual reference/operator/query/text/ $text
     * @deprecated use {@link dev.morphia.query.experimental.filters.Filters#text(String)} instead
     */
    @Deprecated(since = "2.0", forRemoval = true)
    Query<T> search(String text, String language);

    /**
     * Provides a {@link Stream} representation of the results of this query.
     *
     * @return the stream
     * @since 2.2
     */
    default Stream<T> stream() {
        return stream(new FindOptions());
    }

    /**
     * Provides a {@link Stream} representation of the results of this query.
     *
     * @param options the options to apply
     * @return the stream
     * @since 2.2
     */
    default Stream<T> stream(FindOptions options) {
        Spliterator<T> spliterator = Spliterators.spliteratorUnknownSize(iterator(options), 0);
        return StreamSupport.stream(spliterator, false);
    }

    /**
     * @return the document form of this query
     * @morphia.internal
     */
    Document toDocument();

    /**
     * Creates an update operation based on this query
     *
     * @param updates lists the set of updates to apply
     * @return the update operation
     */
    default Update<T> update(List<UpdateOperator> updates) {
        if (updates.isEmpty()) {
            throw new IllegalArgumentException(Sofia.atLeastOneUpdateRequired());
        }
        var first = updates.get(0);
        var others = updates.subList(1, updates.size()).toArray(new UpdateOperator[0]);
        return update(first, others);
    }

    /**
     * Creates an update operation based on this query
     *
     * @param first   the first and required update operator
     * @param updates lists the set of updates to apply
     * @return the update operation
     */
    Update<T> update(UpdateOperator first, UpdateOperator... updates);

    /**
     * @param operations the prebuilt operations
     * @return the Updates instance
     * @morphia.internal
     * @since 2.0
     * @deprecated
     */
    @Deprecated(since = "2.0", forRemoval = true)
    default Update<T> update(UpdateOperations<T> operations) {
        return legacyOperation();
    }
}
