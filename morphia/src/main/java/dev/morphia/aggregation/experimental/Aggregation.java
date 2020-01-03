package dev.morphia.aggregation.experimental;

import dev.morphia.aggregation.experimental.stages.AddFields;
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
    <S extends Stage> S getStage(String name);

    /**
     * @return the stage in this aggregation
     * @morphia.internal
     */
    List<Stage> getStages();

    /**
     * Adds new fields to documents. $addFields outputs documents that contain all existing fields from the input documents and newly
     * added fields.
     * <p>
     * The $addFields stage is equivalent to a $project stage that explicitly specifies all existing fields in the input documents and
     * adds the new fields.
     *
     * @param fields the stage specification
     * @return this
     * @mongodb.driver.manual reference/operator/aggregation/addFields $addFields
     */
    Aggregation<T> addFields(AddFields fields);

    /**
     * Groups input documents by the specified _id expression and for each distinct grouping, outputs a document. The _id field of each
     * output document contains the unique group by value. The output documents can also contain computed fields that hold the values of
     * some accumulator expression.
     *
     * @param group the group definition
     * @return this
     * @mongodb.driver.manual reference/operator/aggregation/group $group
     */
    Aggregation<T> group(Group group);

    /**
     * Limits the number of documents passed to the next stage in the pipeline.
     *
     * @param limit the maximum docs to pass along to the next stage
     * @return this
     * @mongodb.driver.manual reference/operator/aggregation/limit $limit
     */
    Aggregation<T> limit(int limit);

    /**
     * Performs a left outer join to an unsharded collection in the same database to filter in documents from the “joined” collection for
     * processing. To each input document, the $lookup stage adds a new array field whose elements are the matching documents from the
     * “joined” collection. The $lookup stage passes these reshaped documents to the next stage.
     *
     * @param lookup the lookup specification
     * @return this
     * @mongodb.driver.manual reference/operator/aggregation/lookup $lookup
     */
    Aggregation<T> lookup(Lookup lookup);

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
     * Execute the aggregation and write the results to a collection.  The target collection will be created if it's missing or replaced
     * with the results if it already exists.
     *
     * @param <O> the output type used to determine the target collection
     * @mongodb.driver.manual reference/operator/aggregation/out $out
     */
    <O> void out(Class<O> type);

    /**
     * Execute the aggregation and write the results to a collection.  The target collection will be created if it's missing or replaced
     * with the results if it already exists.
     *
     * @param collection the collection to create/overwrite
     * @mongodb.driver.manual reference/operator/aggregation/out $out
     */
    <O> void out(String collection);

    /**
     * Execute the aggregation and write the results to a collection.  The target collection will be created if it's missing or replaced
     * with the results if it already exists.
     *
     * @param collection the collection to create/overwrite
     * @param options    the options to apply
     * @mongodb.driver.manual reference/operator/aggregation/out $out
     */
    void out(String collection, AggregationOptions options);

    /**
     * Execute the aggregation and write the results to a collection.  The target collection will be created if it's missing or replaced
     * with the results if it already exists.
     *
     * @param <O>     the output type used to determine the target collection
     * @param options the options to apply
     * @mongodb.driver.manual reference/operator/aggregation/out $out
     */
    <O> void out(Class<O> type, AggregationOptions options);

    /**
     * Passes along the documents with the requested fields to the next stage in the pipeline. The specified fields can be existing fields
     * from the input documents or newly computed fields.
     *
     * @param projection
     * @return
     * @mongodb.driver.manual reference/operator/aggregation/project $project
     */
    Aggregation<T> project(Projection projection);

    /**
     * Randomly selects the specified number of documents from the previous pipeline stage.
     *
     * @param sample the sample definition
     * @return this
     * @mongodb.driver.manual reference/operator/aggregation/sample $sample
     */
    Aggregation<T> sample(Sample sample);

    /**
     * Sorts all input documents and returns them to the pipeline in sorted order.
     *
     * @param sort the sort definition
     * @return this
     * @mongodb.driver.manual reference/operator/aggregation/sort $sort
     */
    Aggregation<T> sort(Sort sort);
}
