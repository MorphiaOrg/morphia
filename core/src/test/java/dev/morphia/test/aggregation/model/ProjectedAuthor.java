package dev.morphia.test.aggregation.model;

import java.util.Objects;

import dev.morphia.annotations.Entity;

@Entity
public class ProjectedAuthor {
    private String last;
    private String first;

    public ProjectedAuthor() {
    }

    public ProjectedAuthor(String last, String first) {
        this.last = last;
        this.first = first;
    }

    @Override
    public int hashCode() {
        return Objects.hash(last, first);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ProjectedAuthor)) {
            return false;
        }
        final ProjectedAuthor that = (ProjectedAuthor) o;
        return last.equals(that.last) &&
                first.equals(that.first);
    }
}
