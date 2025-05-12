package dev.morphia.aggregation;

import java.util.List;

import dev.morphia.aggregation.stages.Merge;
import dev.morphia.aggregation.stages.Out;
import dev.morphia.aggregation.stages.Stage;
import dev.morphia.query.MorphiaCursor;

/**
 * @param <T> The initial type of the aggregation. Used for collection name resolution.
 * @since 2.0
 */
public interface Aggregation<T> extends AutoCloseable, Iterable<T> {
    /**
     * Appends the stages to this aggregation's pipeline.
     * 
     * @param stages the stages to add
     * @return this
     * @since 3.0
     */
    Aggregation<T> pipeline(Stage... stages);

    default Aggregation<T> pipeline(List<Stage> stages) {
        return pipeline(stages.toArray(new Stage[0]));
    }

    @Override
    MorphiaCursor<T> iterator();

    default List<T> toList() {
        try (var iterator = iterator()) {
            return iterator.toList();
        }
    }

    /**
     * Writes the results of the aggregation pipeline to a specified collection. The $merge operator must be the last stage in the pipeline.
     *
     * @param merge the merge definition
     * @param <M>   the output collection type
     * @aggregation.stage $merge
     * @mongodb.server.release 4.2
     */
    <M> void merge(Merge<M> merge);

    /**
     * Writes the results of the aggregation pipeline to a specified collection. The $merge operator must be the last stage in the pipeline.
     *
     * @param merge   the merge definition
     * @param options the options to apply
     * @param <M>     the output collection type
     * @aggregation.stage $merge
     * @mongodb.server.release 3.4
     */
    <M> void merge(Merge<M> merge, AggregationOptions options);

    /**
     * Writes the results of the aggregation pipeline to a specified collection. The $out operator must be the last stage in the pipeline.
     *
     * @param out the out definition
     * @param <O> the output collection type
     * @aggregation.stage $out
     */
    <O> void out(Out<O> out);

    /**
     * Writes the results of the aggregation pipeline to a specified collection. The $out operator must be the last stage in the pipeline.
     *
     * @param out     the out definition
     * @param options the options to apply
     * @param <O>     the output collection type
     * @aggregation.stage $out
     */
    <O> void out(Out<O> out, AggregationOptions options);
}
