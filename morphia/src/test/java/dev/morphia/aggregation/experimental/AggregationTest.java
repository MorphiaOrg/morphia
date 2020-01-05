/*
 * Copyright (c) 2008 - 2013 MongoDB, Inc. <http://mongodb.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.morphia.aggregation.experimental;

import com.mongodb.client.model.Collation;
import dev.morphia.TestBase;
import dev.morphia.aggregation.experimental.model.Author;
import dev.morphia.aggregation.experimental.model.Book;
import dev.morphia.aggregation.experimental.model.Inventory;
import dev.morphia.aggregation.experimental.model.Order;
import dev.morphia.aggregation.experimental.model.Sales;
import dev.morphia.aggregation.experimental.stages.AddFields;
import dev.morphia.aggregation.experimental.stages.AutoBucket;
import dev.morphia.aggregation.experimental.stages.Bucket;
import dev.morphia.aggregation.experimental.stages.Facet;
import dev.morphia.aggregation.experimental.stages.Group;
import dev.morphia.aggregation.experimental.stages.Match;
import dev.morphia.aggregation.experimental.stages.Sample;
import dev.morphia.aggregation.experimental.stages.SortByCount;
import dev.morphia.aggregation.experimental.stages.Unset;
import dev.morphia.aggregation.experimental.stages.Unwind;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.query.internal.MorphiaCursor;
import dev.morphia.testmodel.User;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.ComparisonFailure;
import org.junit.Test;

import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import static com.mongodb.client.model.CollationStrength.SECONDARY;
import static dev.morphia.aggregation.experimental.Lookup.from;
import static dev.morphia.aggregation.experimental.expressions.Accumulator.add;
import static dev.morphia.aggregation.experimental.expressions.Accumulator.sum;
import static dev.morphia.aggregation.experimental.expressions.Expression.field;
import static dev.morphia.aggregation.experimental.expressions.Expression.literal;
import static dev.morphia.aggregation.experimental.expressions.Expression.push;
import static dev.morphia.aggregation.experimental.stages.Group.id;
import static dev.morphia.aggregation.experimental.stages.Projection.of;
import static dev.morphia.aggregation.experimental.stages.Sort.on;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.bson.Document.parse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class AggregationTest extends TestBase {

    @Test
    public void testAdd() {
        getMapper().map(Sales.class);
        List<Document> parse = asList(
            parse("{ '_id' : 1, 'item' : 'abc', 'price' : 10, 'fee' : 2, date: ISODate('2014-03-01T08:00:00Z') }"),
            parse("{ '_id' : 2, 'item' : 'jkl', 'price' : 20, 'fee' : 1, date: ISODate('2014-03-01T09:00:00Z') }"),
            parse("{ '_id' : 3, 'item' : 'xyz', 'price' : 5,  'fee' : 0, date: ISODate('2014-03-15T09:00:00Z') }"));
        getDatabase().getCollection("sales", Document.class)
                     .insertMany(parse);
        Aggregation<Sales> pipeline = getDs()
                                          .aggregate(Sales.class)
                                          .project(of()
                                                       .include("item")
                                                       .include("total",
                                                           add(field("price"), field("fee"))));

        List<Document> list = pipeline.execute(Document.class).toList();
        List<Document> expected = asList(
            parse("{ '_id' : 1, 'item' : 'abc', 'total' : 12 }"),
            parse("{ '_id' : 2, 'item' : 'jkl', 'total' : 21 }"),
            parse("{ '_id' : 3, 'item' : 'xyz', 'total' : 5 } }"));
        for (int i = 1; i < 4; i++) {
            compare(i, expected, list);
        }
    }

    private void compare(final int id, final List<Document> expected, final List<Document> actual) {
        assertEquals(find(id, expected), find(id, actual));
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    private Document find(final int id, final List<Document> documents) {
        return documents.stream().filter(d -> d.getInteger("_id").equals(id)).findFirst().get();
    }

    @Test
    public void testAddFields() {
        List<Document> list = List.of(
            parse("{ _id: 1, student: 'Maya', homework: [ 10, 5, 10 ],quiz: [ 10, 8 ],extraCredit: 0 }"),
            parse("{ _id: 2, student: 'Ryan', homework: [ 5, 6, 5 ],quiz: [ 8, 8 ],extraCredit: 8 }"));

        getDatabase().getCollection("scores").insertMany(list);

        List<Document> result = getDs().aggregate(Score.class)
                                       .addFields(AddFields.of()
                                                           .field("totalHomework", sum(field("homework")))
                                                           .field("totalQuiz", sum(field("quiz"))))
                                       .addFields(AddFields.of()
                                                           .field("totalScore", add(field("totalHomework"),
                                                               field("totalQuiz"), field("extraCredit"))))
                                       .execute(Document.class)
                                       .toList();

        list = List.of(
            parse("{ '_id' : 1, 'student' : 'Maya', 'homework' : [ 10, 5, 10 ],'quiz' : [ 10, 8 ],'extraCredit' : 0, 'totalHomework' : 25,"
                  + " 'totalQuiz' : 18, 'totalScore' : 43 }"),
            parse("{ '_id' : 2, 'student' : 'Ryan', 'homework' : [ 5, 6, 5 ],'quiz' : [ 8, 8 ],'extraCredit' : 8, 'totalHomework' : 16, "
                  + "'totalQuiz' : 16, 'totalScore' : 40 }"));

        assertEquals(list, result);
    }

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

        getDatabase().getCollection("artwork").insertMany(list);

        List<Document> results = getDs().aggregate(Artwork.class)
                                        .autoBucket(AutoBucket.of()
                                                              .groupBy(field("price"))
                                                              .buckets(4))
                                        .execute(Document.class)
                                        .toList();

        List<Document> documents = List.of(
            parse("{'_id': { 'min': NumberDecimal('76.04'), 'max': NumberDecimal('159.00') },'count': 2}"),
            parse("{'_id': { 'min': NumberDecimal('159.00'), 'max': NumberDecimal('199.99') },'count': 2 }"),
            parse("{'_id': { 'min': NumberDecimal('199.99'), 'max': NumberDecimal('385.00') },'count': 2 }"),
            parse("{'_id': { 'min': NumberDecimal('385.00'), 'max': NumberDecimal('483.00') },'count': 2 }"));

        assertEquals(documents, results);
    }

    @Test
    public void testBucket() {
        List<Document> list = List.of(
            parse("{'_id': 1, 'title': 'The Pillars of Society', 'artist': 'Grosz', 'year': 1926, 'price': NumberDecimal('199.99') }"),
            parse("{'_id': 2, 'title': 'Melancholy III', 'artist': 'Munch', 'year': 1902, 'price': NumberDecimal('280.00') }"),
            parse("{'_id': 3, 'title': 'Dancer', 'artist': 'Miro', 'year': 1925, 'price': NumberDecimal('76.04') }"),
            parse("{'_id': 4, 'title': 'The Great Wave off Kanagawa', 'artist': 'Hokusai', 'price': NumberDecimal('167.30') }"),
            parse("{'_id': 5, 'title': 'The Persistence of Memory', 'artist': 'Dali', 'year': 1931, 'price': NumberDecimal('483.00') }"),
            parse("{'_id': 6, 'title': 'Composition VII', 'artist': 'Kandinsky', 'year': 1913, 'price': NumberDecimal('385.00') }"),
            parse("{'_id': 7, 'title': 'The Scream', 'artist': 'Munch', 'year': 1893}"),
            parse("{'_id': 8, 'title': 'Blue Flower', 'artist': 'O\\'Keefe', 'year': 1918, 'price': NumberDecimal('118.42') }"));

        getDatabase().getCollection("artwork").insertMany(list);

        List<Document> results = getDs().aggregate(Artwork.class).bucket(Bucket.of()
                                                                               .groupBy(field("price"))
                                                                               .boundaries(literal(0), literal(200), literal(400))
                                                                               .defaultValue("Other")
                                                                               .outputField("count", sum(literal(1)))
                                                                               .outputField("titles", push().single(field("title"))))
                                        .execute(Document.class)
                                        .toList();

        List<Document> documents = List.of(
            parse("{'_id': 0, 'count': 4, 'titles': ['The Pillars of Society', 'Dancer', 'The Great Wave off Kanagawa', 'Blue Flower']}"),
            parse("{'_id': 200, 'count': 2, 'titles': ['Melancholy III', 'Composition VII']}"),
            parse("{'_id': 'Other', 'count': 2, 'titles': ['The Persistence of Memory', 'The Scream']}"));
        assertEquals(documents, results);
    }

    @Test
    public void testCollation() {
        getDs().save(asList(new User("john doe", new Date()), new User("John Doe", new Date())));

        Aggregation<User> pipeline = getDs()
                                         .aggregate(User.class)
                                         .match(getDs().find(User.class)
                                                       .field("name").equal("john doe"));
        assertEquals(1, count(pipeline.execute(User.class)));

        assertEquals(2, count(pipeline.execute(User.class,
            new dev.morphia.aggregation.experimental.AggregationOptions()
                .collation(Collation.builder()
                                    .locale("en")
                                    .collationStrength(SECONDARY)
                                    .build()))));
    }

    @Test
    public void testCount() {
        List<Document> list = List.of(
            parse("{ '_id' : 1, 'subject' : 'History', 'score' : 88 }"),
            parse("{ '_id' : 2, 'subject' : 'History', 'score' : 92 }"),
            parse("{ '_id' : 3, 'subject' : 'History', 'score' : 97 }"),
            parse("{ '_id' : 4, 'subject' : 'History', 'score' : 71 }"),
            parse("{ '_id' : 5, 'subject' : 'History', 'score' : 79 }"),
            parse("{ '_id' : 6, 'subject' : 'History', 'score' : 83 }"));

        getDatabase().getCollection("scores").insertMany(list);

        Document scores = getDs().aggregate(Score.class)
                                 .match(getDs().find(Score.class)
                                               .filter("score >", 80))
                                 .count("passing_scores")
                                 .execute(Document.class)
                                 .next();
        assertEquals(parse("{ \"passing_scores\" : 4 }"), scores);
    }

    @Test
    public void testFacet() {
        List<Document> list = List.of(
            parse("{'_id': 1, 'title': 'The Pillars of Society', 'artist': 'Grosz', 'year': 1926, 'price': NumberDecimal('199.99'),"
                  + " 'tags': [ 'painting', 'satire', 'Expressionism', 'caricature' ] }"),
            parse("{'_id': 2, 'title': 'Melancholy III', 'artist': 'Munch', 'year': 1902, 'price': NumberDecimal('280.00'),"
                  + " 'tags': [ 'woodcut', 'Expressionism' ] }"),
            parse("{'_id': 3, 'title': 'Dancer', 'artist': 'Miro', 'year': 1925, 'price': NumberDecimal('76.04'),"
                  + " 'tags': [ 'oil', 'Surrealism', 'painting' ] }"),
            parse("{'_id': 4, 'title': 'The Great Wave off Kanagawa', 'artist': 'Hokusai', 'price': NumberDecimal('167.30'),"
                  + " 'tags': [ 'woodblock', 'ukiyo-e' ] }"),
            parse("{'_id': 5, 'title': 'The Persistence of Memory', 'artist': 'Dali', 'year': 1931, 'price': NumberDecimal('483.00'),"
                + " 'tags': [ 'Surrealism', 'painting', 'oil' ] }"),
            parse("{'_id': 6, 'title': 'Composition VII', 'artist': 'Kandinsky', 'year': 1913, 'price': NumberDecimal('385.00'), "
                + "'tags': [ 'oil', 'painting', 'abstract' ] }"),
            parse("{'_id': 7, 'title': 'The Scream', 'artist': 'Munch', 'year': 1893, 'tags': [ 'Expressionism', 'painting', 'oil' ] }"),
            parse("{'_id': 8, 'title': 'Blue Flower', 'artist': 'O\\'Keefe', 'year': 1918, 'price': NumberDecimal('118.42'),"
                  + " 'tags': [ 'abstract', 'painting' ] }"));

        getDatabase().getCollection("artwork").insertMany(list);

        Document result = getDs().aggregate(Artwork.class)
                                 .facet(Facet.of()
                                             .field("categorizedByTags",
                                                 Unwind.on("tags"),
                                                 SortByCount.on(field("tags")))
                                             .field("categorizedByPrice",
                                                 Match.of(getDs().find(Artwork.class)
                                                                 .field("price").exists()),
                                                 Bucket.of()
                                                       .groupBy(field("price"))
                                                       .boundaries(literal(0), literal(150), literal(200), literal(300), literal(400))
                                                       .defaultValue("Other")
                                                       .outputField("count", sum(literal(1)))
                                                       .outputField("titles", push().single(field("title"))))
                                             .field("categorizedByYears(Auto)", AutoBucket.of()
                                                                                          .groupBy(field("year"))
                                                                                          .buckets(4)))
                                 .execute(Document.class)
                                 .next();

        Document document = parse("{"
                                  + "    'categorizedByTags' : ["
                                  + "    { '_id' : 'painting', 'count' : 6 },"
                                  + "    { '_id' : 'oil', 'count' : 4 },"
                                  + "    { '_id' : 'Expressionism', 'count' : 3 },"
                                  + "    { '_id' : 'Surrealism', 'count' : 2 },"
                                  + "    { '_id' : 'abstract', 'count' : 2 },"
                                  + "    { '_id' : 'woodblock', 'count' : 1 },"
                                  + "    { '_id' : 'woodcut', 'count' : 1 },"
                                  + "    { '_id' : 'ukiyo-e', 'count' : 1 },"
                                  + "    { '_id' : 'satire', 'count' : 1 },"
                                  + "    { '_id' : 'caricature', 'count' : 1 }"
                                  + "    ],"
                                  + "    'categorizedByYears(Auto)' : ["
                                  + "    { '_id' : { 'min' : null, 'max' : 1902 }, 'count' : 2 },"
                                  + "    { '_id' : { 'min' : 1902, 'max' : 1918 }, 'count' : 2 },"
                                  + "    { '_id' : { 'min' : 1918, 'max' : 1926 }, 'count' : 2 },"
                                  + "    { '_id' : { 'min' : 1926, 'max' : 1931 }, 'count' : 2 }"
                                  + "    ],"
                                  + "    'categorizedByPrice' : ["
                                  + "    { '_id' : 0, 'count' : 2, 'titles' : ['Dancer', 'Blue Flower']},"
                                  + "    { '_id' : 150, 'count' : 2, 'titles' : ['The Pillars of Society', 'The Great Wave off Kanagawa']},"
                                  + "    { '_id' : 200, 'count' : 1, 'titles' : ['Melancholy III']},"
                                  + "    { '_id' : 300, 'count' : 1, 'titles' : ['Composition VII']},"
                                  + "    { '_id' : 'Other', 'count' : 1, 'titles' : ['The Persistence of Memory']}"
                                  + "    ],"
                                  + "}");

        assertDocumentEquals(document, result);
    }

    private void assertDocumentEquals(final Document document, final Document result) {
        assertDocumentEquals("", document, result);
    }

    private void assertDocumentEquals(final String path, final Object expected, final Object actual) {
        if(expected instanceof Document) {
            for (final Entry<String, Object> entry : ((Document) expected).entrySet()) {
                final String key = entry.getKey();
                assertDocumentEquals(path.isEmpty() ? key : (path + "." + key), entry.getValue(), ((Document) actual).get(key));
            }
        } else if(expected instanceof List) {
            List list = (List) expected;
            for (int i = 0; i < list.size(); i++) {
                final Object o = list.get(i);
                boolean found = false;
                final Iterator actualIterator = ((List) actual).iterator();
                while(!found && actualIterator.hasNext()) {
                    try {
                        assertDocumentEquals(format("%s[%d]", path, i), o, actualIterator.next());
                        found = true;
                    } catch (AssertionError ignore) {
                    }
                }
                if(!found) {
                    fail(format("mismatch found at %s:%n%s", path));
                }
            }
        } else {
            assertEquals(format("mismatch found at %s:%n%s", path, expected, actual), expected, actual);
        }
    }

    @Test
    public void testLimit() {
        getDs().save(asList(new Book("The Banquet", "Dante", 2),
            new Book("Divine Comedy", "Dante", 1),
            new Book("Eclogues", "Dante", 2),
            new Book("The Odyssey", "Homer", 10),
            new Book("Iliad", "Homer", 10)));

        List<Book> aggregate = getDs().aggregate(Book.class)
                                      .limit(2)
                                      .execute(Book.class)
                                      .toList();
        Assert.assertEquals(2, aggregate.size());
    }

    @Test
    public void testLookup() {
        // Test data pulled from https://docs.mongodb.com/v3.2/reference/operator/aggregation/lookup/
        getDs().save(asList(new Order(1, "abc", 12, 2),
            new Order(2, "jkl", 20, 1),
            new Order(3)));
        List<Inventory> inventories = asList(new Inventory(1, "abc", "product 1", 120),
            new Inventory(2, "def", "product 2", 80),
            new Inventory(3, "ijk", "product 3", 60),
            new Inventory(4, "jkl", "product 4", 70),
            new Inventory(5, null, "Incomplete"),
            new Inventory(6));
        getDs().save(inventories);

        List<Order> lookups = getDs().aggregate(Order.class)
                                     .lookup(from(Inventory.class)
                                                 .localField("item")
                                                 .foreignField("sku")
                                                 .as("inventoryDocs"))
                                     .sort(on().ascending("_id"))
                                     .execute(Order.class)
                                     .toList();
        Assert.assertEquals(inventories.get(0), lookups.get(0).getInventoryDocs().get(0));
        Assert.assertEquals(inventories.get(3), lookups.get(1).getInventoryDocs().get(0));
        Assert.assertEquals(inventories.get(4), lookups.get(2).getInventoryDocs().get(0));
        Assert.assertEquals(inventories.get(5), lookups.get(2).getInventoryDocs().get(1));
    }

    @Test
    public void testNullGroupId() {
        getDs().save(asList(new User("John", new Date()),
            new User("Paul", new Date()),
            new User("George", new Date()),
            new User("Ringo", new Date())));
        Aggregation<User> pipeline = getDs()
                                         .aggregate(User.class)
                                         .group(Group.of()
                                                     .fields("count", sum(literal(1))));

        Group group = pipeline.getStage("$group");
        Assert.assertNull(group.getId());
        assertEquals(1, group.getFields().size());

        Document execute = pipeline.execute(Document.class).tryNext();
        assertEquals(Integer.valueOf(4), execute.getInteger("count"));
    }

    @Test
    public void testOut() {
        getDs().save(asList(new Book("The Banquet", "Dante", 2),
            new Book("Divine Comedy", "Dante", 1),
            new Book("Eclogues", "Dante", 2),
            new Book("The Odyssey", "Homer", 10),
            new Book("Iliad", "Homer", 10)));

        AggregationOptions options = new AggregationOptions();
        Aggregation<Book> aggregation = getDs().aggregate(Book.class)
                                               .group(Group.of(id("author"))
                                                           .fields("books", push()
                                                                                .single(field("title"))));
        aggregation.out(Author.class, options);
        Assert.assertEquals(2, getMapper().getCollection(Author.class).countDocuments());

        aggregation.out("different");
        Assert.assertEquals(2, getDatabase().getCollection("different").countDocuments());
    }

    @Test
    public void testProjection() {
        var doc = parse("{'_id' : 1, title: 'abc123', isbn: '0001122223334', author: { last: 'zzz', first: 'aaa' }, copies: 5,\n"
                        + "  lastModified: '2016-07-28'}");

        getDatabase().getCollection("books").insertOne(doc);
        Aggregation<Book> pipeline = getDs().aggregate(Book.class)
                                            .project(of()
                                                         .include("title")
                                                         .include("author"));
        MorphiaCursor<Document> aggregate = pipeline.execute(Document.class);
        doc = parse("{ '_id' : 1, title: 'abc123', author: { last: 'zzz', first: 'aaa' }}");
        Assert.assertEquals(doc, aggregate.next());

        pipeline = getDs().aggregate(Book.class)
                          .project(of()
                                       .supressId()
                                       .include("title")
                                       .include("author"));
        aggregate = pipeline.execute(Document.class);

        doc = parse("{title: 'abc123', author: { last: 'zzz', first: 'aaa' }}");
        Assert.assertEquals(doc, aggregate.next());

        pipeline = getDs().aggregate(Book.class)
                          .project(of()
                                       .exclude("lastModified"));
        aggregate = pipeline.execute(Document.class);

        doc = parse("{'_id' : 1, title: 'abc123', isbn: '0001122223334', author: { last: 'zzz', first: 'aaa' }, copies: 5}");
        Assert.assertEquals(doc, aggregate.next());
    }

    @Test
    public void testSample() {
        getDs().save(asList(new User("John", new Date()),
            new User("Paul", new Date()),
            new User("George", new Date()),
            new User("Ringo", new Date())));
        Aggregation<User> pipeline = getDs()
                                         .aggregate(User.class)
                                         .sample(Sample.of(1));
        Sample sample = pipeline.getStage("$sample");
        assertEquals(1, sample.getSize());

        List<User> list = pipeline.execute(User.class).toList();
        assertEquals(1, list.size());
    }

    @Test
    public void testUnset() {
        List<Document> documents = List.of(
            parse("{'_id': 1, title: 'Antelope Antics', isbn: '0001122223334', author: {last:'An', first: 'Auntie' }, copies: "
                  + "[ {warehouse: 'A', qty: 5 }, {warehouse: 'B', qty: 15 } ] }"),
            parse("{'_id': 2, title: 'Bees Babble', isbn: '999999999333', author: {last:'Bumble', first: 'Bee' }, copies: [ "
                  + "{warehouse: 'A', qty: 2 }, {warehouse: 'B', qty: 5 } ] }"));
        getDatabase().getCollection("books")
                     .insertMany(documents);

        for (final Document document : documents) {
            document.remove("copies");
        }

        List<Document> copies = getDs().aggregate(Book.class)
                                       .unset(Unset.fields("copies"))
                                       .execute(Document.class)
                                       .toList();

        assertEquals(documents, copies);

    }

    @Entity(useDiscriminator = false)
    public static class Artwork {
        @Id
        private ObjectId id;
        private Double price;
    }

    @Entity(value = "scores", useDiscriminator = false)
    private static class Score {
        @Id
        private ObjectId id;
        private int score;
    }
}
