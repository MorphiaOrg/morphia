package dev.morphia.test.query;

import java.util.List;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Field;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Index;
import dev.morphia.annotations.Indexes;
import dev.morphia.mapping.IndexType;
import dev.morphia.query.FindOptions;
import dev.morphia.query.Meta;
import dev.morphia.test.TestBase;
import dev.morphia.test.models.Book;

import org.bson.types.ObjectId;
import org.testng.annotations.Test;

import static dev.morphia.query.Sort.ascending;
import static dev.morphia.query.filters.Filters.text;
import static java.util.Arrays.asList;
import static org.testng.Assert.assertEquals;

public class TestTextSearching extends TestBase {
    @Test
    public void testTextSearch() {
        getMapper().map(Greeting.class);
        getDs().applyIndexes();

        getDs().save(new Greeting("good morning", "english"));
        getDs().save(new Greeting("good afternoon", "english"));
        getDs().save(new Greeting("good night", "english"));
        getDs().save(new Greeting("good riddance", "english"));

        getDs().save(new Greeting("guten Morgen", "german"));
        getDs().save(new Greeting("guten Tag", "german"));
        getDs().save(new Greeting("gute Nacht", "german"));

        getDs().save(new Greeting("buenos días", "spanish"));
        getDs().save(new Greeting("buenas tardes", "spanish"));
        getDs().save(new Greeting("buenos noches", "spanish"));

        List<Greeting> good = getDs().find(Greeting.class, new FindOptions().sort(ascending("_id")))
                .filter(text("good"))
                .iterator()
                .toList();
        assertEquals(good.size(), 4);
        assertEquals(good.get(0).value, "good morning");
        assertEquals(good.get(1).value, "good afternoon");
        assertEquals(good.get(2).value, "good night");
        assertEquals(good.get(3).value, "good riddance");

        good = getDs().find(Greeting.class, new FindOptions().sort(ascending("_id")))
                .filter(text("good")
                        .language("english"))
                .iterator()
                .toList();
        assertEquals(good.size(), 4);
        assertEquals(good.get(0).value, "good morning");
        assertEquals(good.get(1).value, "good afternoon");
        assertEquals(good.get(2).value, "good night");
        assertEquals(good.get(3).value, "good riddance");

        assertEquals(getDs().find(Greeting.class)
                .filter(text("riddance"))
                .iterator().toList().size(), 1);
        assertEquals(getDs().find(Greeting.class)
                .filter(text("noches")
                        .language("spanish"))
                .iterator().toList().size(), 1);
        assertEquals(getDs().find(Greeting.class)
                .filter(text("Tag"))
                .iterator().toList().size(), 1);
    }

    @Test
    public void testTextSearchSorting() {
        getMapper().map(Book.class);
        getDs().applyIndexes();

        getDs().save(asList(new Book("The Banquet", "Dante"),
                new Book("Divine Comedy", "Dante"),
                new Book("Eclogues", "Dante"),
                new Book("The Odyssey", "Homer"),
                new Book("Iliad", "Homer")));

        List<Book> books = getDs().find(Book.class,
                new FindOptions().sort(Meta.textScore("score")))
                .filter(text("Dante Comedy"))
                .iterator()
                .toList();
        assertEquals(books.size(), 3);
        assertEquals(books.get(0).title, "Divine Comedy");
    }

    @Test
    public void testTextSearchValidationFailed() {
        getMapper().map(Book.class);
        getDs().applyIndexes();

        getDs().save(asList(new Book("The Banquet", "Dante"),
                new Book("Divine Comedy", "Dante"),
                new Book("Eclogues", "Dante"),
                new Book("The Odyssey", "Homer"),
                new Book("Iliad", "Homer")));

        List<Book> books = getDs().find(Book.class,
                new FindOptions().sort(Meta.textScore("score")))
                .filter(text("Dante"))
                .iterator()
                .toList();
        assertEquals(books.size(), 3);
        assertEquals(books.get(0).authorString, "Dante");
    }

    @Test
    public void testTextSearchWithMeta() {
        getMapper().map(Book.class);
        getDs().applyIndexes();

        getDs().save(asList(new Book("The Banquet", "Dante"),
                new Book("Divine Comedy", "Dante"),
                new Book("Eclogues", "Dante"),
                new Book("The Odyssey", "Homer"),
                new Book("Iliad", "Homer")));

        List<Book> books = getDs().find(Book.class,
                new FindOptions().sort(Meta.textScore("score")))
                .filter(text("Dante"))
                .iterator()
                .toList();
        assertEquals(books.size(), 3);
        for (Book book : books) {
            assertEquals(book.authorString, "Dante");
        }
    }

    @Entity
    @Indexes(@Index(fields = @Field(value = "$**", type = IndexType.TEXT)))
    private static class Greeting {
        @Id
        private ObjectId id;
        private String value;
        private String language;

        Greeting() {
        }

        private Greeting(String value, String language) {
            this.language = language;
            this.value = value;
        }
    }
}
