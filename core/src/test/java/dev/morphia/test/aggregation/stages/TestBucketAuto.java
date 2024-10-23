package dev.morphia.test.aggregation.stages;

import java.util.Iterator;

import com.mongodb.client.model.BucketGranularity;

import dev.morphia.test.TemplatedTestBase;
import dev.morphia.test.aggregation.model.Book;
import dev.morphia.test.aggregation.model.BooksBucketResult;
import dev.morphia.test.aggregation.model.BucketAutoResult;

import org.testng.Assert;
import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.AccumulatorExpressions.addToSet;
import static dev.morphia.aggregation.expressions.AccumulatorExpressions.push;
import static dev.morphia.aggregation.expressions.AccumulatorExpressions.sum;
import static dev.morphia.aggregation.expressions.MathExpressions.multiply;
import static dev.morphia.aggregation.stages.AutoBucket.autoBucket;
import static dev.morphia.aggregation.stages.Facet.facet;
import static java.util.Arrays.asList;
import static java.util.Collections.singleton;

public class TestBucketAuto extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/stages/bucketAuto/example1
     * 
     */
    @Test(testName = "Single Facet Aggregation")
    public void testExample1() {
        testPipeline((aggregation) -> aggregation.pipeline(autoBucket().groupBy("$price").buckets(4)));
    }

    /**
     * test data: dev/morphia/test/aggregation/stages/bucketAuto/example2
     * 
     */
    @Test(testName = "Multi-Faceted Aggregation")
    public void testExample2() {
        testPipeline(
                (aggregation) -> aggregation.pipeline(facet().field("price", autoBucket().groupBy("$price").buckets(4))
                        .field("year",
                                autoBucket().groupBy("$year").buckets(3).outputField("count", sum(1))
                                        .outputField("years", push("$year")))
                        .field("area", autoBucket().groupBy(multiply("$dimensions.height", "$dimensions.width"))
                                .buckets(4).outputField("count", sum(1)).outputField("titles", push("$title")))));
    }

    @Test(testName = "Multi-Faceted Aggregation")
    public void testExample3() {
        testPipeline(
                (aggregation) -> aggregation.pipeline(facet().field("price", autoBucket().groupBy("$price").buckets(4))
                        .field("year",
                                autoBucket().groupBy("$year").buckets(3).outputField("count", sum(1))
                                        .outputField("years", push("$year")))
                        .field("area", autoBucket().groupBy(multiply("$dimensions.height", "$dimensions.width"))
                                .buckets(4).outputField("count", sum(1)).outputField("titles", push("$title")))));
    }

    @Test
    public void testWithGranularity() {
        getDs().save(asList(new Book("The Banquet", "Dante", 5), new Book("Divine Comedy", "Dante", 7),
                new Book("Eclogues", "Dante", 40), new Book("The Odyssey", "Homer", 21)));

        Iterator<BooksBucketResult> aggregate = getDs().aggregate(Book.class)
                .autoBucket(autoBucket().groupBy("$copies").buckets(3).granularity(BucketGranularity.POWERSOF2)
                        .outputField("authors", addToSet("$author")).outputField("count", sum(1)))
                .execute(BooksBucketResult.class);
        BooksBucketResult result1 = aggregate.next();
        Assert.assertEquals(result1.getId().getMin(), 4);
        Assert.assertEquals(result1.getId().getMax(), 8);
        Assert.assertEquals(result1.getCount(), 2);
        Assert.assertEquals(result1.getAuthors(), singleton("Dante"));

        result1 = aggregate.next();
        Assert.assertEquals(result1.getId().getMin(), 8);
        Assert.assertEquals(result1.getId().getMax(), 32);
        Assert.assertEquals(result1.getCount(), 1);
        Assert.assertEquals(result1.getAuthors(), singleton("Homer"));

        result1 = aggregate.next();
        Assert.assertEquals(result1.getId().getMin(), 32);
        Assert.assertEquals(result1.getId().getMax(), 64);
        Assert.assertEquals(result1.getCount(), 1);
        Assert.assertEquals(result1.getAuthors(), singleton("Dante"));
        Assert.assertFalse(aggregate.hasNext());

    }

    @Test
    public void testWithoutGranularity() {
        getDs().save(asList(new Book("The Banquet", "Dante", 5), new Book("Divine Comedy", "Dante", 10),
                new Book("Eclogues", "Dante", 40), new Book("The Odyssey", "Homer", 21)));

        Iterator<BucketAutoResult> aggregate = getDs().aggregate(Book.class)
                .autoBucket(autoBucket().groupBy("$copies").buckets(2)).execute(BucketAutoResult.class);
        BucketAutoResult result1 = aggregate.next();
        Assert.assertEquals(result1.getId().getMin(), 5);
        Assert.assertEquals(result1.getId().getMax(), 21);
        Assert.assertEquals(result1.getCount(), 2);
        result1 = aggregate.next();
        Assert.assertEquals(result1.getId().getMin(), 21);
        Assert.assertEquals(result1.getId().getMax(), 40);
        Assert.assertEquals(result1.getCount(), 2);
        Assert.assertFalse(aggregate.hasNext());

    }
}
