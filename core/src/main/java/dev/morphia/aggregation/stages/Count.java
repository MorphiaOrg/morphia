package dev.morphia.aggregation.stages;

import dev.morphia.annotations.internal.MorphiaInternal;

/**
 * @hidden
 * @morphia.internal
 */
@MorphiaInternal
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
     * @return the name
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public String getName() {
        return name;
    }
}
