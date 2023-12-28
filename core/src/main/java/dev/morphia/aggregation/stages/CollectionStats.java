package dev.morphia.aggregation.stages;

import dev.morphia.annotations.internal.MorphiaInternal;

/**
 * Returns statistics regarding a collection or view.
 */
public class CollectionStats extends Stage {
    private boolean histogram;
    private Integer scale;
    private boolean count;

    /**
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    protected CollectionStats() {
        super("$collStats");
    }

    /**
     * Creates a new collStats stage
     *
     * @return the new stage
     * @since 2.2
     * @aggregation.stage $collStats
     */
    public static CollectionStats collStats() {
        return new CollectionStats();
    }

    /**
     * Adds the total number of documents in the collection to the return document.
     *
     * @param count true to include the count
     * @return this
     */
    public CollectionStats count(boolean count) {
        this.count = count;
        return this;
    }

    /**
     * @return whether to get the count
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public boolean getCount() {
        return count;
    }

    /**
     * @return whether to add the histogram
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public boolean getHistogram() {
        return histogram;
    }

    /**
     * @return the scale
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public Integer getScale() {
        return scale;
    }

    /**
     * Adds latency histogram information to the embedded documents in latencyStats if true.
     *
     * @param histogram whether to add the histogram
     * @return this
     */
    public CollectionStats histogram(boolean histogram) {
        this.histogram = histogram;
        return this;
    }

    /**
     * Specify the scale factor (i.e. storageStats: { scale: &lt;number&gt; }) to use the specified scale factor for the various size data.
     * For example, to display kilobytes rather than bytes, specify a scale value of 1024.
     * <p>
     * If you specify a non-integer scale factor, MongoDB uses the integer part of the specified factor. For example, if you specify a
     * scale factor of 1023.999, MongoDB uses 1023 as the scale factor.
     * <p>
     * The scale factor does not affect those sizes that specify the unit of measurement in the field name, such as "bytes currently in
     * the cache".
     *
     * @param scale the scale
     * @return this
     */
    public CollectionStats scale(Integer scale) {
        this.scale = scale;
        return this;
    }
}
