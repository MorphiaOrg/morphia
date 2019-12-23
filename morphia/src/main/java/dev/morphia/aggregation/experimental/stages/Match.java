package dev.morphia.aggregation.experimental.stages;

import dev.morphia.query.Query;

public class Match extends Stage {
    private Query query;

    protected Match(final Query query) {
        super("$match");
        this.query = query;
    }

    public static Match of(final Query<?> query) {
        return new Match(query);
    }
}
