package dev.morphia.aggregation.experimental;

import dev.morphia.aggregation.experimental.stages.Group;
import dev.morphia.aggregation.experimental.stages.Projection;
import dev.morphia.aggregation.experimental.stages.Sample;
import dev.morphia.aggregation.experimental.stages.Sort;
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

    /**
     * @return the named stage or stages in this aggregation
     * @morphia.internal
     */
    <S> S getStage(String name);

    /**
     * @return the stage in this aggregation
     * @morphia.internal
     */
    List<Stage> getStages();

    Aggregation<T> group(Group group);

    Aggregation<T> limit(int limit);

    Aggregation<T> lookup(Lookup lookup);

    /**
     * Execute the aggregation and get the results.
     *
     * @param <O> the output type used to determine the target collection
     */
    <O> void out(Class<O> type);

    /**
     * Execute the aggregation and get the results.
     *
     * @param <O> the output type used to determine the target collection
     * @param options the options to apply
     */
    <O> void out(Class<O> type, AggregationOptions options);

    Aggregation<T> sort(Sort sort);

    /**
     * Filters the document stream to allow only matching documents to pass unmodified into the next pipeline stage. $match uses standard
     * MongoDB queries. For each input document, outputs either one document (a match) or zero documents (no match).
     *
     * @param query the query to use when matching
     * @return this
     * @mongodb.driver.manual reference/operator/aggregation/match $match
     */
    Aggregation<T> match(Query<?> query);

    Aggregation<T> project(Projection projection);

    /**
     * Randomly selects the specified number of documents from the previous pipeline stage.
     *
     * @param sample the sample definition
     * @return this
     * @mongodb.driver.manual reference/operator/aggregation/match $sample
     */
    Aggregation<T> sample(Sample sample);
}
