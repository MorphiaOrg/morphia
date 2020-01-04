package dev.morphia.aggregation.experimental.stages;

/**
 * @morphia.internal
 */
public class Count extends Stage {
    private String name;

    public Count(final String name) {
        super("$count");
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
