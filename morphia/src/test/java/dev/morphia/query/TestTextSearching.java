package dev.morphia.query;

import dev.morphia.TestBase;
import dev.morphia.annotations.Field;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Index;
import dev.morphia.annotations.Indexes;
import dev.morphia.utils.IndexType;
import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static java.util.Arrays.asList;

public class TestTextSearching extends TestBase {
    @Override
    @Before
    public void setUp() {
        assumeMinServerVersion(2.6);
        super.setUp();
    }

    @Test
    public void testTextSearch() {
        getMorphia().map(Greeting.class);
        getDs().ensureIndexes();

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

        List<Greeting> good = toList(getDs().find(Greeting.class)
                                            .search("good")
                                            .order("_id")
                                            .find());
        Assert.assertEquals(4, good.size());
        Assert.assertEquals("good morning", good.get(0).value);
        Assert.assertEquals("good afternoon", good.get(1).value);
        Assert.assertEquals("good night", good.get(2).value);
        Assert.assertEquals("good riddance", good.get(3).value);

        good = toList(getDs().find(Greeting.class)
                             .search("good", "english")
                             .order("_id")
                             .find());
        Assert.assertEquals(4, good.size());
        Assert.assertEquals("good morning", good.get(0).value);
        Assert.assertEquals("good afternoon", good.get(1).value);
        Assert.assertEquals("good night", good.get(2).value);
        Assert.assertEquals("good riddance", good.get(3).value);

        Assert.assertEquals(1, toList(getDs().find(Greeting.class)
                                      .search("riddance")
                                      .find()).size());
        Assert.assertEquals(1, toList(getDs().find(Greeting.class)
                                      .search("noches", "spanish")
                                      .find()).size());
        Assert.assertEquals(1, toList(getDs().find(Greeting.class)
                                      .search("Tag")
                                      .find()).size());
    }

    @Test
    public void testTextSearchSorting() {
        getMorphia().map(Book.class);
        getDs().ensureIndexes();

        getDs().save(asList(new Book("The Banquet", "Dante"),
                            new Book("Divine Comedy", "Dante"),
                            new Book("Eclogues", "Dante"),
                            new Book("The Odyssey", "Homer"),
                            new Book("Iliad", "Homer")));

        List<Book> books = toList(getDs().find(Book.class)
                                         .search("Dante Comedy").project(Meta.textScore("score"))
                                         .order(Meta.textScore("score"))
                                         .find());
        Assert.assertEquals(3, books.size());
        Assert.assertEquals("Divine Comedy", books.get(0).title);
    }

    @Test
    public void testTextSearchValidationFailed() {
        getMorphia().map(Book.class);
        getDs().ensureIndexes();

        getDs().save(asList(new Book("The Banquet", "Dante"),
                            new Book("Divine Comedy", "Dante"),
                            new Book("Eclogues", "Dante"),
                            new Book("The Odyssey", "Homer"),
                            new Book("Iliad", "Homer")));

        List<Book> books = toList(getDs().find(Book.class)
                                         .search("Dante").project(Meta.textScore())
                                         .order(Meta.textScore())
                                         .find());
        Assert.assertEquals(3, books.size());
        Assert.assertEquals("Dante", books.get(0).author);
    }

    @Test
    public void testTextSearchWithMeta() {
        getMorphia().map(Book.class);
        getDs().ensureIndexes();

        getDs().save(asList(new Book("The Banquet", "Dante"),
                            new Book("Divine Comedy", "Dante"),
                            new Book("Eclogues", "Dante"),
                            new Book("The Odyssey", "Homer"),
                            new Book("Iliad", "Homer")));

        List<Book> books = toList(getDs().find(Book.class)
                                         .search("Dante").project(Meta.textScore("score"))
                                         .order(Meta.textScore("score"))
                                         .find());
        Assert.assertEquals(3, books.size());
        for (Book book : books) {
            Assert.assertEquals("Dante", book.author);
        }
    }

    @Indexes(@Index(fields = @Field(value = "$**", type = IndexType.TEXT)))
    private static class Greeting {
        @Id
        private ObjectId id;
        private String value;
        private String language;

        Greeting() {
        }

        private Greeting(final String value, final String language) {
            this.language = language;
            this.value = value;
        }
    }

    @Indexes(@Index(fields = @Field(value = "$**", type = IndexType.TEXT)))
    private static class Book {
        @Id
        private ObjectId id;
        private String title;
        private String author;

        Book() {
        }

        private Book(final String title, final String author) {
            this.author = author;
            this.title = title;
        }
    }
}
