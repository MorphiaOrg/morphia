package dev.morphia.query;

import java.util.Map;
import java.util.stream.Stream;

import com.mongodb.ExplainVerbosity;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import com.mongodb.lang.Nullable;

import dev.morphia.DeleteOptions;
import dev.morphia.ModifyOptions;
import dev.morphia.UpdateOptions;
import dev.morphia.aggregation.stages.Stage;
import dev.morphia.query.filters.Filter;
import dev.morphia.query.updates.UpdateOperator;

/**
 * @param <T> The java type to query against
 */
@SuppressWarnings("removal")
public interface Query<T> extends Iterable<T> {

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
     * Adds filters to this query. This operation is cumulative.
     *
     * @param filters the filters to add
     * @return this
     */
    Query<T> filter(Filter... filters);

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
     * Deletes documents matching this query. Optionally deleting the first or all matched documents.
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
     * Provides information on the query plan. The query plan is the plan the server uses to find the matches for a query. This information
     * may be useful when optimizing a query.
     *
     * @return Map describing the process used to return the query results.
     * @mongodb.driver.manual reference/operator/meta/explain/ explain
     */
    Map<String, Object> explain();

    /**
     * Provides information on the query plan. The query plan is the plan the server uses to find the matches for a query. This information
     * may be useful when optimizing a query.
     *
     * @param verbosity the verbosity of the explanation
     * @return Map describing the process used to return the query results.
     * @mongodb.driver.manual reference/operator/meta/explain/ explain
     * @since 2.2
     */
    Map<String, Object> explain(ExplainVerbosity verbosity);

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
     * Deletes an entity from the database and returns it.
     *
     * @param options the options to apply
     * @return the deleted entity
     */
    @Nullable
    T findAndDelete(FindAndDeleteOptions options);

    /**
     * Gets the first entity in the result set. Obeys the {@link Query} offset value.
     *
     * @return the only instance in the result, or null if the result set is empty.
     * @since 1.5
     */
    @Nullable
    T first();

    /**
     * Execute the query and get the results.
     *
     * @return a MorphiaCursor
     * @since 2.0
     */
    MorphiaCursor<T> iterator();

    /**
     * Create a modify operation based on this query
     *
     * @param first   the first and required update operator
     * @param updates lists the set of updates to apply
     * @return the modify operation
     */
    @Nullable
    default T modify(UpdateOperator first, UpdateOperator... updates) {
        return modify(new ModifyOptions(), first, updates);
    }

    /**
     * Create a modify operation based on this query
     *
     * @param updates lists the set of updates to apply
     * @param first   the first and required update operator
     * @param options the options to apply
     * @return the modify operation
     * @since 2.3
     */
    @Nullable
    T modify(ModifyOptions options, UpdateOperator first, UpdateOperator... updates);

    /**
     * Provides a {@link Stream} representation of the results of this query.
     *
     * @return the stream
     * @since 2.2
     */
    Stream<T> stream();

    /**
     * Creates an update operation based on this query
     *
     * @param updates lists the set of updates to apply
     * @return the update operation
     */
    default UpdateResult update(UpdateOperator... updates) {
        return update(new UpdateOptions(), updates);
    }

    /**
     * Creates an update operation based on this query
     *
     * @param updates lists the set of updates to apply
     * @param options the options to apply
     * @return the update operation
     * @since 2.3
     */
    UpdateResult update(UpdateOptions options, UpdateOperator... updates);

    /**
     * Creates an update operation based on this query
     *
     * @param first  the first and required stage
     * @param stages any remaining stages
     * @return the update operation
     * @since 2.3
     */
    default UpdateResult update(Stage first, Stage... stages) {
        return update(new UpdateOptions(), first, stages);
    }

    /**
     * Creates an update operation based on this query
     *
     * @param first   the first and required stage
     * @param updates lists the set of updates to apply
     * @param options the options to apply
     * @return the update operation
     * @since 2.3
     */
    UpdateResult update(UpdateOptions options, Stage first, Stage... updates);
}
