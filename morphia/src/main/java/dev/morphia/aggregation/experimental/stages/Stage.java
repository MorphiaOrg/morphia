package dev.morphia.aggregation.experimental.stages;

public abstract class Stage {
    private String name;

    protected Stage(final String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

}
