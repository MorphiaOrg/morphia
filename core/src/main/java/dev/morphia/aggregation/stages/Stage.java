package dev.morphia.aggregation.stages;

import dev.morphia.annotations.internal.MorphiaInternal;

/**
 * Base type for stages.
 */
public abstract class Stage {
    private final String stageName;

    /**
     * @param stageName the stage name
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    protected Stage(String stageName) {
        this.stageName = stageName;
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
}
