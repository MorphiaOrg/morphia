package dev.morphia.test.aggregation.stages;

import dev.morphia.test.aggregation.AggregationTest;
import dev.morphia.test.aggregation.model.Book;
import dev.morphia.test.aggregation.model.SortByCountResult;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Iterator;

import static dev.morphia.aggregation.expressions.Expressions.field;
import static java.util.Arrays.asList;

public class TestSortByCount extends AggregationTest {
    @Test
    public void testSortByCount() {
        getDs().save(asList(new Book("The Banquet", "Dante", 2),
                new Book("Divine Comedy", "Dante", 1),
                new Book("Eclogues", "Dante", 2),
                new Book("The Odyssey", "Homer", 10),
                new Book("Iliad", "Homer", 10)));

        Iterator<SortByCountResult> aggregate = getDs().aggregate(Book.class)
                .sortByCount(field("author"))
                .execute(SortByCountResult.class);
        SortByCountResult result1 = aggregate.next();
        Assert.assertEquals(result1.getId(), "Dante");
        Assert.assertEquals(result1.getCount(), 3);

        SortByCountResult result2 = aggregate.next();
        Assert.assertEquals(result2.getId(), "Homer");
        Assert.assertEquals(result2.getCount(), 2);

    }

}
