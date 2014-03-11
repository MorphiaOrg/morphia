package org.mongodb.morphia.aggregation;


import com.mongodb.AggregationOptions;
import com.mongodb.ReadPreference;
import org.mongodb.morphia.query.MorphiaIterator;
import org.mongodb.morphia.query.Query;

import java.util.List;

public interface AggregationPipeline<T, U> {
    AggregationPipeline<T, U> project(Projection... projections);

    AggregationPipeline<T, U> group(String id, Group... groupings);

    AggregationPipeline<T, U> group(List<Group> id, Group... groupings);

    AggregationPipeline<T, U> match(Query query);

    AggregationPipeline<T, U> sort(Sort... sorts);

    AggregationPipeline<T, U> limit(int count);

    AggregationPipeline<T, U> skip(int count);

    AggregationPipeline<T, U> unwind(String field);

    AggregationPipeline<T, U> geoNear(GeoNear geoNear);

    /**
     * Places the output of the aggregation in the collection mapped by the target type using the default options as defined in {@link
     * AggregationOptions}.
     *
     * @param target The class to use when iterating over the results
     * @return an iterator of the computed results
     */
    MorphiaIterator<U, U> out(Class<U> target);

    /**
     * Places the output of the aggregation in the collection mapped by the target type.
     *
     * @param target  The class to use when iterating over the results
     * @param options The options to apply to this aggregation
     * @return an iterator of the computed results
     */
    MorphiaIterator<U, U> out(Class<U> target, AggregationOptions options);

    /**
     * Places the output of the aggregation in the collection mapped by the target type using the default options as defined in {@link
     * AggregationOptions}.
     *
     * @param collectionName The collection in which to store the results of the aggregation overriding the mapped value in target
     * @param target         The class to use when iterating over the results
     * @return an iterator of the computed results
     */
    MorphiaIterator<U, U> out(String collectionName, Class<U> target);

    /**
     * Places the output of the aggregation in the collection mapped by the target type.
     *
     * @param collectionName The collection in which to store the results of the aggregation overriding the mapped value in target
     * @param target         The class to use when iterating over the results
     * @param options        The options to apply to this aggregation
     * @return an iterator of the computed results
     */
    MorphiaIterator<U, U> out(String collectionName, Class<U> target, AggregationOptions options);

    /**
     * Executes the pipeline and aggregates the output in to the type mapped by the target type using the default options as defined in
     * {@link AggregationOptions}.
     *
     * @param target The class to use when iterating over the results
     * @return an iterator of the computed results
     */
    MorphiaIterator<U, U> aggregate(Class<U> target);

    /**
     * Executes the pipeline and aggregates the output in to the type mapped by the target type.
     *
     * @param target  The class to use when iterating over the results
     * @param options The options to apply to this aggregation
     * @return an iterator of the computed results
     */
    MorphiaIterator<U, U> aggregate(Class<U> target, AggregationOptions options);

    /**
     * Executes the pipeline and aggregates the output in to the type mapped by the target type.
     *
     * @param target         The class to use when iterating over the results
     * @param options        The options to apply to this aggregation
     * @param readPreference The read preference to apply to this pipeline
     * @return an iterator of the computed results
     */
    MorphiaIterator<U, U> aggregate(Class<U> target, AggregationOptions options, ReadPreference readPreference);

    /**
     * Executes the pipeline and aggregates the output in to the type mapped by the target type.
     *
     * @param collectionName The collection in which to store the results of the aggregation overriding the mapped value in target
     * @param target         The class to use when iterating over the results
     * @param options        The options to apply to this aggregation
     * @param readPreference The read preference to apply to this pipeline
     * @return an iterator of the computed results
     */
    MorphiaIterator<U, U> aggregate(String collectionName, Class<U> target, AggregationOptions options, ReadPreference readPreference);
}
