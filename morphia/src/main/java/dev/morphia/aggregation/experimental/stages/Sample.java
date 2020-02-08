package dev.morphia.aggregation.experimental.stages;

/**
 * Randomly selects the specified number of documents from its input.
 *
 * @aggregation.expression $sample
 */
public class Sample extends Stage {
    private int size;

    protected Sample(final int size) {
        super("$sample");
        this.size = size;
    }

    /**
     * Creates a new stage with the given sample size.
     *
     * @param size the sample size
     * @return the new stage
     */
    public static Sample of(final int size) {
        return new Sample(size);
    }

    /**
     * @return the size
     * @morphia.internal
     */
    public int getSize() {
        return size;
    }
}
