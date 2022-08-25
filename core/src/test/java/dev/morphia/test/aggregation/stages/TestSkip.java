package dev.morphia.test.aggregation.stages;

import dev.morphia.test.aggregation.AggregationTest;
import dev.morphia.test.aggregation.model.Book;
import org.testng.Assert;
import org.testng.annotations.Test;

import static java.util.Arrays.asList;

public class TestSkip extends AggregationTest {
    @Test
    public void testSkip() {
        getDs().save(asList(new Book("The Banquet", "Dante", 2),
            new Book("Divine Comedy", "Dante", 1),
            new Book("Eclogues", "Dante", 2),
            new Book("The Odyssey", "Homer", 10),
            new Book("Iliad", "Homer", 10)));

        Book book = getDs().aggregate(Book.class)
                           .skip(2)
                           .execute(Book.class)
                           .next();
        Assert.assertEquals(book.getTitle(), "Eclogues");
        Assert.assertEquals(book.getAuthor(), "Dante");
        Assert.assertEquals(book.getCopies().intValue(), 2);
    }

}
