package dev.morphia.aggregation.stages;

import dev.morphia.annotations.internal.MorphiaInternal;

/**
 * Randomly selects the specified number of documents from its input.
 *
 * @aggregation.expression $skip
 */
public class Skip extends Stage {
    private final long size;

    /**
     * @param size the amount to skip
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    protected Skip(long size) {
        super("$skip");
        this.size = size;
    }

    /**
     * Creates a new stage with the given skip size
     *
     * @param size the skip size
     * @return the new stage
     * @since 2.2
     */
    public static Skip skip(long size) {
        return new Skip(size);
    }

    /**
     * @return the size
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public long getSize() {
        return size;
    }
}
