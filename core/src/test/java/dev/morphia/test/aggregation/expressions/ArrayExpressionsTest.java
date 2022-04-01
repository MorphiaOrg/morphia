package dev.morphia.test.aggregation.expressions;

import com.mongodb.client.MongoCollection;
import dev.morphia.aggregation.expressions.ArrayExpressions;
import dev.morphia.aggregation.expressions.Expressions;
import dev.morphia.aggregation.stages.Projection;
import org.bson.Document;
import org.testng.annotations.Test;

import java.util.List;

import static dev.morphia.aggregation.expressions.ArrayExpressions.array;
import static dev.morphia.aggregation.expressions.ArrayExpressions.arrayToObject;
import static dev.morphia.aggregation.expressions.ArrayExpressions.concatArrays;
import static dev.morphia.aggregation.expressions.ArrayExpressions.elementAt;
import static dev.morphia.aggregation.expressions.ArrayExpressions.filter;
import static dev.morphia.aggregation.expressions.ArrayExpressions.indexOfArray;
import static dev.morphia.aggregation.expressions.ArrayExpressions.isArray;
import static dev.morphia.aggregation.expressions.ArrayExpressions.map;
import static dev.morphia.aggregation.expressions.ArrayExpressions.objectToArray;
import static dev.morphia.aggregation.expressions.ArrayExpressions.range;
import static dev.morphia.aggregation.expressions.ArrayExpressions.reduce;
import static dev.morphia.aggregation.expressions.ArrayExpressions.reverseArray;
import static dev.morphia.aggregation.expressions.ArrayExpressions.size;
import static dev.morphia.aggregation.expressions.ArrayExpressions.slice;
import static dev.morphia.aggregation.expressions.ArrayExpressions.zip;
import static dev.morphia.aggregation.expressions.BooleanExpressions.and;
import static dev.morphia.aggregation.expressions.ComparisonExpressions.gte;
import static dev.morphia.aggregation.expressions.ComparisonExpressions.lte;
import static dev.morphia.aggregation.expressions.ConditionalExpressions.condition;
import static dev.morphia.aggregation.expressions.Expressions.field;
import static dev.morphia.aggregation.expressions.Expressions.value;
import static dev.morphia.aggregation.expressions.MathExpressions.add;
import static dev.morphia.aggregation.expressions.StringExpressions.concat;
import static org.bson.Document.parse;

public class ArrayExpressionsTest extends ExpressionsTestBase {

    @Test
    public void testArrayToObject() {
        insert("inventory", List.of(
            parse("{'_id' : 1, 'item' : 'ABC1', dimensions: [{'k': 'l', 'v': 25} , {'k': 'w', 'v': 10}, {'k': 'uom', 'v': 'cm'}]}"),
            parse("{'_id' : 2, 'item' : 'ABC2', dimensions: [['l', 50], ['w', 25], ['uom', 'cm']]}"),
            parse("{'_id' : 3, 'item' : 'ABC3', dimensions: [['l', 25], ['l', 'cm'], ['l', 50]]}")));

        List<Document> actual = getDs().aggregate("inventory")
                                       .project(Projection.project()
                                                          .include("item")
                                                          .include("dimensions", arrayToObject(field("dimensions"))))
                                       .execute(Document.class)
                                       .toList();

        List<Document> expected = List.of(parse("{ '_id' : 1, 'item' : 'ABC1', 'dimensions' : { 'l' : 25, 'w' : 10, 'uom' : 'cm' } }"),
            parse("{ '_id' : 2, 'item' : 'ABC2', 'dimensions' : { 'l' : 50, 'w' : 25, 'uom' : 'cm' } }"),
            parse("{ '_id' : 3, 'item' : 'ABC3', 'dimensions' : { 'l' : 50 } }"));

        assertDocumentEquals(actual, expected);
    }

    @Test
    public void testConcatArrays() {
        assertAndCheckDocShape("{$concatArrays: [['hello', ' '], ['world']]}",
            concatArrays(array(value("hello"), value(" ")), array(value("world"))), List.of("hello", " ", "world"));
    }

    @Test
    public void testElementAt() {
        assertAndCheckDocShape("{ $arrayElemAt: [ [ 1, 2, 3 ], 0 ] }", elementAt(array(value(1), value(2), value(3)), value(0)), 1);
    }

    @Test
    public void testFilter() {

        assertAndCheckDocShape("{$filter: {input: [1, 'a', 2, null, 3.1, NumberLong(4), '5' ], as: 'num', cond: {$and: ["
                               + "{$gte: ['$$num', NumberLong('-9223372036854775807') ] },{ $lte: [ '$$num', NumberLong('9223372036854775807')]}]}}}",
            filter(array(value(1), value('a'), value(2), value(null), value(3.1), value(4L), value('5')),
                and(
                    gte(value("$$num"), value(-9223372036854775807L)),
                    lte(value("$$num"), value(9223372036854775807L))))
                .as("num"),
            List.of(1, 2, 3.1, 4L));
    }

    @Test
    public void testIn() {
        assertAndCheckDocShape("{$in: [2, [1, 2, 3]]}", ArrayExpressions.in(value(2), array(value(1), value(2), value(3))), true);
    }

    @Test
    public void testIndexOfArray() {
        assertAndCheckDocShape("{ $indexOfArray: [ [ 'a', 'abc' ], 'a' ] }", indexOfArray(array(value("a"), value("abc")),
            value("a")), 0);
    }

    @Test
    public void testIsArray() {
        assertAndCheckDocShape("{ $isArray: [ 'hello' ] }", isArray(value("hello")), false);
    }

    @Test
    public void testMap() {
        insert("grades",
            List.of(
                parse("{ _id: 1, quizzes: [ 5, 6, 7 ] }"),
                parse("{ _id: 2, quizzes: [ ] }"),
                parse("{ _id: 3, quizzes: [ 3, 8, 9 ] }")));

        List<Document> actual = getDs().aggregate("grades")
                                       .project(Projection.project()
                                                          .include("adjustedGrades",
                                                              map(field("quizzes"), add(value("$$grade"), value(2)))
                                                                  .as("grade")))
                                       .execute(Document.class)
                                       .toList();

        List<Document> expected = List.of(
            parse("{ '_id' : 1, 'adjustedGrades' : [ 7, 8, 9 ] }"),
            parse("{ '_id' : 2, 'adjustedGrades' : [ ] }"),
            parse("{ '_id' : 3, 'adjustedGrades' : [ 5, 10, 11 ] }"));

        assertDocumentEquals(actual, expected);
    }

    @Test
    public void testObjectToArray() {
        assertAndCheckDocShape("{ $objectToArray: { item: 'foo', qty: 25 } }",
            objectToArray(Expressions.of()
                                     .field("item", value("foo"))
                                     .field("qty", value(25))),
            List.of(parse("{ 'k' : 'item', 'v' : 'foo' }"), parse("{ 'k' : 'qty', 'v' : 25 }")));
    }

    @Test
    public void testRange() {
        assertAndCheckDocShape("{ $range: [ 0, 10, 2 ] }", range(0, 10).step(2), List.of(0, 2, 4, 6, 8));
    }

    @Test
    public void testReduce() {
        assertAndCheckDocShape("{$reduce: {input: ['a', 'b', 'c'], initialValue: '', in: { $concat : ['$$value', '$$this'] } } }",
            reduce(array(value("a"), value("b"), value("c")), value(""), concat(value("$$value"), value("$$this"))),
            "abc");
    }

    @Test
    public void testReverseArray() {
        MongoCollection<Document> users = getDatabase().getCollection("users");
        users.insertMany(
            List.of(parse("{ '_id' : 1, 'name' : 'dave123', 'favorites' : [ 'chocolate', 'cake', 'butter', 'apples' ] }"),
                parse("{ '_id' : 2, 'name' : 'li', 'favorites' : [ 'apples', 'pudding', 'pie' ] }"),
                parse("{ '_id' : 3, 'name' : 'ahn', 'favorites' : [ ] }"),
                parse("{ '_id' : 4, 'name' : 'ty' }")));

        List<Document> actual = getDs().aggregate("users")
                                       .project(Projection.project()
                                                          .include("name")
                                                          .include("reverseFavorites", reverseArray(field("favorites"))))
                                       .execute(Document.class)
                                       .toList();

        List<Document> expected = List.of(
            parse("{ '_id' : 1, 'name' : 'dave123', 'reverseFavorites' : [ 'apples', 'butter', 'cake', 'chocolate' ] }"),
            parse("{ '_id' : 2, 'name' : 'li', 'reverseFavorites' : [ 'pie', 'pudding', 'apples' ] }"),
            parse("{ '_id' : 3, 'name' : 'ahn', 'reverseFavorites' : [ ] }"),
            parse("{ '_id' : 4, 'name' : 'ty', 'reverseFavorites' : null }"));

        assertDocumentEquals(actual, expected);
    }

    @Test
    public void testSize() {
        insert("inventory", List.of(
            parse("{ '_id' : 1, 'item' : 'ABC1', 'description' : 'product 1', colors: [ 'blue', 'black', 'red' ] }"),
            parse("{ '_id' : 2, 'item' : 'ABC2', 'description' : 'product 2', colors: [ 'purple' ] }"),
            parse("{ '_id' : 3, 'item' : 'XYZ1', 'description' : 'product 3', colors: [ ] }"),
            parse("{ '_id' : 4, 'item' : 'ZZZ1', 'description' : 'product 4 - missing colors' }"),
            parse("{ '_id' : 5, 'item' : 'ZZZ2', 'description' : 'product 5 - colors is string', colors: 'blue,red' }")));

        List<Document> actual = getDs().aggregate("inventory")
                                       .project(Projection.project()
                                                          .include("item")
                                                          .include("numberOfColors",
                                                              condition(isArray(field("$colors")), size(field("$colors")),
                                                                  value("NA"))))
                                       .execute(Document.class)
                                       .toList();

        List<Document> expected = List.of(
            parse("{ '_id' : 1, 'item' : 'ABC1', 'numberOfColors' : 3 }"),
            parse("{ '_id' : 2, 'item' : 'ABC2', 'numberOfColors' : 1 }"),
            parse("{ '_id' : 3, 'item' : 'XYZ1', 'numberOfColors' : 0 }"),
            parse("{ '_id' : 4, 'item' : 'ZZZ1', 'numberOfColors' : 'NA' }"),
            parse("{ '_id' : 5, 'item' : 'ZZZ2', 'numberOfColors' : 'NA' }"));

        assertDocumentEquals(actual, expected);
    }

    @Test
    public void testSlice() {
        insert("users", List.of(
            parse("{ '_id' : 1, 'name' : 'dave123', favorites: [ 'chocolate', 'cake', 'butter', 'apples' ] }"),
            parse("{ '_id' : 2, 'name' : 'li', favorites: [ 'apples', 'pudding', 'pie' ] }"),
            parse("{ '_id' : 3, 'name' : 'ahn', favorites: [ 'pears', 'pecans', 'chocolate', 'cherries' ] }"),
            parse("{ '_id' : 4, 'name' : 'ty', favorites: [ 'ice cream' ] }")));

        List<Document> actual = getDs().aggregate("users")
                                       .project(Projection.project()
                                                          .include("name")
                                                          .include("threeFavorites", slice(field("favorites"), 3)))
                                       .execute(Document.class)
                                       .toList();
        List<Document> expected = List.of(parse("{ '_id' : 1, 'name' : 'dave123', 'threeFavorites' : [ 'chocolate', 'cake', 'butter' ] }"),
            parse("{ '_id' : 2, 'name' : 'li', 'threeFavorites' : [ 'apples', 'pudding', 'pie' ] }"),
            parse("{ '_id' : 3, 'name' : 'ahn', 'threeFavorites' : [ 'pears', 'pecans', 'chocolate' ] }"),
            parse("{ '_id' : 4, 'name' : 'ty', 'threeFavorites' : [ 'ice cream' ] }"));

        assertDocumentEquals(actual, expected);
    }

    @Test
    public void testZip() {
        insert("matrices", List.of(
            parse("{ matrix: [[1, 2], [2, 3], [3, 4]] }"),
            parse("{ matrix: [[8, 7], [7, 6], [5, 4]] }")));

        List<Document> actual = getDs().aggregate("matrices")
                                       .project(Projection.project()
                                                          .suppressId()
                                                          .include("transposed", zip(
                                                              elementAt(field("matrix"), value(0)),
                                                              elementAt(field("matrix"), value(1)),
                                                              elementAt(field("matrix"), value(2)))))
                                       .execute(Document.class)
                                       .toList();

        List<Document> expected = List.of(
            parse("{ 'transposed' : [ [ 1, 2, 3 ], [ 2, 3, 4 ] ] }"),
            parse("{ 'transposed' : [ [ 8, 7, 5 ], [ 7, 6, 4 ] ] }"));

        assertDocumentEquals(actual, expected);
    }
}
