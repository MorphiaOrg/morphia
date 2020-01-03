package dev.morphia.aggregation.experimental.stages;

public abstract class Stage {
    private String stageName;

    protected Stage(final String stageName) {
        this.stageName = stageName;
    }

    public String getStageName() {
        return stageName;
    }

}
