package dev.morphia.test.aggregation.experimental;

import com.mongodb.ReadConcern;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Collation;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.MergeOptions.WhenMatched;
import com.mongodb.client.model.MergeOptions.WhenNotMatched;
import dev.morphia.aggregation.experimental.Aggregation;
import dev.morphia.aggregation.experimental.AggregationOptions;
import dev.morphia.aggregation.experimental.expressions.ComparisonExpressions;
import dev.morphia.aggregation.experimental.expressions.Expressions;
import dev.morphia.aggregation.experimental.stages.AddFields;
import dev.morphia.aggregation.experimental.stages.AutoBucket;
import dev.morphia.aggregation.experimental.stages.Bucket;
import dev.morphia.aggregation.experimental.stages.CollectionStats;
import dev.morphia.aggregation.experimental.stages.Facet;
import dev.morphia.aggregation.experimental.stages.GraphLookup;
import dev.morphia.aggregation.experimental.stages.Group;
import dev.morphia.aggregation.experimental.stages.Lookup;
import dev.morphia.aggregation.experimental.stages.Match;
import dev.morphia.aggregation.experimental.stages.Merge;
import dev.morphia.aggregation.experimental.stages.Out;
import dev.morphia.aggregation.experimental.stages.Redact;
import dev.morphia.aggregation.experimental.stages.ReplaceRoot;
import dev.morphia.aggregation.experimental.stages.SortByCount;
import dev.morphia.aggregation.experimental.stages.Unset;
import dev.morphia.aggregation.experimental.stages.Unwind;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Property;
import dev.morphia.query.MorphiaCursor;
import dev.morphia.query.Sort;
import dev.morphia.query.Type;
import dev.morphia.test.TestBase;
import dev.morphia.test.aggregation.experimental.model.Author;
import dev.morphia.test.aggregation.experimental.model.Book;
import dev.morphia.test.aggregation.experimental.model.Inventory;
import dev.morphia.test.aggregation.experimental.model.Order;
import dev.morphia.test.aggregation.experimental.model.Sales;
import dev.morphia.test.models.User;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.jetbrains.annotations.NotNull;
import org.testng.annotations.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.mongodb.client.model.CollationStrength.SECONDARY;
import static dev.morphia.aggregation.experimental.expressions.AccumulatorExpressions.avg;
import static dev.morphia.aggregation.experimental.expressions.AccumulatorExpressions.push;
import static dev.morphia.aggregation.experimental.expressions.AccumulatorExpressions.sum;
import static dev.morphia.aggregation.experimental.expressions.ArrayExpressions.array;
import static dev.morphia.aggregation.experimental.expressions.ArrayExpressions.size;
import static dev.morphia.aggregation.experimental.expressions.ComparisonExpressions.gt;
import static dev.morphia.aggregation.experimental.expressions.ConditionalExpressions.condition;
import static dev.morphia.aggregation.experimental.expressions.ConditionalExpressions.ifNull;
import static dev.morphia.aggregation.experimental.expressions.DateExpressions.dateAdd;
import static dev.morphia.aggregation.experimental.expressions.DateExpressions.dateDiff;
import static dev.morphia.aggregation.experimental.expressions.DateExpressions.dateSubtract;
import static dev.morphia.aggregation.experimental.expressions.DateExpressions.dateTrunc;
import static dev.morphia.aggregation.experimental.expressions.DateExpressions.month;
import static dev.morphia.aggregation.experimental.expressions.DateExpressions.year;
import static dev.morphia.aggregation.experimental.expressions.Expressions.field;
import static dev.morphia.aggregation.experimental.expressions.Expressions.literal;
import static dev.morphia.aggregation.experimental.expressions.Expressions.value;
import static dev.morphia.aggregation.experimental.expressions.MathExpressions.add;
import static dev.morphia.aggregation.experimental.expressions.MathExpressions.trunc;
import static dev.morphia.aggregation.experimental.expressions.ObjectExpressions.mergeObjects;
import static dev.morphia.aggregation.experimental.expressions.SetExpressions.setIntersection;
import static dev.morphia.aggregation.experimental.expressions.SystemVariables.DESCEND;
import static dev.morphia.aggregation.experimental.expressions.SystemVariables.PRUNE;
import static dev.morphia.aggregation.experimental.expressions.TimeUnit.DAY;
import static dev.morphia.aggregation.experimental.expressions.TimeUnit.HOUR;
import static dev.morphia.aggregation.experimental.expressions.TimeUnit.WEEK;
import static dev.morphia.aggregation.experimental.expressions.WindowExpressions.covariancePop;
import static dev.morphia.aggregation.experimental.expressions.WindowExpressions.covarianceSamp;
import static dev.morphia.aggregation.experimental.expressions.WindowExpressions.denseRank;
import static dev.morphia.aggregation.experimental.expressions.WindowExpressions.expMovingAvg;
import static dev.morphia.aggregation.experimental.expressions.WindowExpressions.shift;
import static dev.morphia.aggregation.experimental.stages.Group.group;
import static dev.morphia.aggregation.experimental.stages.Group.id;
import static dev.morphia.aggregation.experimental.stages.Lookup.lookup;
import static dev.morphia.aggregation.experimental.stages.Projection.project;
import static dev.morphia.aggregation.experimental.stages.ReplaceWith.replaceWith;
import static dev.morphia.aggregation.experimental.stages.Set.set;
import static dev.morphia.aggregation.experimental.stages.SetWindowFields.Output.output;
import static dev.morphia.aggregation.experimental.stages.SetWindowFields.setWindowFields;
import static dev.morphia.aggregation.experimental.stages.Sort.sort;
import static dev.morphia.query.experimental.filters.Filters.eq;
import static dev.morphia.query.experimental.filters.Filters.exists;
import static dev.morphia.query.experimental.filters.Filters.expr;
import static dev.morphia.query.experimental.filters.Filters.gt;
import static dev.morphia.query.experimental.filters.Filters.type;
import static java.time.DayOfWeek.MONDAY;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.bson.Document.parse;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

@SuppressWarnings({"unused", "RedundantSuppression"})
public class TestAggregation extends TestBase {
    @Test
    public void testAdd() {
        getMapper().map(Sales.class);
        insert("sales", asList(
            parse("{ '_id' : 1, 'item' : 'abc', 'price' : 10, 'fee' : 2, date: ISODate('2014-03-01T08:00:00Z') }"),
            parse("{ '_id' : 2, 'item' : 'jkl', 'price' : 20, 'fee' : 1, date: ISODate('2014-03-01T09:00:00Z') }"),
            parse("{ '_id' : 3, 'item' : 'xyz', 'price' : 5,  'fee' : 0, date: ISODate('2014-03-15T09:00:00Z') }")));
        Aggregation<Sales> pipeline = getDs()
            .aggregate(Sales.class)
            .project(project()
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

    @Test
    public void testAddFields() {
        List<Document> list = List.of(
            parse("{ _id: 1, student: 'Maya', homework: [ 10, 5, 10 ],quiz: [ 10, 8 ],extraCredit: 0 }"),
            parse("{ _id: 2, student: 'Ryan', homework: [ 5, 6, 5 ],quiz: [ 8, 8 ],extraCredit: 8 }"));

        insert("scores", list);

        List<Document> result = getDs().aggregate(Score.class)
                                       .addFields(AddFields.addFields()
                                                           .field("totalHomework", sum(field("homework")))
                                                           .field("totalQuiz", sum(field("quiz"))))
                                       .addFields(AddFields.addFields()
                                                           .field("totalScore", add(field("totalHomework"),
                                                               field("totalQuiz"), field("extraCredit"))))
                                       .execute(Document.class)
                                       .toList();

        list = List.of(
            parse("{ '_id' : 1, 'student' : 'Maya', 'homework' : [ 10, 5, 10 ],'quiz' : [ 10, 8 ],'extraCredit' : 0, 'totalHomework' : 25,"
                  + " 'totalQuiz' : 18, 'totalScore' : 43 }"),
            parse("{ '_id' : 2, 'student' : 'Ryan', 'homework' : [ 5, 6, 5 ],'quiz' : [ 8, 8 ],'extraCredit' : 8, 'totalHomework' : 16, "
                  + "'totalQuiz' : 16, 'totalScore' : 40 }"));

        assertEquals(result, list);
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

        insert("artwork", list);

        List<Document> results = getDs().aggregate(Artwork.class)
                                        .autoBucket(AutoBucket.autoBucket()
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

        insert("artwork", list);

        List<Document> results = getDs().aggregate(Artwork.class)
                                        .bucket(Bucket.bucket()
                                                      .groupBy(field("price"))
                                                      .boundaries(value(0), value(200), value(400))
                                                      .defaultValue("Other")
                                                      .outputField("count", sum(value(1)))
                                                      .outputField("titles", push().single(field("title"))))
                                        .execute(Document.class)
                                        .toList();

        List<Document> documents = List.of(
            parse("{'_id': 0, 'count': 4, 'titles': ['The Pillars of Society', 'Dancer', 'The Great Wave off Kanagawa', 'Blue Flower']}"),
            parse("{'_id': 200, 'count': 2, 'titles': ['Melancholy III', 'Composition VII']}"),
            parse("{'_id': 'Other', 'count': 2, 'titles': ['The Persistence of Memory', 'The Scream']}"));
        assertEquals(results, documents);
    }

    @Test
    public void testCollation() {
        getDs().save(asList(new User("john doe", LocalDate.now()), new User("John Doe", LocalDate.now())));

        Aggregation<User> pipeline = getDs()
            .aggregate(User.class)
            .match(eq("name", "john doe"));
        assertEquals(count(pipeline.execute(User.class)), 1);

        assertEquals(count(pipeline.execute(User.class,
            new AggregationOptions()
                .collation(Collation.builder()
                                    .locale("en")
                                    .collationStrength(SECONDARY)
                                    .build()))), 2);
    }

    @Test
    public void testCollectionStats() {
        getDs().save(new Author());
        Document stats = getDs().aggregate(Author.class)
                                .collStats(CollectionStats.collStats()
                                                          .histogram(true)
                                                          .scale(42)
                                                          .count(true))
                                .execute(Document.class)
                                .tryNext();
        assertNotNull(stats);
    }

    @Test
    public void testCount() {
        insert("scores", List.of(
            parse("{ '_id' : 1, 'subject' : 'History', 'score' : 88 }"),
            parse("{ '_id' : 2, 'subject' : 'History', 'score' : 92 }"),
            parse("{ '_id' : 3, 'subject' : 'History', 'score' : 97 }"),
            parse("{ '_id' : 4, 'subject' : 'History', 'score' : 71 }"),
            parse("{ '_id' : 5, 'subject' : 'History', 'score' : 79 }"),
            parse("{ '_id' : 6, 'subject' : 'History', 'score' : 83 }")));

        Document scores = getDs().aggregate(Score.class)
                                 .match(gt("score", 80))
                                 .count("passing_scores")
                                 .execute(Document.class)
                                 .next();
        assertEquals(scores, parse("{ \"passing_scores\" : 4 }"));
    }

    @Test
    public void testCovariancePop() {
        checkMinServerVersion(5.0);
        cakeSales();

        List<Document> actual = getDs().aggregate("cakeSales")
                                       .setWindowFields(setWindowFields()
                                           .partitionBy(field("state"))
                                           .sortBy(Sort.ascending("orderDate"))
                                           .output(output("covariancePopForState")
                                               .operator(covariancePop(year(field("orderDate")), field("quantity")))
                                               .window()
                                               .documents("unbounded", "current")))
                                       .execute(Document.class)
                                       .toList();

        List<Document> expected = List.of(
            parse("{ '_id' : 4, 'type' : 'strawberry', 'orderDate' : ISODate('2019-05-18T16:09:01Z'), 'state' : 'CA', 'price' : 41, " +
                  "'quantity' : 162, 'covariancePopForState' : 0.0 }"),
            parse("{ '_id' : 0, 'type' : 'chocolate', 'orderDate' : ISODate('2020-05-18T14:10:30Z'), 'state' : 'CA', 'price' : 13, " +
                  "'quantity' : 120, 'covariancePopForState' : -10.5 }"),
            parse("{ '_id' : 2, 'type' : 'vanilla', 'orderDate' : ISODate('2021-01-11T06:31:15Z'), 'state' : 'CA', 'price' : 12, " +
                  "'quantity' : 145, 'covariancePopForState' : -5.666666666666671 }"),
            parse("{ '_id' : 5, 'type' : 'strawberry', 'orderDate' : ISODate('2019-01-08T06:12:03Z'), 'state' : 'WA', 'price' : 43, " +
                  "'quantity' : 134, 'covariancePopForState' : 0.0 }"),
            parse("{ '_id' : 3, 'type' : 'vanilla', 'orderDate' : ISODate('2020-02-08T13:13:23Z'), 'state' : 'WA', 'price' : 13, " +
                  "'quantity' : 104, 'covariancePopForState' : -7.5 }"),
            parse("{ '_id' : 1, 'type' : 'chocolate', 'orderDate' : ISODate('2021-03-20T11:30:05Z'), 'state' : 'WA', 'price' : 14, " +
                  "'quantity' : 140, 'covariancePopForState' : 2.0 }"));

        assertListEquals(actual, expected);
    }

    @Test
    public void testCovarianceSamp() {
        checkMinServerVersion(5.0);
        cakeSales();

        List<Document> actual = getDs().aggregate("cakeSales")
                                       .setWindowFields(setWindowFields()
                                           .partitionBy(field("state"))
                                           .sortBy(Sort.ascending("orderDate"))
                                           .output(output("covarianceSampForState")
                                               .operator(covarianceSamp(year(field("orderDate")), field("quantity")))
                                               .window()
                                               .documents("unbounded", "current")))
                                       .execute(Document.class)
                                       .toList();

        List<Document> expected = parseDocs(
            "{ '_id' : 4, 'type' : 'strawberry', 'orderDate' : ISODate('2019-05-18T16:09:01Z'), 'state' : 'CA', 'price' : 41, " +
            "'quantity' : 162, 'covarianceSampForState' : null }",
            "{ '_id' : 0, 'type' : 'chocolate', 'orderDate' : ISODate('2020-05-18T14:10:30Z'), 'state' : 'CA', 'price' : 13, " +
            "'quantity' : 120, 'covarianceSampForState' : -21.0 }",
            "{ '_id' : 2, 'type' : 'vanilla', 'orderDate' : ISODate('2021-01-11T06:31:15Z'), 'state' : 'CA', 'price' : 12, " +
            "'quantity' : 145, 'covarianceSampForState' : -8.500000000000007 }",
            "{ '_id' : 5, 'type' : 'strawberry', 'orderDate' : ISODate('2019-01-08T06:12:03Z'), 'state' : 'WA', 'price' : 43, " +
            "'quantity' : 134, 'covarianceSampForState' : null }",
            "{ '_id' : 3, 'type' : 'vanilla', 'orderDate' : ISODate('2020-02-08T13:13:23Z'), 'state' : 'WA', 'price' : 13, " +
            "'quantity' : 104, 'covarianceSampForState' : -15.0 }",
            "{ '_id' : 1, 'type' : 'chocolate', 'orderDate' : ISODate('2021-03-20T11:30:05Z'), 'state' : 'WA', 'price' : 14, " +
            "'quantity' : 140, 'covarianceSampForState' : 3.0 }");

        assertListEquals(actual, expected);
    }

    @Test
    public void testDateAdd() {
        checkMinServerVersion(5.0);

        insert("shipping", parseDocs(
            "{ '_id' : ObjectId('603dd4b2044b995ad331c0b2'), custId: 456, purchaseDate: ISODate('2020-12-31') }",
            "{ '_id' : ObjectId('603dd4b2044b995ad331c0b3'), custId: 457, purchaseDate: ISODate('2021-02-28') }",
            "{ '_id' : ObjectId('603dd4b2044b995ad331c0b4'), custId: 458, purchaseDate: ISODate('2021-02-26') }"));

        getDs().aggregate("shipping")
               .project(project()
                   .include("expectedDeliveryDate", dateAdd(field("purchaseDate"), 3, DAY)))
               .merge(Merge.into("shipping"));

        List<Document> actual = getDatabase().getCollection("shipping").find().into(new ArrayList<>());
        List<Document> expected = parseDocs(
            "{ '_id' : ObjectId('603dd4b2044b995ad331c0b2'), 'custId' : 456, 'purchaseDate' : ISODate('2020-12-31T00:00:00Z'), " +
            "'expectedDeliveryDate' : ISODate('2021-01-03T00:00:00Z') }",
            "{ '_id' : ObjectId('603dd4b2044b995ad331c0b3'), 'custId' : 457, 'purchaseDate' : ISODate('2021-02-28T00:00:00Z'), " +
            "'expectedDeliveryDate' : ISODate('2021-03-03T00:00:00Z') }",
            "{ '_id' : ObjectId('603dd4b2044b995ad331c0b4'), 'custId' : 458, 'purchaseDate' : ISODate('2021-02-26T00:00:00Z'), " +
            "'expectedDeliveryDate' : ISODate('2021-03-01T00:00:00Z') }");

        assertListEquals(actual, expected);
    }

    @Test
    public void testDateDiff() {
        checkMinServerVersion(5.0);

        insert("orders", parseDocs(
            "{ _id: 1, custId: 456, purchased: ISODate('2020-12-31'), delivered: ISODate('2021-01-05') }",
            "{ _id: 2, custId: 457, purchased: ISODate('2021-02-28'), delivered: ISODate('2021-03-07') }",
            "{ _id: 3, custId: 458, purchased: ISODate('2021-02-16'), delivered: ISODate('2021-02-18') }"));

        Document actual = getDs().aggregate("orders")
                                 .group(group()
                                     .field("averageTime", avg(dateDiff(field("purchased"), field("delivered"), DAY))))
                                 .project(project()
                                     .suppressId()
                                     .include("numDays", trunc(field("averageTime"), literal(1))))
                                 .execute(Document.class)
                                 .next();

        Document expected = new Document("numDays", 4.6D);

        assertEquals(actual, expected);
    }

    @Test
    public void testDateSubtract() {
        checkMinServerVersion(5.0);

        insert("connectionTime", parseDocs(
            "{ _id: 1, custId: 457, login: ISODate('2020-12-25T19:04:00Z'), logout: ISODate('2020-12-28T09:04:00Z')}",
            "{ _id: 2, custId: 457, login: ISODate('2021-01-27T05:12:00Z'), logout: ISODate('2021-01-28T13:05:00Z') }",
            "{ _id: 3, custId: 458, login: ISODate('2021-01-22T06:27:00Z'), logout: ISODate('2021-01-31T11:00:00Z') }",
            "{ _id: 4, custId: 459, login: ISODate('2021-02-14T20:14:00Z'), logout: ISODate('2021-02-17T16:05:00Z') }",
            "{ _id: 5, custId: 460, login: ISODate('2021-02-26T02:44:00Z'), logout: ISODate('2021-02-18T14:13:00Z') }"));

        getDs().aggregate("connectionTime")
               .match(
                   expr(ComparisonExpressions.eq(year(field("logout")), literal(2021))),
                   expr(ComparisonExpressions.eq(month(field("logout")), literal(1))))
               .project(project()
                   .include("logoutTime", dateSubtract(field("logout"), 3, HOUR)))
               .merge(Merge.into("connectionTime"));


        List<Document> actual = getDatabase().getCollection("connectionTime").find().into(new ArrayList<>());
        List<Document> expected =
            parseDocs(
                "{ '_id' : 1, 'custId' : 457, 'login' : ISODate('2020-12-25T19:04:00Z'), 'logout' : ISODate('2020-12-28T09:04:00Z')}",
                "{ '_id' : 2, 'custId' : 457, 'login' : ISODate('2021-01-27T05:12:00Z'), 'logout' : ISODate('2021-01-28T13:05:00Z'), " +
                "'logoutTime' : ISODate('2021-01-28T10:05:00Z') }",
                "{ '_id' : 3, 'custId' : 458, 'login' : ISODate('2021-01-22T06:27:00Z'), 'logout' : ISODate('2021-01-31T11:00:00Z'), " +
                "'logoutTime' : ISODate('2021-01-31T08:00:00Z') }",
                "{ '_id' : 4, 'custId' : 459, 'login' : ISODate('2021-02-14T20:14:00Z'), 'logout' : ISODate('2021-02-17T16:05:00Z') }",
                "{ '_id' : 5, 'custId' : 460, 'login' : ISODate('2021-02-26T02:44:00Z'), 'logout' : ISODate('2021-02-18T14:13:00Z') }");

        assertListEquals(actual, expected);
    }

    @Test
    public void testDateTrunc() {
        checkMinServerVersion(5.0);

        cakeSales();

        List<Document> actual = getDs().aggregate("cakeSales")
                                       .project(project()
                                           .include("orderDate")
                                           .include("truncatedOrderDate", dateTrunc(field("orderDate"), WEEK)
                                               .binSize(2)
                                               .timezone(value("America/Los_Angeles"))
                                               .startOfWeek(MONDAY)))
                                       .execute(Document.class)
                                       .toList();


        List<Document> expected = parseDocs(
            "{ _id: 0, orderDate: ISODate('2020-05-18T14:10:30.000Z'), truncatedOrderDate: ISODate('2020-05-11T07:00:00.000Z') }",
            "{ _id: 1, orderDate: ISODate('2021-03-20T11:30:05.000Z'), truncatedOrderDate: ISODate('2021-03-15T07:00:00.000Z') }",
            "{ _id: 2, orderDate: ISODate('2021-01-11T06:31:15.000Z'), truncatedOrderDate: ISODate('2021-01-04T08:00:00.000Z') }",
            "{ _id: 3, orderDate: ISODate('2020-02-08T13:13:23.000Z'), truncatedOrderDate: ISODate('2020-02-03T08:00:00.000Z') }",
            "{ _id: 4, orderDate: ISODate('2019-05-18T16:09:01.000Z'), truncatedOrderDate: ISODate('2019-05-13T07:00:00.000Z') }",
            "{ _id: 5, orderDate: ISODate('2019-01-08T06:12:03.000Z'), truncatedOrderDate: ISODate('2019-01-07T08:00:00.000Z') }");

        assertListEquals(actual, expected);
    }

    @Test
    public void testDenseRank() {
        checkMinServerVersion(5.0);

        cakeSales();

        List<Document> actual = getDs().aggregate("cakeSales")
                                       .setWindowFields(setWindowFields()
                                           .partitionBy(field("state"))
                                           .sortBy(Sort.descending("quantity"))
                                           .output(output("denseRankQuantityForState")
                                               .operator(denseRank())))
                                       .execute(Document.class)
                                       .toList();

        List<Document> expected = parseDocs(
            "{ '_id' : 4, 'type' : 'strawberry', 'orderDate' : ISODate('2019-05-18T16:09:01Z'), 'state' : 'CA', 'price' : 41, 'quantity' " +
            ": 162, 'denseRankQuantityForState' : 1 }",
            "{ '_id' : 2, 'type' : 'vanilla', 'orderDate' : ISODate('2021-01-11T06:31:15Z'), 'state' : 'CA', 'price' : 12, 'quantity' : " +
            "145, 'denseRankQuantityForState' : 2 }",
            "{ '_id' : 0, 'type' : 'chocolate', 'orderDate' : ISODate('2020-05-18T14:10:30Z'), 'state' : 'CA', 'price' : 13, 'quantity' :" +
            " 120, 'denseRankQuantityForState' : 3 }",
            "{ '_id' : 1, 'type' : 'chocolate', 'orderDate' : ISODate('2021-03-20T11:30:05Z'), 'state' : 'WA', 'price' : 14, 'quantity' :" +
            " 140, 'denseRankQuantityForState' : 1 }",
            "{ '_id' : 5, 'type' : 'strawberry', 'orderDate' : ISODate('2019-01-08T06:12:03Z'), 'state' : 'WA', 'price' : 43, 'quantity' " +
            ": 134, 'denseRankQuantityForState' : 2 }",
            "{ '_id' : 3, 'type' : 'vanilla', 'orderDate' : ISODate('2020-02-08T13:13:23Z'), 'state' : 'WA', 'price' : 13, 'quantity' : " +
            "104, 'denseRankQuantityForState' : 3 }");
        assertListEquals(actual, expected);
    }

    @Test
    public void testExpMovingAverage() {
        checkMinServerVersion(5.0);

        insert("stockPrices", parseDocs(
            "{_id: ObjectId('60d11fef833dfeadc8e6286b'), stock: 'ABC', date: ISODate('2020-05-18T20:00:00Z'), price: 13.0 }",
            "{_id: ObjectId('60d11fef833dfeadc8e6286c'), stock: 'ABC', date: ISODate('2020-05-19T20:00:00Z'), price: 15.4 }",
            "{_id: ObjectId('60d11fef833dfeadc8e6286d'), stock: 'ABC', date: ISODate('2020-05-20T20:00:00Z'), price: 12.0 }",
            "{_id: ObjectId('60d11fef833dfeadc8e6286e'), stock: 'ABC', date: ISODate('2020-05-21T20:00:00Z'), price: 11.7 }",
            "{_id: ObjectId('60d11fef833dfeadc8e6286f'), stock: 'DEF', date: ISODate('2020-05-18T20:00:00Z'), price: 82.0 }",
            "{_id: ObjectId('60d11fef833dfeadc8e62870'), stock: 'DEF', date: ISODate('2020-05-19T20:00:00Z'), price: 94.0 }",
            "{_id: ObjectId('60d11fef833dfeadc8e62871'), stock: 'DEF', date: ISODate('2020-05-20T20:00:00Z'), price: 112.0 }",
            "{_id: ObjectId('60d11fef833dfeadc8e62872'), stock: 'DEF', date: ISODate('2020-05-21T20:00:00Z'), price: 97.3 }"));

        List<Document> actual = getDs().aggregate("stockPrices")
                                       .setWindowFields(setWindowFields()
                                           .partitionBy(field("stock"))
                                           .sortBy(Sort.ascending("date"))
                                           .output(output("expMovingAvgForStock")
                                               .operator(expMovingAvg(field("price"), 2))))
                                       .execute(Document.class)
                                       .toList();
        List<Document> expected = parseDocs(
            "{ '_id' : ObjectId('60d11fef833dfeadc8e6286b'), 'stock' : 'ABC', 'date' : ISODate('2020-05-18T20:00:00Z'), 'price'" +
            " : 13.0, 'expMovingAvgForStock' : 13.0 }",
            "{ '_id' : ObjectId('60d11fef833dfeadc8e6286c'), 'stock' : 'ABC', 'date' : ISODate('2020-05-19T20:00:00Z'), 'price'" +
            " : 15.4, 'expMovingAvgForStock' : 14.6 }",
            "{ '_id' : ObjectId('60d11fef833dfeadc8e6286d'), 'stock' : 'ABC', 'date' : ISODate('2020-05-20T20:00:00Z'), 'price'" +
            " : 12.0, 'expMovingAvgForStock' : 12.866666666666667 }",
            "{ '_id' : ObjectId('60d11fef833dfeadc8e6286e'), 'stock' : 'ABC', 'date' : ISODate('2020-05-21T20:00:00Z'), 'price'" +
            " : 11.7, 'expMovingAvgForStock' : 12.088888888888889 }",
            "{ '_id' : ObjectId('60d11fef833dfeadc8e6286f'), 'stock' : 'DEF', 'date' : ISODate('2020-05-18T20:00:00Z'), 'price'" +
            " : 82.0, 'expMovingAvgForStock' : 82.0 }",
            "{ '_id' : ObjectId('60d11fef833dfeadc8e62870'), 'stock' : 'DEF', 'date' : ISODate('2020-05-19T20:00:00Z'), 'price'" +
            " : 94.0, 'expMovingAvgForStock' : 90.0 }",
            "{ '_id' : ObjectId('60d11fef833dfeadc8e62871'), 'stock' : 'DEF', 'date' : ISODate('2020-05-20T20:00:00Z'), 'price'" +
            " : 112.0, 'expMovingAvgForStock' : 104.66666666666667 }",
            "{ '_id' : ObjectId('60d11fef833dfeadc8e62872'), 'stock' : 'DEF', 'date' : ISODate('2020-05-21T20:00:00Z'), 'price'" +
            " : 97.3, 'expMovingAvgForStock' : 99.75555555555556 }");

        assertListEquals(actual, expected);
    }

    @Test
    public void testFacet() {

        insert("artwork", parseDocs(
            "{'_id': 1, 'title': 'The Pillars of Society', 'artist': 'Grosz', 'year': 1926, 'price': NumberDecimal('199.99'),"
            + " 'tags': [ 'painting', 'satire', 'Expressionism', 'caricature' ] }",
            "{'_id': 2, 'title': 'Melancholy III', 'artist': 'Munch', 'year': 1902, 'price': NumberDecimal('280.00'),"
            + " 'tags': [ 'woodcut', 'Expressionism' ] }",
            "{'_id': 3, 'title': 'Dancer', 'artist': 'Miro', 'year': 1925, 'price': NumberDecimal('76.04'),"
            + " 'tags': [ 'oil', 'Surrealism', 'painting' ] }",
            "{'_id': 4, 'title': 'The Great Wave off Kanagawa', 'artist': 'Hokusai', 'price': NumberDecimal('167.30'),"
            + " 'tags': [ 'woodblock', 'ukiyo-e' ] }",
            "{'_id': 5, 'title': 'The Persistence of Memory', 'artist': 'Dali', 'year': 1931, 'price': NumberDecimal('483.00'),"
            + " 'tags': [ 'Surrealism', 'painting', 'oil' ] }",
            "{'_id': 6, 'title': 'Composition VII', 'artist': 'Kandinsky', 'year': 1913, 'price': NumberDecimal('385.00'), "
            + "'tags': [ 'oil', 'painting', 'abstract' ] }",
            "{'_id': 7, 'title': 'The Scream', 'artist': 'Munch', 'year': 1893, 'tags': [ 'Expressionism', 'painting', 'oil' ] }",
            "{'_id': 8, 'title': 'Blue Flower', 'artist': 'O\\'Keefe', 'year': 1918, 'price': NumberDecimal('118.42'),"
            + " 'tags': [ 'abstract', 'painting' ] }"));

        Document result = getDs().aggregate(Artwork.class)
                                 .facet(Facet.facet()
                                             .field("categorizedByTags",
                                                 Unwind.unwind("tags"),
                                                 SortByCount.sortByCount(field("tags")))
                                             .field("categorizedByPrice",
                                                 Match.match(exists("price")),
                                                 Bucket.bucket()
                                                       .groupBy(field("price"))
                                                       .boundaries(value(0), value(150), value(200), value(300), value(400))
                                                       .defaultValue("Other")
                                                       .outputField("count", sum(value(1)))
                                                       .outputField("titles", push().single(field("title"))))
                                             .field("categorizedByYears(Auto)", AutoBucket.autoBucket()
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

        assertDocumentEquals(result, document);
    }

    @Test
    public void testGraphLookup() {
        List<Document> list = parseDocs("{ '_id' : 1, 'name' : 'Dev' }",
            "{ '_id' : 2, 'name' : 'Eliot', 'reportsTo' : 'Dev' }",
            "{ '_id' : 3, 'name' : 'Ron', 'reportsTo' : 'Eliot' }",
            "{ '_id' : 4, 'name' : 'Andrew', 'reportsTo' : 'Eliot' }",
            "{ '_id' : 5, 'name' : 'Asya', 'reportsTo' : 'Ron' }",
            "{ '_id' : 6, 'name' : 'Dan', 'reportsTo' : 'Andrew' }");

        insert("employees", list);

        List<Document> actual = getDs().aggregate(Employee.class)
                                       .graphLookup(GraphLookup.graphLookup("employees")
                                                               .startWith(field("reportsTo"))
                                                               .connectFromField("reportsTo")
                                                               .connectToField("name")
                                                               .as("reportingHierarchy"))
                                       .execute(Document.class)
                                       .toList();

        List<Document> expected = parseDocs("{'_id': 1, 'name': 'Dev', 'reportingHierarchy': []}",
            "{'_id': 2, 'name': 'Eliot', 'reportsTo': 'Dev', 'reportingHierarchy': [{'_id': 1, 'name': 'Dev'}]}",
            "{'_id': 3, 'name': 'Ron', 'reportsTo': 'Eliot', 'reportingHierarchy': [{'_id': 1, 'name': 'Dev'},{'_id': 2, 'name': "
            + "'Eliot', 'reportsTo': 'Dev'}]}",
            "{'_id': 4, 'name': 'Andrew', 'reportsTo': 'Eliot', 'reportingHierarchy': [{'_id': 1, 'name': 'Dev'},{'_id': 2, 'name': "
            + "'Eliot', 'reportsTo': 'Dev'}]}",
            "{'_id': 5, 'name': 'Asya', 'reportsTo': 'Ron', 'reportingHierarchy': [{'_id': 1, 'name': 'Dev'},{'_id': 2, 'name': "
            + "'Eliot', 'reportsTo': 'Dev'},{'_id': 3, 'name': 'Ron', 'reportsTo': 'Eliot'}]}",
            "{'_id': 6, 'name': 'Dan', 'reportsTo': 'Andrew', 'reportingHierarchy': [{'_id': 1, 'name': 'Dev'},{'_id': 2, 'name': "
            + "'Eliot', 'reportsTo': 'Dev'},{'_id': 4, 'name': 'Andrew', 'reportsTo': 'Eliot'}]}");

        assertDocumentEquals(actual, expected);
    }

    @Test
    public void testIndexStats() {
        getDs().getMapper().map(Author.class);
        getDs().ensureIndexes();
        Document stats = getDs().aggregate(Author.class)
                                .indexStats()
                                .match(eq("name", "books_1"))
                                .execute(Document.class)
                                .next();

        assertNotNull(stats);
    }

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
                                     .lookup(lookup(Inventory.class)
                                         .localField("item")
                                         .foreignField("sku")
                                         .as("inventoryDocs"))
                                     .sort(sort().ascending("_id"))
                                     .execute(Order.class)
                                     .toList();
        assertEquals(lookups.get(0).getInventoryDocs().get(0), inventories.get(0));
        assertEquals(lookups.get(1).getInventoryDocs().get(0), inventories.get(3));
        assertEquals(lookups.get(2).getInventoryDocs().get(0), inventories.get(4));
        assertEquals(lookups.get(2).getInventoryDocs().get(1), inventories.get(5));
    }

    @Test
    public void testLookupWithPipeline() {
        // Test data pulled from https://docs.mongodb.com/v3.2/reference/operator/aggregation/lookup/
        insert("orders", parseDocs("{ '_id' : 1, 'item' : 'almonds', 'price' : 12, 'ordered' : 2 }",
            "{ '_id' : 2, 'item' : 'pecans', 'price' : 20, 'ordered' : 1 }",
            "{ '_id' : 3, 'item' : 'cookies', 'price' : 10, 'ordered' : 60 }"));

        insert("warehouses", parseDocs("{ '_id' : 1, 'stock_item' : 'almonds', warehouse: 'A', 'instock' : 120 },",
            "{ '_id' : 2, 'stock_item' : 'pecans', warehouse: 'A', 'instock' : 80 }",
            "{ '_id' : 3, 'stock_item' : 'almonds', warehouse: 'B', 'instock' : 60 }",
            "{ '_id' : 4, 'stock_item' : 'cookies', warehouse: 'B', 'instock' : 40 }",
            "{ '_id' : 5, 'stock_item' : 'cookies', warehouse: 'A', 'instock' : 80 }"));

        List<Document> actual = getDs().aggregate("orders")
                                       .lookup(Lookup.lookup("warehouses")
                                                     .let("order_item", field("item"))
                                                     .let("order_qty", field("ordered"))
                                                     .as("stockdata")
                                                     .pipeline(
                                                         Match.match(
                                                             expr(
                                                                 Expressions.of().field(
                                                                     "$and",
                                                                     array(Expressions.of()
                                                                                      .field("$eq",
                                                                                          array(field("stock"), field("$order_item"))),
                                                                         Expressions.of()
                                                                                    .field("$gte",
                                                                                        array(field("instock"), field("$order_qty"))))

                                                                                       ))),
                                                         project()
                                                             .exclude("stock_item")
                                                             .exclude("_id")))
                                       .execute(Document.class, new AggregationOptions().readConcern(ReadConcern.LOCAL))
                                       .toList();

        List<Document> expected = parseDocs(
            "{ '_id' : 1, 'item' : 'almonds', 'price' : 12, 'ordered' : 2, 'stockdata' : [ { 'warehouse' : 'A', 'instock'" +
            " : 120 }, { 'warehouse' : 'B', 'instock' : 60 } ] }",
            "{ '_id' : 2, 'item' : 'pecans', 'price' : 20, 'ordered' : 1, 'stockdata' : [ { 'warehouse' : 'A', 'instock' : 80 } ] }",
            "{ '_id' : 3, 'item' : 'cookies', 'price' : 10, 'ordered' : 60, 'stockdata' : [ { 'warehouse' : 'A', 'instock' : 80 } ] }");

        assertDocumentEquals(actual, expected);
    }

    @Test
    public void testMerge() {
        checkMinServerVersion(4.2);

        insert("salaries", parseDocs(
            "{ '_id' : 1, employee: 'Ant', dept: 'A', salary: 100000, fiscal_year: 2017 }",
            "{ '_id' : 2, employee: 'Bee', dept: 'A', salary: 120000, fiscal_year: 2017 }",
            "{ '_id' : 3, employee: 'Cat', dept: 'Z', salary: 115000, fiscal_year: 2017 }",
            "{ '_id' : 4, employee: 'Ant', dept: 'A', salary: 115000, fiscal_year: 2018 }",
            "{ '_id' : 5, employee: 'Bee', dept: 'Z', salary: 145000, fiscal_year: 2018 }",
            "{ '_id' : 6, employee: 'Cat', dept: 'Z', salary: 135000, fiscal_year: 2018 }",
            "{ '_id' : 7, employee: 'Gecko', dept: 'A', salary: 100000, fiscal_year: 2018 }",
            "{ '_id' : 8, employee: 'Ant', dept: 'A', salary: 125000, fiscal_year: 2019 }",
            "{ '_id' : 9, employee: 'Bee', dept: 'Z', salary: 160000, fiscal_year: 2019 }",
            "{ '_id' : 10, employee: 'Cat', dept: 'Z', salary: 150000, fiscal_year: 2019 }"));

        getDs().aggregate(Salary.class)
               .group(Group.group(id()
                               .field("fiscal_year")
                               .field("dept"))
                           .field("salaries", sum(field("salary"))))
               .merge(Merge.into("budgets")
                           .on("_id")
                           .whenMatched(WhenMatched.REPLACE)
                           .whenNotMatched(WhenNotMatched.INSERT));
        List<Document> actual = getDs().find("budgets", Document.class).iterator().toList();

        List<Document> expected = parseDocs(
            "{ '_id' : { 'fiscal_year' : 2017, 'dept' : 'A' }, 'salaries' : 220000 }",
            "{ '_id' : { 'fiscal_year' : 2017, 'dept' : 'Z' }, 'salaries' : 115000 }",
            "{ '_id' : { 'fiscal_year' : 2018, 'dept' : 'A' }, 'salaries' : 215000 }",
            "{ '_id' : { 'fiscal_year' : 2018, 'dept' : 'Z' }, 'salaries' : 280000 }",
            "{ '_id' : { 'fiscal_year' : 2019, 'dept' : 'A' }, 'salaries' : 125000 }",
            "{ '_id' : { 'fiscal_year' : 2019, 'dept' : 'Z' }, 'salaries' : 310000 }");

        assertDocumentEquals(actual, expected);
    }

    @Test
    public void testNullGroupId() {
        getDs().save(asList(new User("John", LocalDate.now()),
            new User("Paul", LocalDate.now()),
            new User("George", LocalDate.now()),
            new User("Ringo", LocalDate.now())));
        Aggregation<User> pipeline = getDs()
            .aggregate(User.class)
            .group(Group.group()
                        .field("count", sum(value(1))));

        assertEquals(pipeline.execute(Document.class).next().getInteger("count"), Integer.valueOf(4));
    }

    @Test
    public void testOut() {
        getDs().save(asList(new Book("The Banquet", "Dante", 2),
            new Book("Divine Comedy", "Dante", 1),
            new Book("Eclogues", "Dante", 2),
            new Book("The Odyssey", "Homer", 10),
            new Book("Iliad", "Homer", 10)));

        getDs().aggregate(Book.class)
               .group(Group.group(id("author"))
                           .field("books", push()
                               .single(field("title"))))
               .out(Out.to(Author.class));
        assertEquals(getDs().getCollection(Author.class).countDocuments(), 2);

        getDs().aggregate(Book.class)
               .group(Group.group(id("author"))
                           .field("books", push()
                               .single(field("title"))))
               .out(Out.to("different"));
        assertEquals(getDatabase().getCollection("different").countDocuments(), 2);
    }

    @Test
    public void testPlanCacheStats() {
        checkMinServerVersion(4.2);
        List<Document> list = parseDocs(
            "{ '_id' : 1, 'item' : 'abc', 'price' : NumberDecimal('12'), 'quantity' : 2, 'type': 'apparel' }",
            "{ '_id' : 2, 'item' : 'jkl', 'price' : NumberDecimal('20'), 'quantity' : 1, 'type': 'electronics' }",
            "{ '_id' : 3, 'item' : 'abc', 'price' : NumberDecimal('10'), 'quantity' : 5, 'type': 'apparel' }",
            "{ '_id' : 4, 'item' : 'abc', 'price' : NumberDecimal('8'), 'quantity' : 10, 'type': 'apparel' }",
            "{ '_id' : 5, 'item' : 'jkl', 'price' : NumberDecimal('15'), 'quantity' : 15, 'type': 'electronics' }");

        MongoCollection<Document> orders = getDatabase().getCollection("orders");
        insert("orders", list);

        assertNotNull(orders.createIndex(new Document("item", 1)));
        assertNotNull(orders.createIndex(new Document("item", 1)
            .append("quantity", 1)));
        assertNotNull(orders.createIndex(new Document("item", 1)
                .append("price", 1),
            new IndexOptions()
                .partialFilterExpression(new Document("price", new Document("$gte", 10)))));
        assertNotNull(orders.createIndex(new Document("quantity", 1)));
        assertNotNull(orders.createIndex(new Document("quantity", 1)
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

        assertNotNull(stats);
    }

    @Test
    public void testProjection() {

        insert("books", List.of(
            parse("{'_id' : 1, title: 'abc123', isbn: '0001122223334', author: { last: 'zzz', first: 'aaa' }, copies: 5,\n"
                  + "  lastModified: '2016-07-28'}")));
        Aggregation<Book> pipeline = getDs().aggregate(Book.class)
                                            .project(project()
                                                .include("title")
                                                .include("author"));
        MorphiaCursor<ProjectedBook> aggregate = pipeline.execute(ProjectedBook.class);
        assertEquals(aggregate.next(), new ProjectedBook(1, "abc123", "zzz", "aaa"));

        pipeline = getDs().aggregate(Book.class)
                          .project(project()
                              .suppressId()
                              .include("title")
                              .include("author"));
        aggregate = pipeline.execute(ProjectedBook.class);

        assertEquals(aggregate.next(), new ProjectedBook(null, "abc123", "zzz", "aaa"));

        pipeline = getDs().aggregate(Book.class)
                          .project(project()
                              .exclude("lastModified"));
        final MorphiaCursor<Document> docAgg = pipeline.execute(Document.class);

        assertEquals(docAgg.next(),
            parse("{'_id' : 1, title: 'abc123', isbn: '0001122223334', author: { last: 'zzz', first: 'aaa' }, copies: 5}"));
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
                                 .match(eq("year", 2014))
                                 .redact(Redact.redact(
                                     condition(
                                         gt(size(setIntersection(field("tags"), array(value("STLW"), value("G")))),
                                             value(0)),
                                         DESCEND, PRUNE)))
                                 .execute(Document.class)
                                 .next();
        Document expected = parse("{ '_id' : 1, 'title' : '123 Department Report', 'tags' : [ 'G', 'STLW' ],'year' : 2014, 'subsections' :"
                                  + " [{ 'subtitle' : 'Section 1: Overview', 'tags' : [ 'SI', 'G' ],'content' : 'Section 1: This is the "
                                  + "content of section 1.' },{ 'subtitle' : 'Section 2: Analysis', 'tags' : [ 'STLW' ],'content' : "
                                  + "'Section 2: This is the content of section 2.' }]}");

        assertEquals(expected, actual);
    }

    @Test
    public void testReplaceRoot() {
        List<Document> documents = parseDocs(
            "{'_id': 1, 'name': {'first': 'John', 'last': 'Backus'}}",
            "{'_id': 2, 'name': {'first': 'John', 'last': 'McCarthy'}}",
            "{'_id': 3, 'name': {'first': 'Grace', 'last': 'Hopper'}}",
            "{'_id': 4, 'firstname': 'Ole-Johan', 'lastname': 'Dahl'}");

        insert("authors", documents);

        List<Document> actual = getDs().aggregate(Author.class)
                                       .match(exists("name"),
                                           type("name", Type.ARRAY).not(),
                                           type("name", Type.OBJECT))
                                       .replaceRoot(ReplaceRoot.replaceRoot(field("name")))
                                       .execute(Document.class)
                                       .toList();
        List<Document> expected = documents.subList(0, 3)
                                           .stream()
                                           .map(d -> (Document) d.get("name"))
                                           .collect(toList());
        assertDocumentEquals(actual, expected);

        actual = getDs().aggregate(Author.class)
                        .replaceRoot(ReplaceRoot.replaceRoot(ifNull().target(field("name"))
                                                                     .field("_id", field("_id"))
                                                                     .field("missingName", value(true))))
                        .execute(Document.class)
                        .toList();
        expected = documents.subList(0, 3)
                            .stream()
                            .map(d -> (Document) d.get("name"))
                            .collect(toList());
        expected.add(new Document("_id", 4)
            .append("missingName", true));
        assertDocumentEquals(actual, expected);

        actual = getDs().aggregate(Author.class)
                        .replaceRoot(ReplaceRoot.replaceRoot(mergeObjects()
                            .add(Expressions.of()
                                            .field("_id", field("_id"))
                                            .field("first", value(""))
                                            .field("last", value("")))
                            .add(field("name"))))
                        .execute(Document.class)
                        .toList();
        expected = documents.subList(0, 3)
                            .stream()
                            .peek(d -> d.putAll((Document) d.remove("name")))
                            .collect(toList());
        expected.add(new Document("_id", 4)
            .append("first", "")
            .append("last", ""));
        assertDocumentEquals(actual, expected);
    }

    @Test
    public void testReplaceWith() {
        checkMinServerVersion(4.2);
        List<Document> documents = parseDocs(
            "{'_id': 1, 'name': {'first': 'John', 'last': 'Backus'}}",
            "{'_id': 2, 'name': {'first': 'John', 'last': 'McCarthy'}}",
            "{'_id': 3, 'name': {'first': 'Grace', 'last': 'Hopper'}}",
            "{'_id': 4, 'firstname': 'Ole-Johan', 'lastname': 'Dahl'}");

        insert("authors", documents);

        List<Document> actual = getDs().aggregate(Author.class)
                                       .match(exists("name"),
                                           type("name", Type.ARRAY).not(),
                                           type("name", Type.OBJECT))
                                       .replaceWith(replaceWith(field("name")))
                                       .execute(Document.class)
                                       .toList();
        List<Document> expected = documents.subList(0, 3)
                                           .stream()
                                           .map(d -> (Document) d.get("name"))
                                           .collect(toList());
        assertDocumentEquals(actual, expected);

        actual = getDs().aggregate(Author.class)
                        .replaceWith(replaceWith(ifNull().target(field("name"))
                                                         .field("_id", field("_id"))
                                                         .field("missingName", value(true))))
                        .execute(Document.class)
                        .toList();
        expected = documents.subList(0, 3)
                            .stream()
                            .map(d -> (Document) d.get("name"))
                            .collect(toList());
        expected.add(new Document("_id", 4)
            .append("missingName", true));
        assertDocumentEquals(actual, expected);

        actual = getDs().aggregate(Author.class)
                        .replaceWith(replaceWith(mergeObjects()
                            .add(Expressions.of()
                                            .field("_id", field("_id"))
                                            .field("first", value(""))
                                            .field("last", value("")))
                            .add(field("name"))))
                        .execute(Document.class)
                        .toList();
        expected = documents.subList(0, 3)
                            .stream()
                            .peek(d -> d.putAll((Document) d.remove("name")))
                            .collect(toList());
        expected.add(new Document("_id", 4)
            .append("first", "")
            .append("last", ""));
        assertDocumentEquals(actual, expected);
    }

    @Test
    public void testResultTypes() {
        getMapper().map(Martian.class);

        Martian martian = new Martian();
        martian.name = "Marvin";
        getDs().save(martian);

        List<Human> execute = getDs().aggregate(Martian.class)
                                     .limit(1)
                                     .execute(Human.class)
                                     .toList();
        Human human = execute.get(0);
        assertEquals(human.id, martian.id);
        assertEquals(human.name, martian.name);
    }

    @Test
    public void testSample() {
        getDs().save(asList(new User("John", LocalDate.now()),
            new User("Paul", LocalDate.now()),
            new User("George", LocalDate.now()),
            new User("Ringo", LocalDate.now())));
        Aggregation<User> pipeline = getDs()
            .aggregate(User.class)
            .sample(3);

        assertEquals(pipeline.execute(User.class).toList().size(), 3);
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testSet() {
        checkMinServerVersion(4.2);
        List<Document> list = parseDocs(
            "{ _id: 1, student: 'Maya', homework: [ 10, 5, 10 ],quiz: [ 10, 8 ],extraCredit: 0 }",
            "{ _id: 2, student: 'Ryan', homework: [ 5, 6, 5 ],quiz: [ 8, 8 ],extraCredit: 8 }");

        insert("scores", list);

        List<Document> result = getDs().aggregate(Score.class)
                                       .set(AddFields.addFields()
                                                     .field("totalHomework", sum(field("homework")))
                                                     .field("totalQuiz", sum(field("quiz"))))
                                       .set(set()
                                           .field("totalScore", add(field("totalHomework"),
                                               field("totalQuiz"), field("extraCredit"))))
                                       .execute(Document.class)
                                       .toList();

        list = parseDocs(
            "{ '_id' : 1, 'student' : 'Maya', 'homework' : [ 10, 5, 10 ],'quiz' : [ 10, 8 ],'extraCredit' : 0, 'totalHomework' : 25,"
            + " 'totalQuiz' : 18, 'totalScore' : 43 }",
            "{ '_id' : 2, 'student' : 'Ryan', 'homework' : [ 5, 6, 5 ],'quiz' : [ 8, 8 ],'extraCredit' : 8, 'totalHomework' : 16, "
            + "'totalQuiz' : 16, 'totalScore' : 40 }");

        assertEquals(result, list);
    }

    @Test
    public void testSetWindowFields() {
        checkMinServerVersion(5.0);
        cakeSales();

        List<Document> actual = getDs().aggregate("cakeSales")
                                       .setWindowFields(setWindowFields()
                                           .partitionBy(field("state"))
                                           .sortBy(Sort.ascending("orderDate"))
                                           .output(output("cumulativeQuantityForState")
                                               .operator(sum(field("quantity")))
                                               .window()
                                               .documents("unbounded", "current")))
                                       .execute(Document.class)
                                       .toList();

        List<Document> expected = parseDocs(
            "{ '_id' : 4, 'type' : 'strawberry', 'orderDate' : ISODate('2019-05-18T16:09:01Z'), 'state' : 'CA', 'price' : 41, " +
            "'quantity' : 162, 'cumulativeQuantityForState' : 162 }",
            "{ '_id' : 0, 'type' : 'chocolate', 'orderDate' : ISODate('2020-05-18T14:10:30Z'), 'state' : 'CA', 'price' : 13, " +
            "'quantity' : 120, 'cumulativeQuantityForState' : 282 }",
            "{ '_id' : 2, 'type' : 'vanilla', 'orderDate' : ISODate('2021-01-11T06:31:15Z'), 'state' : 'CA', 'price' : 12, " +
            "'quantity' : 145, 'cumulativeQuantityForState' : 427 }",
            "{ '_id' : 5, 'type' : 'strawberry', 'orderDate' : ISODate('2019-01-08T06:12:03Z'), 'state' : 'WA', 'price' : 43, " +
            "'quantity' : 134, 'cumulativeQuantityForState' : 134 }",
            "{ '_id' : 3, 'type' : 'vanilla', 'orderDate' : ISODate('2020-02-08T13:13:23Z'), 'state' : 'WA', 'price' : 13, " +
            "'quantity' : 104, 'cumulativeQuantityForState' : 238 }",
            "{ '_id' : 1, 'type' : 'chocolate', 'orderDate' : ISODate('2021-03-20T11:30:05Z'), 'state' : 'WA', 'price' : 14, " +
            "'quantity' : 140, 'cumulativeQuantityForState' : 378 }");

        assertListEquals(actual, expected);
    }

    @Test
    public void testShift() {
        checkMinServerVersion(5.0);

        cakeSales();

        List<Document> actual = getDs().aggregate("cakeSales")
                                       .setWindowFields(setWindowFields()
                                           .partitionBy(field("state"))
                                           .sortBy(Sort.descending("quantity"))
                                           .output(output("shiftQuantityForState")
                                               .operator(shift(field("quantity"), 1, value("Not available")))))
                                       .execute(Document.class)
                                       .toList();

        List<Document> expected = parseDocs(
            "{ '_id' : 4, 'type' : 'strawberry', 'orderDate' : ISODate('2019-05-18T16:09:01Z'), 'state' : 'CA', 'price' : 41, 'quantity' " +
            ": 162, 'shiftQuantityForState' : 145 }",
            "{ '_id' : 2, 'type' : 'vanilla', 'orderDate' : ISODate('2021-01-11T06:31:15Z'), 'state' : 'CA', 'price' : 12, 'quantity' : " +
            "145, 'shiftQuantityForState' : 120 }",
            "{ '_id' : 0, 'type' : 'chocolate', 'orderDate' : ISODate('2020-05-18T14:10:30Z'), 'state' : 'CA', 'price' : 13, 'quantity' :" +
            " 120, 'shiftQuantityForState' : 'Not available' }",
            "{ '_id' : 1, 'type' : 'chocolate', 'orderDate' : ISODate('2021-03-20T11:30:05Z'), 'state' : 'WA', 'price' : 14, 'quantity' :" +
            " 140, 'shiftQuantityForState' : 134 }",
            "{ '_id' : 5, 'type' : 'strawberry', 'orderDate' : ISODate('2019-01-08T06:12:03Z'), 'state' : 'WA', 'price' : 43, 'quantity' " +
            ": 134, 'shiftQuantityForState' : 104 }",
            "{ '_id' : 3, 'type' : 'vanilla', 'orderDate' : ISODate('2020-02-08T13:13:23Z'), 'state' : 'WA', 'price' : 13, 'quantity' : " +
            "104, 'shiftQuantityForState' : 'Not available' }");

        assertListEquals(actual, expected);
    }

    @Test
    public void testUnionWith() {
        checkMinServerVersion(4.4);
        insert("sales2019q1", parseDocs(
            "{ store: 'A', item: 'Chocolates', quantity: 150 }",
            "{ store: 'B', item: 'Chocolates', quantity: 50 }",
            "{ store: 'A', item: 'Cookies', quantity: 100 }",
            "{ store: 'B', item: 'Cookies', quantity: 120 }",
            "{ store: 'A', item: 'Pie', quantity: 10 }",
            "{ store: 'B', item: 'Pie', quantity: 5 }"));

        insert("sales2019q2", parseDocs(
            "{ store: 'A', item: 'Cheese', quantity: 30 }",
            "{ store: 'B', item: 'Cheese', quantity: 50 }",
            "{ store: 'A', item: 'Chocolates', quantity: 125 }",
            "{ store: 'B', item: 'Chocolates', quantity: 150 }",
            "{ store: 'A', item: 'Cookies', quantity: 200 }",
            "{ store: 'B', item: 'Cookies', quantity: 100 }",
            "{ store: 'B', item: 'Nuts', quantity: 100 }",
            "{ store: 'A', item: 'Pie', quantity: 30 }",
            "{ store: 'B', item: 'Pie', quantity: 25 }"));

        insert("sales2019q3", parseDocs(
            "{ store: 'A', item: 'Cheese', quantity: 50 }",
            "{ store: 'B', item: 'Cheese', quantity: 20 }",
            "{ store: 'A', item: 'Chocolates', quantity: 125 }",
            "{ store: 'B', item: 'Chocolates', quantity: 150 }",
            "{ store: 'A', item: 'Cookies', quantity: 200 }",
            "{ store: 'B', item: 'Cookies', quantity: 100 }",
            "{ store: 'A', item: 'Nuts', quantity: 80 }",
            "{ store: 'B', item: 'Nuts', quantity: 30 }",
            "{ store: 'A', item: 'Pie', quantity: 50 }",
            "{ store: 'B', item: 'Pie', quantity: 75 }"));

        insert("sales2019q4", parseDocs(
            "{ store: 'A', item: 'Cheese', quantity: 100, }",
            "{ store: 'B', item: 'Cheese', quantity: 100}",
            "{ store: 'A', item: 'Chocolates', quantity: 200 }",
            "{ store: 'B', item: 'Chocolates', quantity: 300 }",
            "{ store: 'A', item: 'Cookies', quantity: 500 }",
            "{ store: 'B', item: 'Cookies', quantity: 400 }",
            "{ store: 'A', item: 'Nuts', quantity: 100 }",
            "{ store: 'B', item: 'Nuts', quantity: 200 }",
            "{ store: 'A', item: 'Pie', quantity: 100 }",
            "{ store: 'B', item: 'Pie', quantity: 100 }"));

        List<Document> actual = getDs().aggregate("sales2019q1")
                                       .set(set().field("_id", literal("2019Q1")))
                                       .unionWith("sales2019q2", AddFields.addFields().field("_id", literal("2019Q2")))
                                       .unionWith("sales2019q3", AddFields.addFields().field("_id", literal("2019Q3")))
                                       .unionWith("sales2019q4", AddFields.addFields().field("_id", literal("2019Q4")))
                                       .sort(sort().ascending("_id", "store", "item"))
                                       .execute(Document.class)
                                       .toList();

        List<Document> expected = parseDocs(
            "{ '_id' : '2019Q1', 'store' : 'A', 'item' : 'Chocolates', 'quantity' : 150 }",
            "{ '_id' : '2019Q1', 'store' : 'A', 'item' : 'Cookies', 'quantity' : 100 }",
            "{ '_id' : '2019Q1', 'store' : 'A', 'item' : 'Pie', 'quantity' : 10 }",
            "{ '_id' : '2019Q1', 'store' : 'B', 'item' : 'Chocolates', 'quantity' : 50 }",
            "{ '_id' : '2019Q1', 'store' : 'B', 'item' : 'Cookies', 'quantity' : 120 }",
            "{ '_id' : '2019Q1', 'store' : 'B', 'item' : 'Pie', 'quantity' : 5 }",
            "{ '_id' : '2019Q2', 'store' : 'A', 'item' : 'Cheese', 'quantity' : 30 }",
            "{ '_id' : '2019Q2', 'store' : 'A', 'item' : 'Chocolates', 'quantity' : 125 }",
            "{ '_id' : '2019Q2', 'store' : 'A', 'item' : 'Cookies', 'quantity' : 200 }",
            "{ '_id' : '2019Q2', 'store' : 'A', 'item' : 'Pie', 'quantity' : 30 }",
            "{ '_id' : '2019Q2', 'store' : 'B', 'item' : 'Cheese', 'quantity' : 50 }",
            "{ '_id' : '2019Q2', 'store' : 'B', 'item' : 'Chocolates', 'quantity' : 150 }",
            "{ '_id' : '2019Q2', 'store' : 'B', 'item' : 'Cookies', 'quantity' : 100 }",
            "{ '_id' : '2019Q2', 'store' : 'B', 'item' : 'Nuts', 'quantity' : 100 }",
            "{ '_id' : '2019Q2', 'store' : 'B', 'item' : 'Pie', 'quantity' : 25 }",
            "{ '_id' : '2019Q3', 'store' : 'A', 'item' : 'Cheese', 'quantity' : 50 }",
            "{ '_id' : '2019Q3', 'store' : 'A', 'item' : 'Chocolates', 'quantity' : 125 }",
            "{ '_id' : '2019Q3', 'store' : 'A', 'item' : 'Cookies', 'quantity' : 200 }",
            "{ '_id' : '2019Q3', 'store' : 'A', 'item' : 'Nuts', 'quantity' : 80 }",
            "{ '_id' : '2019Q3', 'store' : 'A', 'item' : 'Pie', 'quantity' : 50 }",
            "{ '_id' : '2019Q3', 'store' : 'B', 'item' : 'Cheese', 'quantity' : 20 }",
            "{ '_id' : '2019Q3', 'store' : 'B', 'item' : 'Chocolates', 'quantity' : 150 }",
            "{ '_id' : '2019Q3', 'store' : 'B', 'item' : 'Cookies', 'quantity' : 100 }",
            "{ '_id' : '2019Q3', 'store' : 'B', 'item' : 'Nuts', 'quantity' : 30 }",
            "{ '_id' : '2019Q3', 'store' : 'B', 'item' : 'Pie', 'quantity' : 75 }",
            "{ '_id' : '2019Q4', 'store' : 'A', 'item' : 'Cheese', 'quantity' : 100 }",
            "{ '_id' : '2019Q4', 'store' : 'A', 'item' : 'Chocolates', 'quantity' : 200 }",
            "{ '_id' : '2019Q4', 'store' : 'A', 'item' : 'Cookies', 'quantity' : 500 }",
            "{ '_id' : '2019Q4', 'store' : 'A', 'item' : 'Nuts', 'quantity' : 100 }",
            "{ '_id' : '2019Q4', 'store' : 'A', 'item' : 'Pie', 'quantity' : 100 }",
            "{ '_id' : '2019Q4', 'store' : 'B', 'item' : 'Cheese', 'quantity' : 100 }",
            "{ '_id' : '2019Q4', 'store' : 'B', 'item' : 'Chocolates', 'quantity' : 300 }",
            "{ '_id' : '2019Q4', 'store' : 'B', 'item' : 'Cookies', 'quantity' : 400 }",
            "{ '_id' : '2019Q4', 'store' : 'B', 'item' : 'Nuts', 'quantity' : 200 }",
            "{ '_id' : '2019Q4', 'store' : 'B', 'item' : 'Pie', 'quantity' : 100 }");

        assertListEquals(actual, expected);
    }

    @Test
    public void testUnset() {
        checkMinServerVersion(4.2);
        List<Document> documents = parseDocs(
            "{'_id': 1, title: 'Antelope Antics', isbn: '0001122223334', author: {last:'An', first: 'Auntie' }, copies: "
            + "[ {warehouse: 'A', qty: 5 }, {warehouse: 'B', qty: 15 } ] }",
            "{'_id': 2, title: 'Bees Babble', isbn: '999999999333', author: {last:'Bumble', first: 'Bee' }, copies: [ "
            + "{warehouse: 'A', qty: 2 }, {warehouse: 'B', qty: 5 } ] }");
        insert("books", documents);

        for (Document document : documents) {
            document.remove("copies");
        }

        List<Document> copies = getDs().aggregate(Book.class)
                                       .unset(Unset.unset("copies"))
                                       .execute(Document.class)
                                       .toList();

        assertEquals(documents, copies);

    }

    private void cakeSales() {
        insert("cakeSales", parseDocs(
            "{ _id: 0, type: 'chocolate', orderDate: ISODate('2020-05-18T14:10:30Z'), state: 'CA', price: 13, quantity: 120 }",
            "{ _id: 1, type: 'chocolate', orderDate: ISODate('2021-03-20T11:30:05Z'), state: 'WA', price: 14, quantity: 140 }",
            "{ _id: 2, type: 'vanilla', orderDate: ISODate('2021-01-11T06:31:15Z'), state: 'CA', price: 12, quantity: 145 }",
            "{ _id: 3, type: 'vanilla', orderDate: ISODate('2020-02-08T13:13:23Z'), state: 'WA', price: 13, quantity: 104 }",
            "{ _id: 4, type: 'strawberry', orderDate: ISODate('2019-05-18T16:09:01Z'), state: 'CA', price: 41, quantity: 162 }",
            "{ _id: 5, type: 'strawberry', orderDate: ISODate('2019-01-08T06:12:03Z'), state: 'WA', price: 43, quantity: 134 }"));
    }

    private void compare(int id, List<Document> expected, List<Document> actual) {
        assertEquals(find(id, actual), find(id, expected));
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    private Document find(int id, List<Document> documents) {
        return documents.stream().filter(d -> d.getInteger("_id").equals(id)).findFirst().get();
    }

    @NotNull
    private List<Document> parseDocs(String... strings) {
        return Arrays.stream(strings).map(Document::parse)
                     .collect(Collectors.toList());
    }

    @Entity(useDiscriminator = false)
    private static class Artwork {
        @Id
        private ObjectId id;
        private Double price;
    }

    @Entity(value = "employees", useDiscriminator = false)
    private static class Employee {
        @Id
        private ObjectId id;
    }

    @Entity
    private static class Human {
        @Property("_id")
        public ObjectId id;
        public String name;
    }

    @Entity
    private static class Martian {
        @Id
        public ObjectId id;
        public String name;
    }

    @Entity
    static class ProjectedAuthor {
        private String last;
        private String first;

        public ProjectedAuthor() {
        }

        public ProjectedAuthor(String last, String first) {
            this.last = last;
            this.first = first;
        }

        @Override
        public int hashCode() {
            return Objects.hash(last, first);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof ProjectedAuthor)) {
                return false;
            }
            final ProjectedAuthor that = (ProjectedAuthor) o;
            return last.equals(that.last) &&
                   first.equals(that.first);
        }
    }

    @Entity
    static class ProjectedBook {
        @Id
        private Integer id;
        private String title;
        private ProjectedAuthor author;

        ProjectedBook() {
        }

        public ProjectedBook(Integer id, String title, String last, String first) {
            this.id = id;
            this.title = title;
            author = new ProjectedAuthor(last, first);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, title, author);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof ProjectedBook)) {
                return false;
            }
            final ProjectedBook that = (ProjectedBook) o;
            return Objects.equals(id, that.id) &&
                   title.equals(that.title) &&
                   author.equals(that.author);
        }
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
