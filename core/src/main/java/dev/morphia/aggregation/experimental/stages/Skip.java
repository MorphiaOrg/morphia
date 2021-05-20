package dev.morphia.aggregation.experimental.stages;

/**
 * Randomly selects the specified number of documents from its input.
 *
 * @aggregation.expression $skip
 */
public class Skip extends Stage {
    private final long size;

    protected Skip(long size) {
        super("$skip");
        this.size = size;
    }

    /**
     * Creates a new stage with the given skip size
     *
     * @param size the skip size
     * @return the new stage
     * @deprecated use {@link #skip(long)}
     */
    @Deprecated(forRemoval = true)
    public static Skip of(long size) {
        return new Skip(size);
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
     * @morphia.internal
     */
    public long getSize() {
        return size;
    }
}
