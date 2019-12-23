package dev.morphia.aggregation.experimental;

import dev.morphia.aggregation.experimental.stages.Expression;
import dev.morphia.aggregation.experimental.stages.Group;
import dev.morphia.aggregation.experimental.stages.Stage;
import dev.morphia.query.Query;
import dev.morphia.query.internal.MorphiaCursor;
import org.bson.Document;

import java.util.List;

/**
 * @since 2.0
 */
public interface Aggregation<T> {
    /**
     * @morphia.internal
     * @return the stage in this aggregation
     */
    List<Stage> getStages();

    /**
     * @morphia.internal
     * @return the named stage or stages in this aggregation
     */
    <S> S getStage(String name);

    Aggregation<T> group(Group group);

    /**
     * Filters the document stream to allow only matching documents to pass unmodified into the next pipeline stage. $match uses standard
     * MongoDB queries. For each input document, outputs either one document (a match) or zero documents (no match).
     *
     * @param query the query to use when matching
     * @return this
     * @mongodb.driver.manual reference/operator/aggregation/match $match
     */
    Aggregation<T> match(Query<?> query);

    /**
     * Execute the aggregation and get the results.
     *
     * @param <S> the output type
     * @return a MorphiaCursor
     */
    <S> MorphiaCursor<S> execute(final Class<S> resultType);

    /**
     * Execute the aggregation and get the results.
     *
     * @param <S>     the output type
     * @param options the options to apply
     * @return a MorphiaCursor
     */
    <S> MorphiaCursor<S> execute(final Class<S> resultType, final AggregationOptions options);

    /**
     * @morphia.internal
     */
    List<Document> getDocuments();
}
