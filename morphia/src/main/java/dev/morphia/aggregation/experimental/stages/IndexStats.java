package dev.morphia.aggregation.experimental.stages;

public class IndexStats extends Stage {
    protected IndexStats() {
        super("$indexStats");
    }

    public static IndexStats of() {
        return new IndexStats();
    }
}
