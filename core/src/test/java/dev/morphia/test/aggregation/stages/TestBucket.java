package dev.morphia.test.aggregation.stages;

import java.util.Iterator;
import java.util.List;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.test.ServerVersion;
import dev.morphia.test.TemplatedTestBase;
import dev.morphia.test.aggregation.model.Artwork;
import dev.morphia.test.aggregation.model.Book;
import dev.morphia.test.util.ActionTestOptions;

import org.bson.Document;
import org.testng.Assert;
import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.AccumulatorExpressions.avg;
import static dev.morphia.aggregation.expressions.AccumulatorExpressions.push;
import static dev.morphia.aggregation.expressions.AccumulatorExpressions.sum;
import static dev.morphia.aggregation.expressions.StringExpressions.concat;
import static dev.morphia.aggregation.stages.Bucket.bucket;
import static dev.morphia.aggregation.stages.Facet.facet;
import static dev.morphia.aggregation.stages.Match.match;
import static dev.morphia.query.filters.Filters.gt;
import static java.lang.Integer.valueOf;
import static java.util.Arrays.asList;
import static org.bson.Document.parse;
import static org.testng.Assert.assertEquals;

public class TestBucket extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/stages/bucket/example1
     * 
     */
    @Test(testName = "Bucket by Year and Filter by Bucket Results")
    public void testExample1() {
        testPipeline((aggregation) -> aggregation.pipeline(bucket()
                .groupBy("$year_born").boundaries(1840, 1850, 1860, 1870, 1880).defaultValue("Other")
                .outputField("count", sum(1)).outputField("artists", push()
                        .field("name", concat("$first_name", " ", "$last_name")).field("year_born", "$year_born")),
                match(gt("count", 3))));
    }

    /**
     * test data: dev/morphia/test/aggregation/stages/bucket/example2
     * 
     */
    @Test(testName = "Use $bucket with $facet to Bucket by Multiple Fields")
    public void testExample2() {
        testPipeline(new ActionTestOptions().serverVersion(ServerVersion.ANY),
                (aggregation) -> aggregation.pipeline(facet()
                        .field("price",
                                bucket().groupBy("$price").boundaries(0, 200, 400).defaultValue("Other")
                                        .outputField("count", sum(1))
                                        .outputField("artwork",
                                                push().field("title", "$title").field("price", "$price"))
                                        .outputField("averagePrice", avg("$price")))
                        .field("year",
                                bucket().groupBy("$year").boundaries(1890, 1910, 1920, 1940).defaultValue("Unknown")
                                        .outputField("count", sum(1)).outputField("artwork",
                                                push().field("title", "$title").field("year", "$year")))));
    }

    @Test
    public void testBucket() {
        List<Document> list = List.of(parse(
                "{'_id': 1, 'title': 'The Pillars of Society', 'artist': 'Grosz', 'year': 1926, 'price': NumberDecimal('199.99') }"),
                parse("{'_id': 2, 'title': 'Melancholy III', 'artist': 'Munch', 'year': 1902, 'price': NumberDecimal('280.00') }"),
                parse("{'_id': 3, 'title': 'Dancer', 'artist': 'Miro', 'year': 1925, 'price': NumberDecimal('76.04') }"),
                parse("{'_id': 4, 'title': 'The Great Wave off Kanagawa', 'artist': 'Hokusai', 'price': NumberDecimal('167.30') }"),
                parse("{'_id': 5, 'title': 'The Persistence of Memory', 'artist': 'Dali', 'year': 1931, 'price': NumberDecimal('483.00') }"),
                parse("{'_id': 6, 'title': 'Composition VII', 'artist': 'Kandinsky', 'year': 1913, 'price': NumberDecimal('385.00') }"),
                parse("{'_id': 7, 'title': 'The Scream', 'artist': 'Munch', 'year': 1893}"),
                parse("{'_id': 8, 'title': 'Blue Flower', 'artist': 'O\\'Keefe', 'year': 1918, 'price': NumberDecimal('118.42') }"));

        insert("artwork", list);

        List<Document> results = getDs().aggregate(Artwork.class)
                .bucket(bucket().groupBy("$price").boundaries(0, 200, 400).defaultValue("Other")
                        .outputField("count", sum(1)).outputField("titles", push().single("$title")))
                .execute(Document.class).toList();

        List<Document> documents = List.of(parse(
                "{'_id': 0, 'count': 4, 'titles': ['The Pillars of Society', 'Dancer', 'The Great Wave off Kanagawa', 'Blue Flower']}"),
                parse("{'_id': 200, 'count': 2, 'titles': ['Melancholy III', 'Composition VII']}"),
                parse("{'_id': 'Other', 'count': 2, 'titles': ['The Persistence of Memory', 'The Scream']}"));
        assertEquals(results, documents);
    }

    @Test(expectedExceptions = RuntimeException.class)
    public void testBucketWithBoundariesWithSizeLessThanTwo() {
        homer();

        getDs().aggregate(Book.class).bucket(bucket().groupBy("$copies").boundaries(10).outputField("count", sum(1)))
                .execute(BucketResult.class);
    }

    private void homer() {
        getDs().save(asList(new Book("The Banquet", "Dante", 2), new Book("Divine Comedy", "Dante", 1),
                new Book("Eclogues", "Dante", 2), new Book("The Odyssey", "Homer", 10),
                new Book("Iliad", "Homer", 10)));
    }

    @Test
    public void testBucketWithOptions() {
        homer();

        Iterator<BucketResult> aggregate = getDs().aggregate(Book.class)
                .bucket(bucket().groupBy("$copies").boundaries(1, 5, 10).defaultValue(-1).outputField("count", sum(1)))
                .execute(BucketResult.class);

        BucketResult result2 = aggregate.next();
        Assert.assertEquals(result2.getId(), valueOf(-1));
        Assert.assertEquals(result2.getCount(), 2);

        BucketResult result1 = aggregate.next();
        Assert.assertEquals(result1.getId(), valueOf(1));
        Assert.assertEquals(result1.getCount(), 3);

    }

    @Test(expectedExceptions = RuntimeException.class)
    public void testBucketWithUnsortedBoundaries() {
        homer();

        Iterator<BucketResult> aggregate = getDs().aggregate(Book.class).bucket(
                bucket().groupBy("$copies").boundaries(5, 1, 10).defaultValue("test").outputField("count", sum(1)))
                .execute(BucketResult.class);
    }

    @Test
    public void testBucketWithoutOptions() {
        homer();

        Iterator<BucketResult> aggregate = getDs().aggregate(Book.class)
                .bucket(bucket().groupBy("$copies").boundaries(1, 5, 12)).execute(BucketResult.class);
        BucketResult result1 = aggregate.next();
        Assert.assertEquals(result1.getId(), 1);
        Assert.assertEquals(result1.getCount(), 3);

        BucketResult result2 = aggregate.next();
        Assert.assertEquals(result2.getId(), valueOf(5));
        Assert.assertEquals(result2.getCount(), 2);
    }

    @Entity
    private static class BucketResult {
        @Id
        private Integer id;

        private int count;

        public int getCount() {
            return count;
        }

        public void setCount(int count) {
            this.count = count;
        }

        public Integer getId() {
            return id;
        }

        public void setId(Integer id) {
            this.id = id;
        }

        @Override
        public String toString() {
            return "BucketResult{" + "id=" + id + ", count=" + count + '}';
        }
    }
}
