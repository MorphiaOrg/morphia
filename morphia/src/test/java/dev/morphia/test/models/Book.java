package dev.morphia.test.models;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Reference;
import org.bson.types.ObjectId;

import java.util.List;

import static java.lang.String.format;
import static java.util.Arrays.asList;

@Entity(value = "books", useDiscriminator = false)
public final class Book {
    @Id
    public ObjectId id;
    public String title;
    @Reference
    public Author author;
    public Integer copies;
    public List<String> tags;

    public Book() {
    }

    public Book(final String title, final Author author, final Integer copies, final String... tags) {
        this.title = title;
        this.author = author;
        this.copies = copies;
        this.tags = asList(tags);
    }

    @Override
    public String toString() {
        return format("Book{title='%s', author='%s', copies=%d, tags=%s}", title, author, copies, tags);
    }
}
