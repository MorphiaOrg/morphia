package dev.morphia.aggregation.experimental.stages;

/**
 * @morphia.internal
 */
public class Count extends Stage {
    private String name;

    /**
     * @param name the field name
     * @morphia.internal
     */
    public Count(final String name) {
        super("$count");
        this.name = name;
    }

    /**
     * @return the name
     * @morphia.internal
     */
    public String getName() {
        return name;
    }
}
