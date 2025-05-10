package dev.morphia.aggregation;

import java.util.List;

import dev.morphia.aggregation.stages.ChangeStream;
import dev.morphia.aggregation.stages.Merge;
import dev.morphia.aggregation.stages.Out;
import dev.morphia.aggregation.stages.Stage;
import dev.morphia.query.MorphiaCursor;

/**
 * @param <T> The initial type of the aggregation. Used for collection name resolution.
 * @since 2.0
 */
public interface Aggregation<T> {
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

    /**
     * Execute the aggregation. This form and {@link #execute(AggregationOptions)} should be used for pipelines with $out and $merge
     * that do not expect any results to be returned.
     *
     * @see #execute(AggregationOptions)
     */
    void execute();

    /**
     * Execute the aggregation and get the results.
     *
     * @param resultType the type of the result
     * @param <S>        the output type
     * @return a MorphiaCursor
     */
    <S> MorphiaCursor<S> execute(Class<S> resultType);

    /**
     * Execute the aggregation. This form and {@link #execute()} should be used for pipelines with $out and $merge
     * that do no expect any results to be returned.
     *
     * @see #execute()
     */
    void execute(AggregationOptions options);

    /**
     * Execute the aggregation and get the results.
     *
     * @param resultType the type of the result
     * @param options    the options to apply
     * @param <S>        the output type
     * @return a MorphiaCursor
     */
    <S> MorphiaCursor<S> execute(Class<S> resultType, AggregationOptions options);

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

    /**
     * Returns a Change Stream cursor on a collection, a database, or an entire cluster. Must be used as the first stage in an
     * aggregation pipeline.
     *
     * @param stream the options to apply to the stage
     * @return this
     * @since 2.3
     * @deprecated use {@link #pipeline(Stage...)} instead
     * @see ChangeStream#changeStream()
     */
    Aggregation changeStream(ChangeStream stream);
}
