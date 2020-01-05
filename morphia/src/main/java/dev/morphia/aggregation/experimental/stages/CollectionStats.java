package dev.morphia.aggregation.experimental.stages;

public class CollectionStats extends Stage {
    private boolean histogram;
    private Integer scale;
    private boolean count;

    protected CollectionStats() {
        super("$collStats");
    }

    public static CollectionStats with() {
        return new CollectionStats();
    }

    public CollectionStats count(final boolean count) {
        this.count = count;
        return this;
    }

    public boolean getCount() {
        return count;
    }

    public boolean getHistogram() {
        return histogram;
    }

    public Integer getScale() {
        return scale;
    }

    public CollectionStats histogram(final boolean histogram) {
        this.histogram = histogram;
        return this;
    }

    public CollectionStats scale(final Integer scale) {
        this.scale = scale;
        return this;
    }
}
