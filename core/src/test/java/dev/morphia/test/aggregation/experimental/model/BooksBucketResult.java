package dev.morphia.test.aggregation.experimental.model;

import java.util.Set;

public class BooksBucketResult extends BucketAutoResult {
    private Set<String> authors;

    public Set<String> getAuthors() {
        return authors;
    }

    public void setAuthors(Set<String> authors) {
        this.authors = authors;
    }
}
