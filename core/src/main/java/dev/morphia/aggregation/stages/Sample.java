package dev.morphia.aggregation.stages;

import dev.morphia.annotations.internal.MorphiaInternal;

/**
 * Randomly selects the specified number of documents from its input.
 *
 * @aggregation.expression $sample
 */
public class Sample extends Stage {
    private final long size;

    /**
     * @param size the sample size
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    protected Sample(long size) {
        super("$sample");
        this.size = size;
    }

    /**
     * Creates a new stage with the given sample size.
     *
     * @param size the sample size
     * @return the new stage
     * @since 2.2
     */
    public static Sample sample(long size) {
        return new Sample(size);
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
