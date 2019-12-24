package dev.morphia.aggregation.experimental;

import dev.morphia.aggregation.experimental.stages.Stage;

public class Limit extends Stage {
    private int limit;

    protected Limit(final int limit) {
        super("$limit");
        this.limit = limit;
    }

    public int getLimit() {
        return limit;
    }

    public static Limit of(int limit) {
        return new Limit(limit);
    }
}
