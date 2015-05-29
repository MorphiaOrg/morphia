package org.mongodb.morphia.aggregation;


import com.mongodb.AggregationOptions;
import com.mongodb.ReadPreference;
import org.mongodb.morphia.query.Query;

import java.util.Iterator;
import java.util.List;

/**
 * This defines the pipeline used in aggregation operations
 *
 * @see <a href="http://docs.mongodb.org/manual/core/aggregation-pipeline/">Aggregation Pipeline</a>
 */
public interface AggregationPipeline {
    /**
     * Reshapes each document in the stream, such as by adding new fields or removing existing fields. For each input document, outputs one
     * document.
     *
     * @see <a href="http://docs.mongodb.org/manual/reference/operator/aggregation/project/#pipe._S_project">$project</a>
     */
    AggregationPipeline project(Projection... projections);

    /**
     * Groups input documents by a specified identifier expression and applies the accumulator expression(s), if specified, to each group .
     * Consumes all input documents and outputs one document per each distinct group. The output documents only contain the identifier field
     * and, if specified, accumulated fields.
     *
     * @see <a href="http://docs.mongodb.org/manual/reference/operator/aggregation/group/#pipe._S_group">$group</a>
     */
    AggregationPipeline group(String id, Group... groupings);

    /**
     * @see #group(String, Group...)
     */
    AggregationPipeline group(List<Group> id, Group... groupings);

    /**
     * Filters the document stream to allow only matching documents to pass unmodified into the next pipeline stage. $match uses standard
     * MongoDB queries. For each input document, outputs either one document (a match) or zero documents (no match).
     *
     * @see <a href="http://docs.mongodb.org/manual/reference/operator/aggregation/match/#pipe._S_match">$match</a>
     */
    AggregationPipeline match(Query query);

    /**
     * Reorders the document stream by a specified sort key. Only the order changes; the documents remain unmodified. For each input
     * document, outputs one document.
     * 
     * @see <a href="http://docs.mongodb.org/manual/reference/operator/aggregation/sort/#pipe._S_sort">$sort</a>
     */
    AggregationPipeline sort(Sort... sorts);

    /**
     * Passes the first n documents unmodified to the pipeline where n is the specified limit. For each input document, outputs either 
     * one document (for the first n documents) or zero documents (after the first n documents).
     * 
     * @see <a href="http://docs.mongodb.org/manual/reference/operator/aggregation/limit/#pipe._S_limit">$limit</a>
     */
    AggregationPipeline limit(int count);

    /**
     * Skips the first n documents where n is the specified skip number and passes the remaining documents unmodified to the pipeline. 
     * For each input document, outputs either zero documents (for the first n documents) or one document (if after the first n documents).
     * 
     * @see <a href="http://docs.mongodb.org/manual/reference/operator/aggregation/skip/#pipe._S_skip">$skip</a>
     */
    AggregationPipeline skip(int count);

    /**
     * Deconstructs an array field from the input documents to output a document for each element. Each output document replaces the 
     * array with an element value. For each input document, outputs n documents where n is the number of array elements and can be zero 
     * for an empty array.
     * 
     * @see <a href="http://docs.mongodb.org/manual/reference/operator/aggregation/unwind/#pipe._S_unwind">$unwind</a>
     */
    AggregationPipeline unwind(String field);

    /**
     * Returns an ordered stream of documents based on the proximity to a geospatial point. Incorporates the functionality of $match, 
     * $sort, and $limit for geospatial data. The output documents include an additional distance field and can include a location 
     * identifier field.
     * 
     * @see <a href="http://docs.mongodb.org/manual/reference/operator/aggregation/geoNear/#pipe._S_geoNear">$geoNear</a>
     */
    AggregationPipeline geoNear(GeoNear geoNear);

    /**
     * Places the output of the aggregation in the collection mapped by the target type using the default options as defined in {@link
     * AggregationOptions}.
     *
     * @param target The class to use when iterating over the results
     * @return an iterator of the computed results
     */
    <U> Iterator<U> out(Class<U> target);

    /**
     * Places the output of the aggregation in the collection mapped by the target type.
     *
     * @param target  The class to use when iterating over the results
     * @param options The options to apply to this aggregation
     * @return an iterator of the computed results
     */
    <U> Iterator<U> out(Class<U> target, AggregationOptions options);

    /**
     * Places the output of the aggregation in the collection mapped by the target type using the default options as defined in {@link
     * AggregationOptions}.
     *
     * @param collectionName The collection in which to store the results of the aggregation overriding the mapped value in target
     * @param target         The class to use when iterating over the results
     * @return an iterator of the computed results
     */
    <U> Iterator<U> out(String collectionName, Class<U> target);

    /**
     * Places the output of the aggregation in the collection mapped by the target type.
     *
     * @param collectionName The collection in which to store the results of the aggregation overriding the mapped value in target
     * @param target         The class to use when iterating over the results
     * @param options        The options to apply to this aggregation
     * @return an iterator of the computed results
     */
    <U> Iterator<U> out(String collectionName, Class<U> target, AggregationOptions options);

    /**
     * Executes the pipeline and aggregates the output in to the type mapped by the target type using the default options as defined in
     * {@link AggregationOptions}.
     *
     * @param target The class to use when iterating over the results
     * @return an iterator of the computed results
     */
    <U> Iterator<U> aggregate(Class<U> target);

    /**
     * Executes the pipeline and aggregates the output in to the type mapped by the target type.
     *
     * @param target  The class to use when iterating over the results
     * @param options The options to apply to this aggregation
     * @return an iterator of the computed results
     */
    <U> Iterator<U> aggregate(Class<U> target, AggregationOptions options);

    /**
     * Executes the pipeline and aggregates the output in to the type mapped by the target type.
     *
     * @param target         The class to use when iterating over the results
     * @param options        The options to apply to this aggregation
     * @param readPreference The read preference to apply to this pipeline
     * @return an iterator of the computed results
     */
    <U> Iterator<U> aggregate(Class<U> target, AggregationOptions options, ReadPreference readPreference);

    /**
     * Executes the pipeline and aggregates the output in to the type mapped by the target type.
     *
     * @param collectionName The collection in which to store the results of the aggregation overriding the mapped value in target
     * @param target         The class to use when iterating over the results
     * @param options        The options to apply to this aggregation
     * @param readPreference The read preference to apply to this pipeline
     * @return an iterator of the computed results
     */
    <U> Iterator<U> aggregate(String collectionName, Class<U> target, AggregationOptions options, ReadPreference readPreference);
}
