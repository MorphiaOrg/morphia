package dev.morphia.aggregation.experimental.stages;

/**
 * Base type for stages.
 */
public abstract class Stage {
    private String stageName;

    protected Stage(final String stageName) {
        this.stageName = stageName;
    }

    /**
     * The name of the stage.
     *
     * @return the name
     * @morphia.internal
     */
    public String getStageName() {
        return stageName;
    }
}
