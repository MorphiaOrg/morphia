package dev.morphia.test.aggregation.stages;

import java.util.Iterator;

import com.mongodb.client.model.BucketGranularity;

import dev.morphia.test.TemplatedTestBase;
import dev.morphia.test.aggregation.model.Book;
import dev.morphia.test.aggregation.model.BooksBucketResult;
import dev.morphia.test.aggregation.model.BucketAutoResult;
import dev.morphia.test.util.ActionTestOptions;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

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
    @Test
    @DisplayName("Single Facet Aggregation")
    public void testExample1() {
        testPipeline((aggregation) -> aggregation.pipeline(autoBucket().groupBy("$price").buckets(4)));
    }

    /**
     * test data: dev/morphia/test/aggregation/stages/bucketAuto/example2
     * 
     */
    @Test
    @DisplayName("Multi-Faceted Aggregation")
    public void testExample2() {
        testPipeline(new ActionTestOptions().orderMatters(false),
                (aggregation) -> aggregation.pipeline(facet().field("price", autoBucket().groupBy("$price").buckets(4))
                        .field("year",
                                autoBucket().groupBy("$year").buckets(3).outputField("count", sum(1))
                                        .outputField("years", push("$year")))
                        .field("area", autoBucket().groupBy(multiply("$dimensions.height", "$dimensions.width"))
                                .buckets(4).outputField("count", sum(1)).outputField("titles", push("$title")))));
    }

    @Test
    @DisplayName("Multi-Faceted Aggregation")
    public void testExample3() {
        testPipeline(new ActionTestOptions().orderMatters(false),
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

        Iterator<BooksBucketResult> aggregate = getDs().aggregate(Book.class, BooksBucketResult.class)
                .pipeline(autoBucket().groupBy("$copies").buckets(3).granularity(BucketGranularity.POWERSOF2)
                        .outputField("authors", addToSet("$author")).outputField("count", sum(1)))
                .iterator();
        BooksBucketResult result1 = aggregate.next();
        Assertions.assertEquals(4, result1.getId().getMin());
        Assertions.assertEquals(8, result1.getId().getMax());
        Assertions.assertEquals(2, result1.getCount());
        Assertions.assertEquals(singleton("Dante"), result1.getAuthors());

        result1 = aggregate.next();
        Assertions.assertEquals(8, result1.getId().getMin());
        Assertions.assertEquals(32, result1.getId().getMax());
        Assertions.assertEquals(1, result1.getCount());
        Assertions.assertEquals(singleton("Homer"), result1.getAuthors());

        result1 = aggregate.next();
        Assertions.assertEquals(32, result1.getId().getMin());
        Assertions.assertEquals(64, result1.getId().getMax());
        Assertions.assertEquals(1, result1.getCount());
        Assertions.assertEquals(singleton("Dante"), result1.getAuthors());
        Assertions.assertFalse(aggregate.hasNext());

    }

    @Test
    public void testWithoutGranularity() {
        getDs().save(asList(new Book("The Banquet", "Dante", 5), new Book("Divine Comedy", "Dante", 10),
                new Book("Eclogues", "Dante", 40), new Book("The Odyssey", "Homer", 21)));

        Iterator<BucketAutoResult> aggregate = getDs().aggregate(Book.class, BucketAutoResult.class)
                .pipeline(autoBucket().groupBy("$copies").buckets(2)).iterator();
        BucketAutoResult result1 = aggregate.next();
        Assertions.assertEquals(5, result1.getId().getMin());
        Assertions.assertEquals(21, result1.getId().getMax());
        Assertions.assertEquals(2, result1.getCount());
        result1 = aggregate.next();
        Assertions.assertEquals(21, result1.getId().getMin());
        Assertions.assertEquals(40, result1.getId().getMax());
        Assertions.assertEquals(2, result1.getCount());
        Assertions.assertFalse(aggregate.hasNext());

    }
}
