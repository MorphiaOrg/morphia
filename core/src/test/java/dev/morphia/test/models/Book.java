package dev.morphia.test.models;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.mapping.experimental.MorphiaReference;
import org.bson.types.ObjectId;

import java.util.List;
import java.util.Objects;

import static java.lang.String.format;
import static java.util.Arrays.asList;

@Entity(value = "books", useDiscriminator = false)
public final class Book {
    @Id
    public ObjectId id;
    public String title;
    public MorphiaReference<Author> author;
    public Integer copies;
    public List<String> tags;

    public Book() {
    }

    public Book(String title, Author author) {
        this.title = title;
        this.author = MorphiaReference.wrap(author);
    }

    public Book(String title, Author author, Integer copies, String... tags) {
        this.title = title;
        this.author = MorphiaReference.wrap(author);
        this.copies = copies;
        this.tags = asList(tags);
    }

    public MorphiaReference<Author> getAuthor() {
        return author;
    }

    public Book setAuthor(Author author) {
        this.author = MorphiaReference.wrap(author);
        return this;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, title, author, copies, tags);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Book)) {
            return false;
        }
        Book book = (Book) o;
        return Objects.equals(id, book.id) && Objects.equals(title, book.title) &&
               Objects.equals(author, book.author) && Objects.equals(copies, book.copies) &&
               Objects.equals(tags, book.tags);
    }

    @Override
    public String toString() {
        return format("Book{title='%s', author='%s', copies=%d, tags=%s}", title, author, copies, tags);
    }
}
