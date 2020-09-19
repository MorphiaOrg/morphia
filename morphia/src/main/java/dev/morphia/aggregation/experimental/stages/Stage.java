package dev.morphia.aggregation.experimental.stages;

/**
 * Base type for stages.
 */
public abstract class Stage {
    private final String stageName;

    protected Stage(String stageName) {
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
