package dev.morphia.aggregation;

import java.util.List;

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
}
