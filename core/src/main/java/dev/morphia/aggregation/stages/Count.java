package dev.morphia.aggregation.stages;

import dev.morphia.annotations.internal.MorphiaInternal;

/**
 * Passes a document to the next stage that contains a count of the number of documents input to the stage.
 *
 * @aggregation.stage $count
 * @mongodb.server.release 3.4
 */
public class Count extends Stage {
    private final String name;

    /**
     * @param name the field name
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public Count(String name) {
        super("$count");
        this.name = name;
    }

    /**
     * Creates a new Count stage
     *
     * @param name the field name
     * @return the new stage
     * @since 3.0
     */
    public static Count count(String name) {
        return new Count(name);
    }

    /**
     * @return the name
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public String getName() {
        return name;
    }
}
