package dev.morphia.test.aggregation.experimental.expressions;

import dev.morphia.aggregation.experimental.stages.AddFields;
import dev.morphia.aggregation.experimental.stages.Group;
import dev.morphia.aggregation.experimental.stages.Sort;
import dev.morphia.test.models.User;
import org.bson.Document;
import org.testng.annotations.Test;

import java.time.LocalDate;
import java.util.List;

import static dev.morphia.aggregation.experimental.expressions.AccumulatorExpressions.accumulator;
import static dev.morphia.aggregation.experimental.expressions.AccumulatorExpressions.addToSet;
import static dev.morphia.aggregation.experimental.expressions.AccumulatorExpressions.avg;
import static dev.morphia.aggregation.experimental.expressions.AccumulatorExpressions.first;
import static dev.morphia.aggregation.experimental.expressions.AccumulatorExpressions.function;
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
import static dev.morphia.aggregation.experimental.stages.Group.of;
import static java.util.List.of;
import static org.bson.Document.parse;

public class AccumulatorExpressionsTest extends ExpressionsTestBase {
    @Test
    public void testAccumulator() {
        checkMinServerVersion(4.4);
        insert("books", of(
            parse("{ '_id' : 8751, 'title' : 'The Banquet', 'author' : 'Dante', 'copies' : 2 }"),
            parse("{ '_id' : 8752, 'title' : 'Divine Comedy', 'author' : 'Dante', 'copies' : 1 }"),
            parse("{ '_id' : 8645, 'title' : 'Eclogues', 'author' : 'Dante', 'copies' : 2 }"),
            parse("{ '_id' : 7000, 'title' : 'The Odyssey', 'author' : 'Homer', 'copies' : 10 }"),
            parse("{ '_id' : 7020, 'title' : 'Iliad', 'author' : 'Homer', 'copies' : 10 }")));
        List<Document> group = getDs().aggregate("books")
                                      .group(of(id(field("author")))
                                                 .field("avgCopies",
                                                     accumulator(
                                                         "function() {\n"
                                                         + "   return { count: 0, sum: 0 }\n"
                                                         + "}",
                                                         "function(state, numCopies) {\n"
                                                         + "   return {\n"
                                                         + "       count: state.count + 1,\n"
                                                         + "       sum: state.sum + numCopies\n"
                                                         + "   }\n"
                                                         + "}",
                                                         of(field("copies")),
                                                         "function(state1, state2) {\n"
                                                         + "   return {\n"
                                                         + "      count: state1.count + state2.count,\n"
                                                         + "      sum: state1.sum + state2.sum\n"
                                                         + "   }\n"
                                                         + "}")
                                                         .finalizeFunction("function(state) {\n" +
                                                                           "   return (state.sum / state.count)\n" +
                                                                           "}")))
                                      .execute(Document.class)
                                      .toList();

        assertListEquals(group, of(
            parse("{ '_id' : 'Homer', 'avgCopies' : 10.0 }"),
            parse("{ '_id' : 'Dante', 'avgCopies' : 1.6666666666666667 }")));
    }

    @Test
    public void testAddToSet() {
        regularDataSet();

        List<Document> actual = getDs().aggregate("sales")
                                       .group(of(id()
                                                     .field("day", dayOfYear(field("date")))
                                                     .field("year", year(field("date"))))
                                                  .field("itemsSold", addToSet(field("item"))))
                                       .execute(Document.class)
                                       .toList();

        assertDocumentEquals(actual, of(
            parse("{ '_id' : { 'day' : 46, 'year' : 2014 }, 'itemsSold' : [ 'xyz', 'abc' ] }"),
            parse("{ '_id' : { 'day' : 34, 'year' : 2014 }, 'itemsSold' : [ 'xyz', 'jkl' ] }"),
            parse("{ '_id' : { 'day' : 1, 'year' : 2014 }, 'itemsSold' : [ 'abc' ] }")));
    }

    @Test
    public void testAvg() {
        regularDataSet();

        List<Document> actual = getDs().aggregate("sales")
                                       .group(of(id("item"))
                                                  .field("avgAmount", avg(multiply(
                                                      field("price"), field("quantity"))))
                                                  .field("avgQuantity", avg(field("quantity"))))
                                       .execute(Document.class)
                                       .toList();

        assertDocumentEquals(actual, of(
            parse("{'_id' : 'jkl', 'avgAmount' : 20.0, 'avgQuantity' : 1.0 }"),
            parse("{'_id' : 'abc', 'avgAmount' : 60.0, 'avgQuantity' : 6.0 }"),
            parse("{'_id' : 'xyz', 'avgAmount' : 37.5, 'avgQuantity' : 7.5 }")));
    }

    @Test
    public void testFirst() {
        largerDataSet();

        List<Document> actual = getDs().aggregate("sales")
                                       .sort(Sort.on()
                                                 .ascending("item", "date"))
                                       .group(of(id("item"))
                                                  .field("firstSalesDate", first(field("date"))))
                                       .execute(Document.class)
                                       .toList();

        assertDocumentEquals(actual, of(
            parse("{ '_id' : 'xyz', 'firstSalesDate' : ISODate('2014-02-03T09:05:00Z') }"),
            parse("{ '_id' : 'jkl', 'firstSalesDate' : ISODate('2014-02-03T09:00:00Z') }"),
            parse("{ '_id' : 'abc', 'firstSalesDate' : ISODate('2014-01-01T08:00:00Z') }")));
    }

    @Test
    public void testFunction() {
        checkMinServerVersion(4.4);
        insert("players", of(
            parse("{ _id: 1, name: 'Miss Cheevous',  scores: [ 10, 5, 10 ] }"),
            parse("{ _id: 2, name: 'Miss Ann Thrope', scores: [ 10, 10, 10 ] }"),
            parse("{ _id: 3, name: 'Mrs. Eppie Delta ', scores: [ 9, 8, 8 ] }")));

        List<Document> actual = getDs().aggregate("players")
                                       .addFields(AddFields.of()
                                                           .field("isFound", function("function(name) {\n"
                                                                                      + "  return hex_md5(name) == "
                                                                                      + "\"15b0a220baa16331e8d80e15367677ad\"\n"
                                                                                      + "}", field("name")))
                                                           .field("message", function("function(name, scores) {\n"
                                                                                      + "  let total = Array.sum(scores);\n"
                                                                                      + "  return `Hello ${name}.  Your total score is"
                                                                                      + " ${total}.`\n"
                                                                                      + "}", field("name"), field("scores"))))
                                       .execute(Document.class)
                                       .toList();

        assertDocumentEquals(actual, of(
            parse("{ '_id' : 1, 'name' : 'Miss Cheevous', 'scores' : [ 10, 5, 10 ], 'isFound' : false, 'message' : 'Hello Miss Cheevous. "
                  + " Your total score is 25.' }"),
            parse("{ '_id' : 2, 'name' : 'Miss Ann Thrope', 'scores' : [ 10, 10, 10 ], 'isFound' : true, 'message' : 'Hello Miss Ann "
                  + "Thrope.  Your total score is 30.' }"),
            parse("{ '_id' : 3, 'name' : 'Mrs. Eppie Delta ', 'scores' : [ 9, 8, 8 ], 'isFound' : false, 'message' : 'Hello Mrs. Eppie "
                  + "Delta .  Your total score is 25.' }")));
    }

    @Test
    public void testLast() {
        largerDataSet();

        List<Document> actual = getDs().aggregate("sales")
                                       .sort(Sort.on()
                                                 .ascending("item", "date"))
                                       .group(of(id("item"))
                                                  .field("lastSalesDate", last(field("date"))))
                                       .execute(Document.class)
                                       .toList();

        assertDocumentEquals(actual, of(
            parse("{ '_id' : 'xyz', 'lastSalesDate' : ISODate('2014-02-15T14:12:12Z') }"),
            parse("{ '_id' : 'jkl', 'lastSalesDate' : ISODate('2014-02-03T09:00:00Z') }"),
            parse("{ '_id' : 'abc', 'lastSalesDate' : ISODate('2014-02-15T08:00:00Z') }")));
    }

    @Test
    public void testMax() {
        regularDataSet();

        List<Document> actual = getDs().aggregate("sales")
                                       .group(of(id("item"))
                                                  .field("avgAmount", avg(multiply(
                                                      field("price"), field("quantity"))))
                                                  .field("avgQuantity", avg(field("quantity"))))
                                       .execute(Document.class)
                                       .toList();

        assertDocumentEquals(actual, of(
            parse("{'_id' : 'jkl', 'avgAmount' : 20.0, 'avgQuantity' : 1.0 }"),
            parse("{'_id' : 'abc', 'avgAmount' : 60.0, 'avgQuantity' : 6.0 }"),
            parse("{'_id' : 'xyz', 'avgAmount' : 37.5, 'avgQuantity' : 7.5 }")));
    }

    @Test
    public void testMin() {
        regularDataSet();

        List<Document> actual = getDs().aggregate("sales")
                                       .group(of(id("item"))
                                                  .field("minQuantity", min(field("quantity")))
                                                  .field("avgQuantity", avg(field("quantity"))))
                                       .execute(Document.class)
                                       .toList();

        assertDocumentEquals(actual, of(
            parse("{ '_id' : 'xyz', 'minQuantity' : 5 }"),
            parse("{ '_id' : 'jkl', 'minQuantity' : 1 }"),
            parse("{ '_id' : 'abc', 'minQuantity' : 2 }")));
    }

    @Test
    public void testPush() {
        largerDataSet();

        List<Document> actual = getDs().aggregate("sales")
                                       .group(of(id()
                                                     .field("day", dayOfYear(field("date")))
                                                     .field("year", year(field("date"))))
                                                  .field("itemsSold", push()
                                                                          .field("item", field("item"))
                                                                          .field("quantity", field("quantity"))))
                                       .execute(Document.class)
                                       .toList();

        assertDocumentEquals(actual, of(
            parse("{ '_id' : { 'day' : 46, 'year' : 2014 },'itemsSold' : [{ 'item' : 'abc', 'quantity' : 10 }, { 'item' : 'xyz', "
                  + "'quantity' : 10 },{ 'item' : 'xyz', 'quantity' : 5 },{ 'item' : 'xyz', 'quantity' : 10 }]}"),
            parse("{ '_id' : { 'day' : 34, 'year' : 2014 },'itemsSold' : [{ 'item' : 'jkl', 'quantity' : 1 },{ 'item' : 'xyz', "
                  + "'quantity' : 5 }]}"),
            parse("{ '_id' : { 'day' : 1, 'year' : 2014 },'itemsSold' : [ { 'item' : 'abc', 'quantity' : 2 } ]}")));
    }

    @Test
    public void testStdDevPop() {
        insert("users", of(
            parse(" { '_id' : 1, 'name' : 'dave123', 'quiz' : 1, 'score' : 85 }"),
            parse("{ '_id' : 2, 'name' : 'dave2', 'quiz' : 1, 'score' : 90 }"),
            parse("{ '_id' : 3, 'name' : 'ahn', 'quiz' : 1, 'score' : 71 }"),
            parse("{ '_id' : 4, 'name' : 'li', 'quiz' : 2, 'score' : 96 }"),
            parse("{ '_id' : 5, 'name' : 'annT', 'quiz' : 2, 'score' : 77 }"),
            parse("{ '_id' : 6, 'name' : 'ty', 'quiz' : 2, 'score' : 82 }  }")));

        List<Document> actual = getDs().aggregate("users")
                                       .group(of(id("quiz"))
                                                  .field("stdDev", stdDevPop(field("score"))))
                                       .execute(Document.class)
                                       .toList();

        assertDocumentEquals(actual, of(
            parse("{ '_id' : 2, 'stdDev' : 8.04155872120988 }"),
            parse("{ '_id' : 1, 'stdDev' : 8.04155872120988 }")));
    }

    @Test
    public void testStdDevSamp() {
        // we don't have a data set to test numbers so let's at least test we're creating the correct structures for the server
        getDs().save(new User("", LocalDate.now()));
        getDs().aggregate(User.class)
               .sample(100)
               .group(Group.of()
                           .field("ageStdDev", stdDevSamp(field("age"))))
               .execute(Document.class)
               .toList();
    }

    @Test
    public void testSum() {
        regularDataSet();
        List<Document> actual = getDs().aggregate("sales")
                                       .group(of(id()
                                                     .field("day", dayOfYear(field("date")))
                                                     .field("year", year(field("date"))))
                                                  .field("totalAmount", sum(multiply(field("quantity"), field("price"))))
                                                  .field("count", sum(value(1))))
                                       .execute(Document.class)
                                       .toList();

        assertDocumentEquals(actual, of(
            parse("{ '_id' : { 'day' : 46, 'year' : 2014 }, 'totalAmount' : 150, 'count' : 2 }"),
            parse("{ '_id' : { 'day' : 34, 'year' : 2014 }, 'totalAmount' : 45, 'count' : 2 }"),
            parse("{ '_id' : { 'day' : 1, 'year' : 2014 }, 'totalAmount' : 20, 'count' : 1 }")));
    }

    private void largerDataSet() {
        insert("sales", of(
            parse("{ '_id' : 1, 'item' : 'abc', 'price' : 10, 'quantity' : 2, 'date' : ISODate('2014-01-01T08:00:00Z') }"),
            parse("{ '_id' : 2, 'item' : 'jkl', 'price' : 20, 'quantity' : 1, 'date' : ISODate('2014-02-03T09:00:00Z') }"),
            parse("{ '_id' : 3, 'item' : 'xyz', 'price' : 5, 'quantity' : 5, 'date' : ISODate('2014-02-03T09:05:00Z') }"),
            parse("{ '_id' : 4, 'item' : 'abc', 'price' : 10, 'quantity' : 10, 'date' : ISODate('2014-02-15T08:00:00Z') }"),
            parse("{ '_id' : 5, 'item' : 'xyz', 'price' : 5, 'quantity' : 10, 'date' : ISODate('2014-02-15T09:05:00Z') }"),
            parse("{ '_id' : 6, 'item' : 'xyz', 'price' : 5, 'quantity' : 5, 'date' : ISODate('2014-02-15T012:05:10Z') }"),
            parse("{ '_id' : 7, 'item' : 'xyz', 'price' : 5, 'quantity' : 10, 'date' : ISODate('2014-02-15T14:12:12Z') }")));
    }

    private void regularDataSet() {
        insert("sales", of(
            parse("{ '_id' : 1, 'item' : 'abc', 'price' : 10, 'quantity' : 2, 'date' : ISODate('2014-01-01T08:00:00Z') }"),
            parse("{ '_id' : 2, 'item' : 'jkl', 'price' : 20, 'quantity' : 1, 'date' : ISODate('2014-02-03T09:00:00Z') }"),
            parse("{ '_id' : 3, 'item' : 'xyz', 'price' : 5, 'quantity' : 5, 'date' : ISODate('2014-02-03T09:05:00Z') }"),
            parse("{ '_id' : 4, 'item' : 'abc', 'price' : 10, 'quantity' : 10, 'date' : ISODate('2014-02-15T08:00:00Z') }"),
            parse("{ '_id' : 5, 'item' : 'xyz', 'price' : 5, 'quantity' : 10, 'date' : ISODate('2014-02-15T09:12:00Z') }")));
    }
}
