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

import com.mongodb.ReadConcern;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Collation;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.MergeOptions.WhenMatched;
import com.mongodb.client.model.MergeOptions.WhenNotMatched;
import dev.morphia.TestBase;
import dev.morphia.aggregation.experimental.expressions.Expression;
import dev.morphia.aggregation.experimental.model.Author;
import dev.morphia.aggregation.experimental.model.Book;
import dev.morphia.aggregation.experimental.model.Inventory;
import dev.morphia.aggregation.experimental.model.Order;
import dev.morphia.aggregation.experimental.model.Sales;
import dev.morphia.aggregation.experimental.stages.AddFields;
import dev.morphia.aggregation.experimental.stages.AutoBucket;
import dev.morphia.aggregation.experimental.stages.Bucket;
import dev.morphia.aggregation.experimental.stages.CollectionStats;
import dev.morphia.aggregation.experimental.stages.Facet;
import dev.morphia.aggregation.experimental.stages.Group;
import dev.morphia.aggregation.experimental.stages.Match;
import dev.morphia.aggregation.experimental.stages.Redact;
import dev.morphia.aggregation.experimental.stages.ReplaceRoot;
import dev.morphia.aggregation.experimental.stages.Sample;
import dev.morphia.aggregation.experimental.stages.SortByCount;
import dev.morphia.aggregation.experimental.stages.Unset;
import dev.morphia.aggregation.experimental.stages.Unwind;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.query.Type;
import dev.morphia.query.internal.MorphiaCursor;
import dev.morphia.testmodel.User;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Test;

import java.util.Date;
import java.util.List;

import static com.mongodb.client.model.CollationStrength.SECONDARY;
import static dev.morphia.aggregation.experimental.Lookup.from;
import static dev.morphia.aggregation.experimental.expressions.Accumulator.add;
import static dev.morphia.aggregation.experimental.expressions.Accumulator.sum;
import static dev.morphia.aggregation.experimental.expressions.ArrayExpression.array;
import static dev.morphia.aggregation.experimental.expressions.ArrayExpression.size;
import static dev.morphia.aggregation.experimental.expressions.Comparison.gt;
import static dev.morphia.aggregation.experimental.expressions.ConditionalExpression.condition;
import static dev.morphia.aggregation.experimental.expressions.ConditionalExpression.ifNull;
import static dev.morphia.aggregation.experimental.expressions.Expression.field;
import static dev.morphia.aggregation.experimental.expressions.Expression.literal;
import static dev.morphia.aggregation.experimental.expressions.Expression.push;
import static dev.morphia.aggregation.experimental.expressions.ObjectExpressions.mergeObjects;
import static dev.morphia.aggregation.experimental.expressions.SetExpression.setIntersection;
import static dev.morphia.aggregation.experimental.stages.Group.group;
import static dev.morphia.aggregation.experimental.stages.Group.id;
import static dev.morphia.aggregation.experimental.stages.Merge.merge;
import static dev.morphia.aggregation.experimental.stages.Projection.of;
import static dev.morphia.aggregation.experimental.stages.ReplaceWith.with;
import static dev.morphia.aggregation.experimental.stages.Sort.on;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.bson.Document.parse;
import static org.junit.Assert.assertEquals;

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

        List<Document> list = pipeline.execute().toList();
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
                                       .execute()
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
                                        .execute()
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
                                        .execute()
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
    public void testCollectionStats() {
        getDs().save(new Author());
        Document execute = getDs().aggregate(Author.class)
                                  .collStats(CollectionStats.with()
                                                            .histogram(true)
                                                            .scale(42)
                                                            .count(true))
                                  .execute()
                                  .next();
        System.out.println(execute);
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
                                 .execute()
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
                                                 Match.on(getDs().find(Artwork.class)
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
                                 .execute()
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

    @Test
    public void testGraphLookup() {
        List<Document> list = List.of(parse("{ '_id' : 1, 'name' : 'Dev' }"),
            parse("{ '_id' : 2, 'name' : 'Eliot', 'reportsTo' : 'Dev' }"),
            parse("{ '_id' : 3, 'name' : 'Ron', 'reportsTo' : 'Eliot' }"),
            parse("{ '_id' : 4, 'name' : 'Andrew', 'reportsTo' : 'Eliot' }"),
            parse("{ '_id' : 5, 'name' : 'Asya', 'reportsTo' : 'Ron' }"),
            parse("{ '_id' : 6, 'name' : 'Dan', 'reportsTo' : 'Andrew' }"));

        getDatabase().getCollection("employees").insertMany(list);

        List<Document> actual = getDs().aggregate(Employee.class)
                                       .graphLookup(GraphLookup.with()
                                                               .from("employees")
                                                               .startWith(field("reportsTo"))
                                                               .connectFromField("reportsTo")
                                                               .connectToField("name")
                                                               .as("reportingHierarchy"))
                                       .execute()
                                       .toList();

        List<Document> expected = List.of(parse("{'_id': 1, 'name': 'Dev', 'reportingHierarchy': []}"),
            parse("{'_id': 2, 'name': 'Eliot', 'reportsTo': 'Dev', 'reportingHierarchy': [{'_id': 1, 'name': 'Dev'}]}"),
            parse("{'_id': 3, 'name': 'Ron', 'reportsTo': 'Eliot', 'reportingHierarchy': [{'_id': 1, 'name': 'Dev'},{'_id': 2, 'name': "
                  + "'Eliot', 'reportsTo': 'Dev'}]}"),
            parse("{'_id': 4, 'name': 'Andrew', 'reportsTo': 'Eliot', 'reportingHierarchy': [{'_id': 1, 'name': 'Dev'},{'_id': 2, 'name': "
                  + "'Eliot', 'reportsTo': 'Dev'}]}"),
            parse("{'_id': 5, 'name': 'Asya', 'reportsTo': 'Ron', 'reportingHierarchy': [{'_id': 1, 'name': 'Dev'},{'_id': 2, 'name': "
                  + "'Eliot', 'reportsTo': 'Dev'},{'_id': 3, 'name': 'Ron', 'reportsTo': 'Eliot'}]}"),
            parse("{'_id': 6, 'name': 'Dan', 'reportsTo': 'Andrew', 'reportingHierarchy': [{'_id': 1, 'name': 'Dev'},{'_id': 2, 'name': "
                  + "'Eliot', 'reportsTo': 'Dev'},{'_id': 4, 'name': 'Andrew', 'reportsTo': 'Eliot'}]}"));

        assertDocumentEquals(expected, actual);
    }

    @Test
    public void testIndexStats() {
        getDs().getMapper().map(Author.class);
        getDs().ensureIndexes();
        Document stats = getDs().aggregate(Author.class)
                                .indexStats()
                                .match(getDs().find()
                                              .filter("name", "books_1"))
                                .execute()
                                .next();

        Assert.assertNotNull(stats);
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
    public void testMerge() {
        MongoCollection<Document> salaries = getDatabase().getCollection("salaries");

        salaries.insertMany(List.of(
            parse("{ '_id' : 1, employee: 'Ant', dept: 'A', salary: 100000, fiscal_year: 2017 }"),
            parse("{ '_id' : 2, employee: 'Bee', dept: 'A', salary: 120000, fiscal_year: 2017 }"),
            parse("{ '_id' : 3, employee: 'Cat', dept: 'Z', salary: 115000, fiscal_year: 2017 }"),
            parse("{ '_id' : 4, employee: 'Ant', dept: 'A', salary: 115000, fiscal_year: 2018 }"),
            parse("{ '_id' : 5, employee: 'Bee', dept: 'Z', salary: 145000, fiscal_year: 2018 }"),
            parse("{ '_id' : 6, employee: 'Cat', dept: 'Z', salary: 135000, fiscal_year: 2018 }"),
            parse("{ '_id' : 7, employee: 'Gecko', dept: 'A', salary: 100000, fiscal_year: 2018 }"),
            parse("{ '_id' : 8, employee: 'Ant', dept: 'A', salary: 125000, fiscal_year: 2019 }"),
            parse("{ '_id' : 9, employee: 'Bee', dept: 'Z', salary: 160000, fiscal_year: 2019 }"),
            parse("{ '_id' : 10, employee: 'Cat', dept: 'Z', salary: 150000, fiscal_year: 2019 }")));

        List<Document> actual = getDs().aggregate(Salary.class)
                                       .group(group(id()
                                                        .field("fiscal_year")
                                                        .field("dept"))
                                                  .field("salaries", sum(field("salary"))))
                                       .merge(merge()
                                                  .into("budgets")
                                                  .on("_id")
                                                  .whenMatched(WhenMatched.REPLACE)
                                                  .whenNotMatched(WhenNotMatched.INSERT))
                                       .execute()
                                       .toList();

        List<Document> expected = List.of(
            parse("{ '_id' : { 'fiscal_year' : 2017, 'dept' : 'A' }, 'salaries' : 220000 }"),
            parse("{ '_id' : { 'fiscal_year' : 2017, 'dept' : 'Z' }, 'salaries' : 115000 }"),
            parse("{ '_id' : { 'fiscal_year' : 2018, 'dept' : 'A' }, 'salaries' : 215000 }"),
            parse("{ '_id' : { 'fiscal_year' : 2018, 'dept' : 'Z' }, 'salaries' : 280000 }"),
            parse("{ '_id' : { 'fiscal_year' : 2019, 'dept' : 'A' }, 'salaries' : 125000 }"),
            parse("{ '_id' : { 'fiscal_year' : 2019, 'dept' : 'Z' }, 'salaries' : 310000 }"));

        assertDocumentEquals(expected, actual);
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
                                                     .field("count", sum(literal(1))));

        Group group = pipeline.getStage("$group");
        Assert.assertNull(group.getId());
        assertEquals(1, group.getFields().size());

        Document execute = pipeline.execute().tryNext();
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
                                               .group(group(id("author"))
                                                          .field("books", push()
                                                                              .single(field("title"))));
        aggregation.out(Author.class, options);
        Assert.assertEquals(2, getMapper().getCollection(Author.class).countDocuments());

        aggregation.out("different");
        Assert.assertEquals(2, getDatabase().getCollection("different").countDocuments());
    }

    @Test
    public void testPlanCacheStats() {
        List<Document> list = List.of(
            parse("{ '_id' : 1, 'item' : 'abc', 'price' : NumberDecimal('12'), 'quantity' : 2, 'type': 'apparel' }"),
            parse("{ '_id' : 2, 'item' : 'jkl', 'price' : NumberDecimal('20'), 'quantity' : 1, 'type': 'electronics' }"),
            parse("{ '_id' : 3, 'item' : 'abc', 'price' : NumberDecimal('10'), 'quantity' : 5, 'type': 'apparel' }"),
            parse("{ '_id' : 4, 'item' : 'abc', 'price' : NumberDecimal('8'), 'quantity' : 10, 'type': 'apparel' }"),
            parse("{ '_id' : 5, 'item' : 'jkl', 'price' : NumberDecimal('15'), 'quantity' : 15, 'type': 'electronics' }"));

        MongoCollection<Document> orders = getDatabase().getCollection("orders");
        orders.insertMany(list);

        Assert.assertNotNull(orders.createIndex(new Document("item", 1)));
        Assert.assertNotNull(orders.createIndex(new Document("item", 1)
                                                    .append("quantity", 1)));
        Assert.assertNotNull(orders.createIndex(new Document("item", 1)
                                                    .append("price", 1),
            new IndexOptions()
                .partialFilterExpression(new Document("price", new Document("$gte", 10)))));
        Assert.assertNotNull(orders.createIndex(new Document("quantity", 1)));
        Assert.assertNotNull(orders.createIndex(new Document("quantity", 1)
                                                    .append("type", 1)));

        orders.find(parse(" { item: 'abc', price: { $gte: NumberDecimal('10') } }"));
        orders.find(parse(" { item: 'abc', price: { $gte: NumberDecimal('5') } }"));
        orders.find(parse(" { quantity: { $gte: 20 } } "));
        orders.find(parse(" { quantity: { $gte: 5 }, type: 'apparel' } "));

        List<Document> stats = getDs().aggregate(Order.class)
                                      .planCacheStats()
                                      .execute(Document.class, new AggregationOptions()
                                                                   .readConcern(ReadConcern.LOCAL))
                                      .toList();

        Assert.assertNotNull(stats);
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
        MorphiaCursor<Document> aggregate = pipeline.execute();
        doc = parse("{ '_id' : 1, title: 'abc123', author: { last: 'zzz', first: 'aaa' }}");
        Assert.assertEquals(doc, aggregate.next());

        pipeline = getDs().aggregate(Book.class)
                          .project(of()
                                       .supressId()
                                       .include("title")
                                       .include("author"));
        aggregate = pipeline.execute();

        doc = parse("{title: 'abc123', author: { last: 'zzz', first: 'aaa' }}");
        Assert.assertEquals(doc, aggregate.next());

        pipeline = getDs().aggregate(Book.class)
                          .project(of()
                                       .exclude("lastModified"));
        aggregate = pipeline.execute();

        doc = parse("{'_id' : 1, title: 'abc123', isbn: '0001122223334', author: { last: 'zzz', first: 'aaa' }, copies: 5}");
        Assert.assertEquals(doc, aggregate.next());
    }

    @Test
    public void testReplaceRoot() {
        List<Document> documents = List.of(
            parse("{'_id': 1, 'name': {'first': 'John', 'last': 'Backus'}}"),
            parse("{'_id': 2, 'name': {'first': 'John', 'last': 'McCarthy'}}"),
            parse("{'_id': 3, 'name': {'first': 'Grace', 'last': 'Hopper'}}"),
            parse("{'_id': 4, 'firstname': 'Ole-Johan', 'lastname': 'Dahl'}"));

        getDatabase().getCollection("authors").insertMany(documents);

        List<Document> actual = getDs().aggregate(Author.class)
                                       .match(getDs().find()
                                                     .field("name").exists()
                                                     .field("name").not().type(Type.ARRAY)
                                                     .field("name").type(Type.OBJECT))
                                       .replaceRoot(ReplaceRoot.with().value(field("name")))
                                       .execute()
                                       .toList();
        List<Document> expected = documents.subList(0, 3)
                                           .stream()
                                           .map(d -> (Document) d.get("name"))
                                           .collect(toList());
        assertDocumentEquals(expected, actual);

        actual = getDs().aggregate(Author.class)
                        .replaceRoot(ReplaceRoot.with()
                                                .value(ifNull().target(field("name"))
                                                               .field("_id", field("_id"))
                                                               .field("missingName", literal(true))))
                        .execute()
                        .toList();
        expected = documents.subList(0, 3)
                            .stream()
                            .map(d -> (Document) d.get("name"))
                            .collect(toList());
        expected.add(new Document("_id", 4)
                         .append("missingName", true));
        assertDocumentEquals(expected, actual);

        actual = getDs().aggregate(Author.class)
                        .replaceRoot(ReplaceRoot.with()
                                                .value(mergeObjects()
                                                           .add(Expression.of()
                                                                          .field("_id", field("_id"))
                                                                          .field("first", literal(""))
                                                                          .field("last", literal("")))
                                                           .add(field("name"))))
                        .execute()
                        .toList();
        expected = documents.subList(0, 3)
                            .stream()
                            .map(d -> {
                                d.putAll((Document) d.remove("name"));
                                return d;
                            })
                            .collect(toList());
        expected.add(new Document("_id", 4)
                         .append("first", "")
                         .append("last", ""));
        assertDocumentEquals(expected, actual);
    }

    @Test
    public void testReplaceWith() {
        List<Document> documents = List.of(
            parse("{'_id': 1, 'name': {'first': 'John', 'last': 'Backus'}}"),
            parse("{'_id': 2, 'name': {'first': 'John', 'last': 'McCarthy'}}"),
            parse("{'_id': 3, 'name': {'first': 'Grace', 'last': 'Hopper'}}"),
            parse("{'_id': 4, 'firstname': 'Ole-Johan', 'lastname': 'Dahl'}"));

        getDatabase().getCollection("authors").insertMany(documents);

        List<Document> actual = getDs().aggregate(Author.class)
                                       .match(getDs().find()
                                                     .field("name").exists()
                                                     .field("name").not().type(Type.ARRAY)
                                                     .field("name").type(Type.OBJECT))
                                       .replaceWith(with().value(field("name")))
                                       .execute()
                                       .toList();
        List<Document> expected = documents.subList(0, 3)
                                           .stream()
                                           .map(d -> (Document) d.get("name"))
                                           .collect(toList());
        assertDocumentEquals(expected, actual);

        actual = getDs().aggregate(Author.class)
                        .replaceWith(with()
                                         .value(ifNull().target(field("name"))
                                                        .field("_id", field("_id"))
                                                        .field("missingName", literal(true))))
                        .execute()
                        .toList();
        expected = documents.subList(0, 3)
                            .stream()
                            .map(d -> (Document) d.get("name"))
                            .collect(toList());
        expected.add(new Document("_id", 4)
                         .append("missingName", true));
        assertDocumentEquals(expected, actual);

        actual = getDs().aggregate(Author.class)
                        .replaceWith(with()
                                         .value(mergeObjects()
                                                    .add(Expression.of()
                                                                   .field("_id", field("_id"))
                                                                   .field("first", literal(""))
                                                                   .field("last", literal("")))
                                                    .add(field("name"))))
                        .execute()
                        .toList();
        expected = documents.subList(0, 3)
                            .stream()
                            .map(d -> {
                                d.putAll((Document) d.remove("name"));
                                return d;
                            })
                            .collect(toList());
        expected.add(new Document("_id", 4)
                         .append("first", "")
                         .append("last", ""));
        assertDocumentEquals(expected, actual);
    }

    @Test
    public void testSample() {
        getDs().save(asList(new User("John", new Date()),
            new User("Paul", new Date()),
            new User("George", new Date()),
            new User("Ringo", new Date())));
        Aggregation<User> pipeline = getDs()
                                         .aggregate(User.class)
                                         .sample(Sample.of(3));
        Sample sample = pipeline.getStage("$sample");
        assertEquals(3, sample.getSize());

        List<User> list = pipeline.execute(User.class).toList();
        assertEquals(3, list.size());
    }

    @Test
    public void testSet() {
        List<Document> list = List.of(
            parse("{ _id: 1, student: 'Maya', homework: [ 10, 5, 10 ],quiz: [ 10, 8 ],extraCredit: 0 }"),
            parse("{ _id: 2, student: 'Ryan', homework: [ 5, 6, 5 ],quiz: [ 8, 8 ],extraCredit: 8 }"));

        getDatabase().getCollection("scores").insertMany(list);

        List<Document> result = getDs().aggregate(Score.class)
                                       .set(AddFields.of()
                                                     .field("totalHomework", sum(field("homework")))
                                                     .field("totalQuiz", sum(field("quiz"))))
                                       .set(AddFields.of()
                                                     .field("totalScore", add(field("totalHomework"),
                                                         field("totalQuiz"), field("extraCredit"))))
                                       .execute()
                                       .toList();

        list = List.of(
            parse("{ '_id' : 1, 'student' : 'Maya', 'homework' : [ 10, 5, 10 ],'quiz' : [ 10, 8 ],'extraCredit' : 0, 'totalHomework' : 25,"
                  + " 'totalQuiz' : 18, 'totalScore' : 43 }"),
            parse("{ '_id' : 2, 'student' : 'Ryan', 'homework' : [ 5, 6, 5 ],'quiz' : [ 8, 8 ],'extraCredit' : 8, 'totalHomework' : 16, "
                  + "'totalQuiz' : 16, 'totalScore' : 40 }"));

        assertEquals(list, result);
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
                                       .execute()
                                       .toList();

        assertEquals(documents, copies);

    }

    @Test
    public void testRedact() {
        Document document = parse(
            "{ _id: 1, title: '123 Department Report', tags: [ 'G', 'STLW' ],year: 2014, subsections: [{ subtitle: 'Section 1: Overview',"
            + " tags: [ 'SI', 'G' ],content:  'Section 1: This is the content of section 1.' },{ subtitle: 'Section 2: Analysis', tags: "
            + "[ 'STLW' ], content: 'Section 2: This is the content of section 2.' },{ subtitle: 'Section 3: Budgeting', tags: [ 'TK' ],"
            + "content: { text: 'Section 3: This is the content of section3.', tags: [ 'HCS' ]} }]}");

        getDatabase().getCollection("forecasts").insertOne(document);

        Document actual = getDs().aggregate("forecasts")
                               .match(getDs().find().filter("year", 2014))
                               .redact(Redact.on(condition(
                                   gt(size(setIntersection(field("tags"), array(literal("STLW"), literal("G")))), literal(0)),
                                   literal("$$DESCEND"),
                                   literal("$$PRUNE"))))
                               .execute()
                               .next();
        Document expected = parse("{ '_id' : 1, 'title' : '123 Department Report', 'tags' : [ 'G', 'STLW' ],'year' : 2014, 'subsections' :"
                                 + " [{ 'subtitle' : 'Section 1: Overview', 'tags' : [ 'SI', 'G' ],'content' : 'Section 1: This is the "
                                + "content of section 1.' },{ 'subtitle' : 'Section 2: Analysis', 'tags' : [ 'STLW' ],'content' : "
                                + "'Section 2: This is the content of section 2.' }]}");

        assertEquals(expected, actual);
    }

    @Entity(useDiscriminator = false)
    public static class Artwork {
        @Id
        private ObjectId id;
        private Double price;
    }

    @Entity(value = "employees", useDiscriminator = false)
    private static class Employee {
        @Id
        private ObjectId id;
    }

    @Entity("salaries")
    public static class Salary {
        @Id
        private ObjectId id;
    }

    @Entity(value = "scores", useDiscriminator = false)
    private static class Score {
        @Id
        private ObjectId id;
        private int score;
    }
}
