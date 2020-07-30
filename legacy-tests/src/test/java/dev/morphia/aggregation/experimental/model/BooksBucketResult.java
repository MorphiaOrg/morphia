package dev.morphia.aggregation.experimental.model;

import java.util.Set;

public class BooksBucketResult extends BucketAutoResult {
    private Set<String> authors;

    public Set<String> getAuthors() {
        return authors;
    }

    public void setAuthors(final Set<String> authors) {
        this.authors = authors;
    }
}
