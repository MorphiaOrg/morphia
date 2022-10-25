package dev.morphia.test.aggregation.stages;

import java.util.Iterator;
import java.util.List;

import com.mongodb.client.model.BucketGranularity;

import dev.morphia.test.aggregation.AggregationTest;
import dev.morphia.test.aggregation.model.Artwork;
import dev.morphia.test.aggregation.model.Book;
import dev.morphia.test.aggregation.model.BooksBucketResult;
import dev.morphia.test.aggregation.model.BucketAutoResult;

import org.bson.Document;
import org.testng.Assert;
import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.AccumulatorExpressions.addToSet;
import static dev.morphia.aggregation.expressions.AccumulatorExpressions.sum;
import static dev.morphia.aggregation.expressions.Expressions.field;
import static dev.morphia.aggregation.expressions.Expressions.value;
import static dev.morphia.aggregation.stages.AutoBucket.autoBucket;
import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static org.bson.Document.parse;
import static org.testng.Assert.assertEquals;

public class TestAutoBucket extends AggregationTest {
    @Test
    public void testAutoBucket() {
        List<Document> list = List.of(
                parse("{'_id': 1, 'title': 'The Pillars of Society', 'artist': 'Grosz', 'year': 1926, 'price': NumberDecimal('199.99') }"),
                parse("{'_id': 2, 'title': 'Melancholy III', 'artist': 'Munch', 'year': 1902, 'price': NumberDecimal('280.00') }"),
                parse("{'_id': 3, 'title': 'Dancer', 'artist': 'Miro', 'year': 1925, 'price': NumberDecimal('76.04') }"),
                parse("{'_id': 4, 'title': 'The Great Wave off Kanagawa', 'artist': 'Hokusai', 'price': NumberDecimal('167.30') }"),
                parse("{'_id': 5, 'title': 'The Persistence of Memory', 'artist': 'Dali', 'year': 1931, 'price': NumberDecimal('483.00') }"),
                parse("{'_id': 6, 'title': 'Composition VII', 'artist': 'Kandinsky', 'year': 1913, 'price': NumberDecimal('385.00') }"),
                parse("{'_id': 7, 'title': 'The Scream', 'artist': 'Munch', 'year': 1893, 'price' : NumberDecimal('159.00')}"),
                parse("{'_id': 8, 'title': 'Blue Flower', 'artist': 'O\\'Keefe', 'year': 1918, 'price': NumberDecimal('118.42') }"));

        insert("artwork", list);

        List<Document> results = getDs().aggregate(Artwork.class)
                .autoBucket(autoBucket()
                        .groupBy(field("price"))
                        .buckets(4))
                .execute(Document.class)
                .toList();

        List<Document> documents = List.of(
                parse("{'_id': { 'min': NumberDecimal('76.04'), 'max': NumberDecimal('159.00') },'count': 2}"),
                parse("{'_id': { 'min': NumberDecimal('159.00'), 'max': NumberDecimal('199.99') },'count': 2 }"),
                parse("{'_id': { 'min': NumberDecimal('199.99'), 'max': NumberDecimal('385.00') },'count': 2 }"),
                parse("{'_id': { 'min': NumberDecimal('385.00'), 'max': NumberDecimal('483.00') },'count': 2 }"));

        assertEquals(results, documents);
    }

    @Test
    public void testWithGranularity() {
        getDs().save(asList(new Book("The Banquet", "Dante", 5),
                new Book("Divine Comedy", "Dante", 7),
                new Book("Eclogues", "Dante", 40),
                new Book("The Odyssey", "Homer", 21)));

        Iterator<BooksBucketResult> aggregate = getDs().aggregate(Book.class)
                .autoBucket(autoBucket()
                        .groupBy(field("copies"))
                        .buckets(3)
                        .granularity(BucketGranularity.POWERSOF2)
                        .outputField("authors", addToSet(field("author")))
                        .outputField("count", sum(value(1))))
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
        getDs().save(asList(
                new Book("The Banquet", "Dante", 5),
                new Book("Divine Comedy", "Dante", 10),
                new Book("Eclogues", "Dante", 40),
                new Book("The Odyssey", "Homer", 21)));

        Iterator<BucketAutoResult> aggregate = getDs().aggregate(Book.class)
                .autoBucket(autoBucket()
                        .groupBy(field("copies"))
                        .buckets(2))
                .execute(BucketAutoResult.class);
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
