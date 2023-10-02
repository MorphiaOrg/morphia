package dev.morphia.aggregation.stages;

import dev.morphia.aggregation.Aggregation;
import dev.morphia.annotations.internal.MorphiaInternal;

/**
 * Base type for stages.
 */
public abstract class Stage {
    private final String stageName;
    private Aggregation<?> aggregation;

    /**
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    protected Stage(String stageName) {
        this.stageName = stageName;
    }

    /**
     * @param aggregation the aggregation
     * @hidden
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
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public String stageName() {
        return stageName;
    }

    /**
     * @return the aggregation
     * @hidden
     * @morphia.internal
     * @since 2.2.4
     */
    @MorphiaInternal
    Aggregation<?> aggregation() {
        return aggregation;
    }
}
