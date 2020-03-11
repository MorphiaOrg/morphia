package dev.morphia.aggregation.experimental.stages;

/**
 * Randomly selects the specified number of documents from its input.
 *
 * @aggregation.expression $sample
 */
public class Sample extends Stage {
    private long size;

    protected Sample(final long size) {
        super("$sample");
        this.size = size;
    }

    /**
     * Creates a new stage with the given sample size.
     *
     * @param size the sample size
     * @return the new stage
     */
    public static Sample of(final long size) {
        return new Sample(size);
    }

    /**
     * @return the size
     * @morphia.internal
     */
    public long getSize() {
        return size;
    }
}
