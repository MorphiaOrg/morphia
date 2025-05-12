package dev.morphia.test.mapping.experimental;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.mapping.experimental.MorphiaReference;
import dev.morphia.test.TestBase;

import org.bson.types.ObjectId;
import org.testng.Assert;
import org.testng.annotations.Test;

import static dev.morphia.aggregation.stages.Lookup.lookup;
import static dev.morphia.aggregation.stages.Unwind.unwind;
import static dev.morphia.mapping.experimental.MorphiaReference.wrap;
import static dev.morphia.query.filters.Filters.eq;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

@SuppressWarnings("removal")
public class MorphiaReferenceTest extends TestBase {
    @Test
    public void basicReference() {
        final Author author = new Author("Jane Austen");
        getDs().save(author);

        final Book book = addBook(author);

        final Book loaded = getDs().find(Book.class).filter(eq("_id", book.id)).first();
        Assert.assertFalse(loaded.author.isResolved());
        assertEquals(author, loaded.author.get());
        assertTrue(loaded.author.isResolved());
    }

    @Test
    public void listReference() {
        final Author author = new Author("Jane Austen");
        getDs().save(author);

        List<Book> list = addListOfBooks(author);
        getDs().save(author);

        final Author loaded = getDs().find(Author.class).filter(eq("_id", author.id)).first();
        validateList(list, loaded);
    }

    @Test
    public void mapReference() {
        final Author author = new Author("Jane Austen");
        getDs().save(author);

        Map<String, Book> books = addBookMap(author);
        getDs().save(author);

        final Author loaded = getDs().find(Author.class).filter(eq("_id", author.id)).first();
        validateMap(books, loaded);
    }

    @Test
    public void setReference() {
        final Author author = new Author("Jane Austen");
        getDs().save(author);

        Set<Book> set = addSetOfBooks(author);
        getDs().save(author);

        final Author loaded = getDs().find(Author.class).filter(eq("_id", author.id)).first();
        Assert.assertFalse(loaded.set.isResolved());
        final Set<Book> set1 = loaded.set.get();

        assertEquals(set1.size(), set.size());

        for (Book book : set) {
            assertTrue(set1.contains(book), "Looking for " + book + " in " + set1);
        }

        assertTrue(loaded.set.isResolved());
    }

    @Test
    public void testAggregationLookups() {
        final Author author = new Author("Jane Austen");
        getDs().save(author);

        Book book = addBook(author);
        Set<Book> set = addSetOfBooks(author);
        List<Book> list = addListOfBooks(author);
        //        Map<String, Book> map = addBookMap(author);

        getDs().save(author);
        Object document = getDs().aggregate(Author.class)
                .pipeline(lookup(Book.class)
                        .as("set")
                        .foreignField("_id")
                        .localField("set"),
                        lookup(Book.class)
                                .as("list")
                                .foreignField("_id")
                                .localField("list"))
                //  TODO how to fetch the values from a nested document for cross-referencing?
                //                                   .lookup(Lookup.from(Book.class)
                //                                                 .as("map")
                //                                                 .foreignField("_id")
                //                                                 .localField("map.$"))
                .iterator()
                .tryNext();

        final Author loaded = (Author) document;
        Book foundBook = getDs().aggregate(Book.class)
                .pipeline(lookup(Author.class)
                        .as("author")
                        .foreignField("_id")
                        .localField("author"),
                        unwind("author"))
                .iterator()
                .next();
        assertTrue(foundBook.author.isResolved());
        assertEquals(author, foundBook.author.get());

        assertTrue(loaded.set.isResolved());
        final Set<Book> set1 = loaded.set.get();
        assertEquals(set.size(), set1.size());
        for (Book book1 : set) {
            assertTrue(set1.contains(book1), "Looking for " + book1 + " in " + set1);
        }

        assertTrue(loaded.list.isResolved());
        assertListEquals(list, loaded.list.get());
        for (Book book1 : list) {
            assertTrue(list.contains(book1), "Looking for " + book1 + " in " + list);
        }
        assertTrue(loaded.list.isResolved());
        //        validateMap(map, loaded);
    }

    protected Book addBook(Author author) {
        final Book book = new Book("Pride and Prejudice");
        book.author = wrap(getDs(), author);
        return getDs().save(book);
    }

    protected Map<String, Book> addBookMap(Author author) {
        Map<String, Book> books = new LinkedHashMap<>();
        for (Book book : new Book[] {
                new Book("Sense and Sensibility"),
                new Book("Pride and Prejudice"),
                new Book("Mansfield Park"),
                new Book("Emma"),
                new Book("Northanger Abbey") }) {
            book.author = wrap(getDs(), author);
            getDs().save(book);
            books.put(book.name, book);
        }
        author.map = wrap(getDs(), books);
        return books;
    }

    protected List<Book> addListOfBooks(Author author) {
        List<Book> list = new ArrayList<>();
        list.add(new Book("Sense and Sensibility"));
        list.add(new Book("Pride and Prejudice"));
        list.add(new Book("Mansfield Park"));
        list.add(new Book("Emma"));
        list.add(new Book("Northanger Abbey"));
        for (Book book : list) {
            book.author = wrap(getDs(), author);
            getDs().save(book);
        }
        author.list = wrap(getDs(), list);
        return list;
    }

    protected Set<Book> addSetOfBooks(Author author) {
        Set<Book> set = new HashSet<>(5);
        set.add(new Book("Sense and Sensibility"));
        set.add(new Book("Pride and Prejudice"));
        set.add(new Book("Mansfield Park"));
        set.add(new Book("Emma"));
        set.add(new Book("Northanger Abbey"));
        for (Book book : set) {
            book.author = wrap(getDs(), author);
            getDs().save(book);
        }
        author.set = wrap(getDs(), set);
        return set;
    }

    protected void validateList(List<Book> list, Author loaded) {
        Assert.assertFalse(loaded.list.isResolved());
        assertEquals(list, loaded.list.get());
        assertTrue(loaded.list.isResolved());
    }

    protected void validateMap(Map<String, Book> books, Author loaded) {
        Assert.assertFalse(loaded.map.isResolved());
        assertEquals(books, loaded.map.get());
        assertTrue(loaded.map.isResolved());
    }

    protected void validateSet(Set<Book> set, Author loaded) {
        assertTrue(loaded.set.isResolved());
        final Set<Book> set1 = loaded.set.get();

        assertEquals(set.size(), set1.size());

        for (Book book : set) {
            assertTrue(set1.contains(book), "Looking for " + book + " in " + set1);
        }

        assertTrue(loaded.set.isResolved());
    }

    @Entity
    private static class Author {
        @Id
        private ObjectId id;

        private String name;

        MorphiaReference<List<Book>> list;
        MorphiaReference<Set<Book>> set;
        MorphiaReference<Map<String, Book>> map;

        public Author() {
        }

        public Author(String name) {
            this.name = name;
        }

        @Override
        public int hashCode() {
            int result = id != null ? id.hashCode() : 0;
            result = 31 * result + (name != null ? name.hashCode() : 0);
            return result;
        }

        @Override
        public boolean equals(Object o) {
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
    }

    @Entity
    private static class Book {
        @Id
        private ObjectId id;
        private String name;
        private MorphiaReference<Author> author;

        public Book() {
        }

        public Book(String name) {
            this.name = name;
        }

        @Override
        public int hashCode() {
            int result = id != null ? id.hashCode() : 0;
            result = 31 * result + (name != null ? name.hashCode() : 0);
            return result;
        }

        @Override
        public boolean equals(Object o) {
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
        public String toString() {
            return "Book{" +
                    "name='" + name + "', " +
                    "hash=" + hashCode() +
                    '}';
        }
    }
}
