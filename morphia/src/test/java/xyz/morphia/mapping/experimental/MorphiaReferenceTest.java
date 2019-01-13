package xyz.morphia.mapping.experimental;

import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Test;
import xyz.morphia.TestBase;
import xyz.morphia.annotations.Id;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MorphiaReferenceTest extends TestBase {
    @Test
    public void basicReference() {
        final Author author = new Author("Jane Austen");
        getDs().save(author);

        final Book book = new Book("Pride and Prejudice");
        book.setAuthor(author);
        getDs().save(book);

        final Book loaded = getDs().find(Book.class).filter("_id", book.id).first();
        Assert.assertFalse(loaded.author.isResolved());
        Assert.assertEquals(author, loaded.author.get());
        Assert.assertTrue(loaded.author.isResolved());
    }

    @Test
    public void listReference() {
        final Author author = new Author("Jane Austen");
        getDs().save(author);

        List<Book> list = new ArrayList<Book>();
        list.add(new Book("Sense and Sensibility "));
        list.add(new Book("Pride and Prejudice"));
        list.add(new Book("Mansfield Park"));
        list.add(new Book("Emma"));
        list.add(new Book("Northanger Abbey"));
        for (final Book book : list) {
            book.setAuthor(author);
            getDs().save(book);
        }
        author.setBooks(list);
        getDs().save(author);

        final Author loaded = getDs().find(Author.class).filter("_id", author.getId()).first();
        Assert.assertFalse(loaded.books.isResolved());
        Assert.assertEquals(list, loaded.getBooks());
        Assert.assertTrue(loaded.books.isResolved());
    }

    @Test
    public void mapReference() {
        final Author author = new Author("Jane Austen");
        getDs().save(author);

        Map<String, Book> books = new LinkedHashMap<String, Book>();
        for (final Book book : new Book[] {
            new Book("Sense and Sensibility "),
                new Book("Pride and Prejudice"),
                new Book("Mansfield Park"),
                new Book("Emma"),
                new Book("Northanger Abbey")}) {
            book.setAuthor(author);
            getDs().save(book);
            books.put(book.name, book);
        }
        author.setMap(books);
        getDs().save(author);

        final Author loaded = getDs().find(Author.class).filter("_id", author.getId()).first();
        Assert.assertFalse(loaded.map.isResolved());
        Assert.assertEquals(books, loaded.getMap());
        Assert.assertTrue(loaded.map.isResolved());
    }

    private static class Author {
        @Id
        private ObjectId id;

        private String name;

        private MorphiaReference<List<Book>> books;
        private MorphiaReference<Map<String, Book>> map;

        public Author() {
        }

        public Author(final String name) {
            this.name = name;
        }

        public ObjectId getId() {
            return id;
        }

        public void setId(final ObjectId id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }

        public List<Book> getBooks() {
            return books.get();
        }

        public void setBooks(final List<Book> books) {
            this.books = MorphiaReference.wrap(books);
        }

        public Map<String, Book> getMap() {
            return map.get();
        }

        public void setMap(final Map<String, Book> map) {
            this.map = MorphiaReference.wrap(map);
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            final Author author = (Author) o;

            if (id != null ? !id.equals(author.id) : author.id != null) {
                return false;
            }
            return name != null ? name.equals(author.name) : author.name == null;
        }

        @Override
        public int hashCode() {
            int result = id != null ? id.hashCode() : 0;
            result = 31 * result + (name != null ? name.hashCode() : 0);
            return result;
        }
    }

    private static class Book {
        @Id
        private ObjectId id;
        private String name;
        private MorphiaReference<Author> author;

        public Book() {
        }

        public Book(final String name) {
            this.name = name;
        }

        public ObjectId getId() {
            return id;
        }

        public void setId(final ObjectId id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }

        public Author getAuthor() {
            return author.get();
        }

        public void setAuthor(final Author author) {
            this.author = MorphiaReference.wrap(author);
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            final Book book = (Book) o;

            if (id != null ? !id.equals(book.id) : book.id != null) {
                return false;
            }
            return name != null ? name.equals(book.name) : book.name == null;
        }

        @Override
        public int hashCode() {
            int result = id != null ? id.hashCode() : 0;
            result = 31 * result + (name != null ? name.hashCode() : 0);
            return result;
        }

        @Override
        public String toString() {
            return "Book{" +
                   "name='" + name + '\'' +
                   '}';
        }
    }
}
