package dev.morphia.aggregation.experimental.expressions;

import dev.morphia.aggregation.experimental.stages.Group;
import dev.morphia.aggregation.experimental.stages.Sample;
import dev.morphia.aggregation.experimental.stages.Sort;
import dev.morphia.testmodel.User;
import org.bson.Document;
import org.junit.Test;

import java.util.Date;
import java.util.List;

import static dev.morphia.aggregation.experimental.expressions.AccumulatorExpressions.addToSet;
import static dev.morphia.aggregation.experimental.expressions.AccumulatorExpressions.avg;
import static dev.morphia.aggregation.experimental.expressions.AccumulatorExpressions.first;
import static dev.morphia.aggregation.experimental.expressions.AccumulatorExpressions.last;
import static dev.morphia.aggregation.experimental.expressions.AccumulatorExpressions.min;
import static dev.morphia.aggregation.experimental.expressions.AccumulatorExpressions.push;
import static dev.morphia.aggregation.experimental.expressions.AccumulatorExpressions.stdDevPop;
import static dev.morphia.aggregation.experimental.expressions.AccumulatorExpressions.stdDevSamp;
import static dev.morphia.aggregation.experimental.expressions.AccumulatorExpressions.sum;
import static dev.morphia.aggregation.experimental.expressions.DateExpressions.dayOfYear;
import static dev.morphia.aggregation.experimental.expressions.DateExpressions.year;
import static dev.morphia.aggregation.experimental.expressions.Expressions.field;
import static dev.morphia.aggregation.experimental.expressions.Expressions.value;
import static dev.morphia.aggregation.experimental.expressions.MathExpressions.multiply;
import static dev.morphia.aggregation.experimental.stages.Group.id;
import static org.bson.Document.parse;

public class AccumulatorExpressionsTest extends ExpressionsTestBase {

    @Test
    public void testAddToSet() {
        regularDataSet();

        List<Document> actual = getDs().aggregate("sales")
                                       .group(Group.of(id()
                                                           .field("day", dayOfYear(field("date")))
                                                           .field("year", year(field("date"))))
                                                   .field("itemsSold", addToSet(field("item"))))
                                       .execute(Document.class)
                                       .toList();

        List<Document> expected = List.of(
            parse("{ '_id' : { 'day' : 46, 'year' : 2014 }, 'itemsSold' : [ 'xyz', 'abc' ] }"),
            parse("{ '_id' : { 'day' : 34, 'year' : 2014 }, 'itemsSold' : [ 'xyz', 'jkl' ] }"),
            parse("{ '_id' : { 'day' : 1, 'year' : 2014 }, 'itemsSold' : [ 'abc' ] }"));

        assertDocumentEquals(expected, actual);
    }

    @Test
    public void testAvg() {
        regularDataSet();

        List<Document> actual = getDs().aggregate("sales")
                                       .group(Group.of(id("item"))
                                                   .field("avgAmount", avg(multiply(
                                                       field("price"), field("quantity"))))
                                                   .field("avgQuantity", avg(field("quantity"))))
                                       .execute(Document.class)
                                       .toList();

        List<Document> expected = List.of(
            parse("{'_id' : 'jkl', 'avgAmount' : 20.0, 'avgQuantity' : 1.0 }"),
            parse("{'_id' : 'abc', 'avgAmount' : 60.0, 'avgQuantity' : 6.0 }"),
            parse("{'_id' : 'xyz', 'avgAmount' : 37.5, 'avgQuantity' : 7.5 }"));

        assertDocumentEquals(expected, actual);
    }

    @Test
    public void testFirst() {
        largerDataSet();

        List<Document> actual = getDs().aggregate("sales")
                                       .sort(Sort.on()
                                                 .ascending("item", "date"))
                                       .group(Group.of(id("item"))
                                                   .field("firstSalesDate", first(field("date"))))
                                       .execute(Document.class)
                                       .toList();

        List<Document> expected = List.of(
            parse("{ '_id' : 'xyz', 'firstSalesDate' : ISODate('2014-02-03T09:05:00Z') }"),
            parse("{ '_id' : 'jkl', 'firstSalesDate' : ISODate('2014-02-03T09:00:00Z') }"),
            parse("{ '_id' : 'abc', 'firstSalesDate' : ISODate('2014-01-01T08:00:00Z') }"));

        assertDocumentEquals(expected, actual);
    }

    @Test
    public void testLast() {
        largerDataSet();

        List<Document> actual = getDs().aggregate("sales")
                                       .sort(Sort.on()
                                                 .ascending("item", "date"))
                                       .group(Group.of(id("item"))
                                                   .field("lastSalesDate", last(field("date"))))
                                       .execute(Document.class)
                                       .toList();

        List<Document> expected = List.of(
            parse("{ '_id' : 'xyz', 'lastSalesDate' : ISODate('2014-02-15T14:12:12Z') }"),
            parse("{ '_id' : 'jkl', 'lastSalesDate' : ISODate('2014-02-03T09:00:00Z') }"),
            parse("{ '_id' : 'abc', 'lastSalesDate' : ISODate('2014-02-15T08:00:00Z') }"));

        assertDocumentEquals(expected, actual);
    }

    @Test
    public void testMax() {
        regularDataSet();

        List<Document> actual = getDs().aggregate("sales")
                                       .group(Group.of(id("item"))
                                                   .field("avgAmount", avg(multiply(
                                                       field("price"), field("quantity"))))
                                                   .field("avgQuantity", avg(field("quantity"))))
                                       .execute(Document.class)
                                       .toList();

        List<Document> expected = List.of(
            parse("{'_id' : 'jkl', 'avgAmount' : 20.0, 'avgQuantity' : 1.0 }"),
            parse("{'_id' : 'abc', 'avgAmount' : 60.0, 'avgQuantity' : 6.0 }"),
            parse("{'_id' : 'xyz', 'avgAmount' : 37.5, 'avgQuantity' : 7.5 }"));

        assertDocumentEquals(expected, actual);
    }

    @Test
    public void testMin() {
        regularDataSet();

        List<Document> actual = getDs().aggregate("sales")
                                       .group(Group.of(id("item"))
                                                   .field("minQuantity", min(field("quantity")))
                                                   .field("avgQuantity", avg(field("quantity"))))
                                       .execute(Document.class)
                                       .toList();

        List<Document> expected = List.of(
            parse("{ '_id' : 'xyz', 'minQuantity' : 5 }"),
            parse("{ '_id' : 'jkl', 'minQuantity' : 1 }"),
            parse("{ '_id' : 'abc', 'minQuantity' : 2 }"));

        assertDocumentEquals(expected, actual);
    }

    @Test
    public void testPush() {
        largerDataSet();

        List<Document> actual = getDs().aggregate("sales")
                                       .group(Group.of(id()
                                                           .field("day", dayOfYear(field("date")))
                                                           .field("year", year(field("date"))))
                                                   .field("itemsSold", push()
                                                                           .field("item", field("item"))
                                                                           .field("quantity", field("quantity"))))
                                       .execute(Document.class)
                                       .toList();

        List<Document> expected = List.of(
            parse("{ '_id' : { 'day' : 46, 'year' : 2014 },'itemsSold' : [{ 'item' : 'abc', 'quantity' : 10 }, { 'item' : 'xyz', "
                  + "'quantity' : 10 },{ 'item' : 'xyz', 'quantity' : 5 },{ 'item' : 'xyz', 'quantity' : 10 }]}"),
            parse("{ '_id' : { 'day' : 34, 'year' : 2014 },'itemsSold' : [{ 'item' : 'jkl', 'quantity' : 1 },{ 'item' : 'xyz', "
                  + "'quantity' : 5 }]}"),
            parse("{ '_id' : { 'day' : 1, 'year' : 2014 },'itemsSold' : [ { 'item' : 'abc', 'quantity' : 2 } ]}"));

        assertDocumentEquals(expected, actual);
    }

    @Test
    public void testStdDevPop() {
        getDatabase().getCollection("users").insertMany(List.of(
            parse(" { '_id' : 1, 'name' : 'dave123', 'quiz' : 1, 'score' : 85 }"),
            parse("{ '_id' : 2, 'name' : 'dave2', 'quiz' : 1, 'score' : 90 }"),
            parse("{ '_id' : 3, 'name' : 'ahn', 'quiz' : 1, 'score' : 71 }"),
            parse("{ '_id' : 4, 'name' : 'li', 'quiz' : 2, 'score' : 96 }"),
            parse("{ '_id' : 5, 'name' : 'annT', 'quiz' : 2, 'score' : 77 }"),
            parse("{ '_id' : 6, 'name' : 'ty', 'quiz' : 2, 'score' : 82 }  }")));

        List<Document> actual = getDs().aggregate("users")
                                       .group(Group.of(id("quiz"))
                                                   .field("stdDev", stdDevPop(field("score"))))
                                       .execute(Document.class)
                                       .toList();

        List<Document> expected = List.of(
            parse("{ '_id' : 2, 'stdDev' : 8.04155872120988 }"),
            parse("{ '_id' : 1, 'stdDev' : 8.04155872120988 }"));

        assertDocumentEquals(expected, actual);
    }

    @Test
    public void testStdDevSamp() {
        // we don't have a data set to test numbers so let's at least test we're creating the correct structures for the server
        getDs().save(new User("", new Date()));
        getDs().aggregate(User.class)
               .sample(Sample.of(100))
               .group(Group.of()
                           .field("ageStdDev", stdDevSamp(field("age"))))
               .execute(Document.class)
               .toList();
    }

    @Test
    public void testSum() {
        regularDataSet();
        List<Document> actual = getDs().aggregate("sales")
                                       .group(Group.of(id()
                                                           .field("day", dayOfYear(field("date")))
                                                           .field("year", year(field("date"))))
                                                   .field("totalAmount", sum(multiply(field("quantity"), field("price"))))
                                                   .field("count", sum(value(1))))
                                       .execute(Document.class)
                                       .toList();

        List<Document> expected = List.of(
            parse("{ '_id' : { 'day' : 46, 'year' : 2014 }, 'totalAmount' : 150, 'count' : 2 }"),
            parse("{ '_id' : { 'day' : 34, 'year' : 2014 }, 'totalAmount' : 45, 'count' : 2 }"),
            parse("{ '_id' : { 'day' : 1, 'year' : 2014 }, 'totalAmount' : 20, 'count' : 1 }"));

        assertDocumentEquals(expected, actual);
    }

    private void largerDataSet() {
        getDatabase().getCollection("sales").insertMany(List.of(
            parse("{ '_id' : 1, 'item' : 'abc', 'price' : 10, 'quantity' : 2, 'date' : ISODate('2014-01-01T08:00:00Z') }"),
            parse("{ '_id' : 2, 'item' : 'jkl', 'price' : 20, 'quantity' : 1, 'date' : ISODate('2014-02-03T09:00:00Z') }"),
            parse("{ '_id' : 3, 'item' : 'xyz', 'price' : 5, 'quantity' : 5, 'date' : ISODate('2014-02-03T09:05:00Z') }"),
            parse("{ '_id' : 4, 'item' : 'abc', 'price' : 10, 'quantity' : 10, 'date' : ISODate('2014-02-15T08:00:00Z') }"),
            parse("{ '_id' : 5, 'item' : 'xyz', 'price' : 5, 'quantity' : 10, 'date' : ISODate('2014-02-15T09:05:00Z') }"),
            parse("{ '_id' : 6, 'item' : 'xyz', 'price' : 5, 'quantity' : 5, 'date' : ISODate('2014-02-15T012:05:10Z') }"),
            parse("{ '_id' : 7, 'item' : 'xyz', 'price' : 5, 'quantity' : 10, 'date' : ISODate('2014-02-15T14:12:12Z') }")));
    }

    private void regularDataSet() {
        getDatabase().getCollection("sales").insertMany(List.of(
            parse("{ '_id' : 1, 'item' : 'abc', 'price' : 10, 'quantity' : 2, 'date' : ISODate('2014-01-01T08:00:00Z') }"),
            parse("{ '_id' : 2, 'item' : 'jkl', 'price' : 20, 'quantity' : 1, 'date' : ISODate('2014-02-03T09:00:00Z') }"),
            parse("{ '_id' : 3, 'item' : 'xyz', 'price' : 5, 'quantity' : 5, 'date' : ISODate('2014-02-03T09:05:00Z') }"),
            parse("{ '_id' : 4, 'item' : 'abc', 'price' : 10, 'quantity' : 10, 'date' : ISODate('2014-02-15T08:00:00Z') }"),
            parse("{ '_id' : 5, 'item' : 'xyz', 'price' : 5, 'quantity' : 10, 'date' : ISODate('2014-02-15T09:12:00Z') }")));
    }
}
