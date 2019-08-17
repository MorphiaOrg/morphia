package dev.morphia.issue502;

import dev.morphia.TestBase;
import dev.morphia.annotations.Embedded;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.Test;

import java.util.HashSet;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Can't inherit HashSet : generic is lost...
 */
public class CollectionInheritanceTest extends TestBase {

    private static Book newBook() {
        final Book book = new Book();
        book.authors.add(new Author("Hergé"));
        book.authors.add(new Author("George R. R. Martin"));
        return book;
    }

    @Test
    public void testMappingBook() {
        // Mapping...
        getMapper().map(Book.class, Authors.class, Author.class);

        // Test mapping : author objects must be converted into Document (but wasn't)
        final Document dbBook = getMapper().toDocument(newBook());
        final Object firstBook = ((List<?>) dbBook.get("authors")).iterator().next();
        assertTrue("Author wasn't converted : expected instanceof <Document>, but was <" + firstBook.getClass() + ">",
                   firstBook instanceof Document);

    }

    @Test
    public void testSavingBook() {
        // Test saving
        getDs().save(newBook());

        assertEquals(1, getDs().getCollection(Book.class).count());
    }

    private static class Author {
        private String name;

        Author(final String name) {
            this.name = name;
        }

    }

    @Embedded
    private static class Authors extends HashSet<Author> {
    }

    @Entity
    private static class Book {
        @Id
        private ObjectId id;

        private Authors authors = new Authors();
    }
}
