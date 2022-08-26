package dev.morphia.aggregation.stages;

import dev.morphia.aggregation.Aggregation;
import dev.morphia.annotations.internal.MorphiaInternal;

/**
 * Base type for stages.
 */
public abstract class Stage {
    private final String stageName;
    private Aggregation<?> aggregation;

    protected Stage(String stageName) {
        this.stageName = stageName;
    }

    /**
     * @param aggregation the aggregation
     * @morphia.internal
     */
    @MorphiaInternal
    public void aggregation(Aggregation<?> aggregation) {
        this.aggregation = aggregation;
    }

    /**
     * The name of the stage.
     *
     * @return the name
     * @morphia.internal
     */
    @MorphiaInternal
    public String stageName() {
        return stageName;
    }

    /**
     * @return the aggregation
     * @morphia.internal
     * @since 2.2.4
     */
    @MorphiaInternal
    Aggregation<?> aggregation() {
        return aggregation;
    }
}
