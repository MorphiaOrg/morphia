package dev.morphia.test.aggregation.stages;

import dev.morphia.test.aggregation.AggregationTest;
import dev.morphia.test.aggregation.model.Book;

import org.bson.Document;
import org.testng.annotations.Test;

import static java.util.Arrays.asList;
import static org.testng.Assert.assertEquals;

public class TestLimit extends AggregationTest {
    @Test
    public void testLimit() {
        getDs().save(asList(new Book("The Banquet", "Dante", 2),
                new Book("Divine Comedy", "Dante", 1),
                new Book("Eclogues", "Dante", 2),
                new Book("The Odyssey", "Homer", 10),
                new Book("Iliad", "Homer", 10)));

        assertEquals(getDs().aggregate(Book.class)
                .limit(2)
                .execute(Document.class)
                .toList().size(), 2);
    }

}
