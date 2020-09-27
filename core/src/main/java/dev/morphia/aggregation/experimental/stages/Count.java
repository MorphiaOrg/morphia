package dev.morphia.aggregation.experimental.stages;

/**
 * @morphia.internal
 */
public class Count extends Stage {
    private final String name;

    /**
     * @param name the field name
     * @morphia.internal
     */
    public Count(String name) {
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
