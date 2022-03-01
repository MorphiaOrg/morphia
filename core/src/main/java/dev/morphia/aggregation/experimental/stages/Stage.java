package dev.morphia.aggregation.experimental.stages;

import dev.morphia.aggregation.experimental.Aggregation;

/**
 * Base type for stages.
 */
public abstract class Stage {
    private final String stageName;
    private Aggregation<?> aggregation;

    protected Stage(String stageName) {
        this.stageName = stageName;
    }

    public Stage aggregation(Aggregation<?> aggregation) {
        this.aggregation = aggregation;
        return this;
    }

    /**
     * The name of the stage.
     *
     * @return the name
     * @morphia.internal
     */
    public String stageName() {
        return stageName;
    }

    /**
     * @return the aggregation
     * @morphia.internal
     * @since 2.2.4
     */
    Aggregation<?> aggregation() {
        return aggregation;
    }
}
