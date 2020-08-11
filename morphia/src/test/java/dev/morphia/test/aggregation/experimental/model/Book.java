package dev.morphia.test.aggregation.experimental.model;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import org.bson.types.ObjectId;

import java.util.List;

import static java.lang.String.format;
import static java.util.Arrays.asList;

@Entity(value = "books", useDiscriminator = false)
public final class Book {
    @Id
    private ObjectId id;
    private String title;
    private String author;
    private Integer copies;
    private List<String> tags;

    private Book() {
    }

    public Book(final String title, final String author, final Integer copies, final String... tags) {
        this.title = title;
        this.author = author;
        this.copies = copies;
        this.tags = asList(tags);
    }

    public ObjectId getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getAuthor() {
        return author;
    }

    public Integer getCopies() {
        return copies;
    }

    public List<String> getTags() {
        return tags;
    }

    @Override
    public String toString() {
        return format("Book{title='%s', author='%s', copies=%d, tags=%s}", title, author, copies, tags);
    }
}
