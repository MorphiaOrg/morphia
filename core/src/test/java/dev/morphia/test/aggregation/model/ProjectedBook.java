package dev.morphia.test.aggregation.model;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;

import java.util.Objects;

@Entity
public class ProjectedBook {
    @Id
    private Integer id;
    private String title;
    private ProjectedAuthor author;

    ProjectedBook() {
    }

    public ProjectedBook(Integer id, String title, String last, String first) {
        this.id = id;
        this.title = title;
        author = new ProjectedAuthor(last, first);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, title, author);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ProjectedBook)) {
            return false;
        }
        final ProjectedBook that = (ProjectedBook) o;
        return Objects.equals(id, that.id) &&
                title.equals(that.title) &&
                author.equals(that.author);
    }
}
