package dev.morphia.test.aggregation.expressions;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;

import org.bson.Document;
import org.bson.types.Decimal128;
import org.bson.types.ObjectId;
import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.Expressions.field;
import static dev.morphia.aggregation.expressions.Expressions.value;
import static dev.morphia.aggregation.expressions.TypeExpressions.isNumber;
import static dev.morphia.aggregation.expressions.TypeExpressions.toBool;
import static dev.morphia.aggregation.expressions.TypeExpressions.toDate;
import static dev.morphia.aggregation.expressions.TypeExpressions.toDecimal;
import static dev.morphia.aggregation.expressions.TypeExpressions.toDouble;
import static dev.morphia.aggregation.expressions.TypeExpressions.toInt;
import static dev.morphia.aggregation.expressions.TypeExpressions.toLong;
import static dev.morphia.aggregation.expressions.TypeExpressions.toObjectId;
import static dev.morphia.aggregation.expressions.TypeExpressions.type;
import static dev.morphia.aggregation.stages.AddFields.addFields;
import static org.bson.Document.parse;

public class TypeExpressionsTest extends ExpressionsTestBase {

    @Test
    public void testToBool() {
        assertAndCheckDocShape("{$toBool: 'true' }", toBool(value("true")), true);
    }

    @Test
    public void testIsNumber() {
        insert("examples", List.of(
                parse("{ '_id' : 1, 'reading' : 42 }"),
                parse("{ '_id' : 2, 'reading' : 'slowly' }")));

        List<Document> actual = getDs().aggregate("examples")
                .addFields(addFields()
                        .field("isNumber", isNumber(field("reading")))
                        .field("hasType", type(field("reading"))))
                .execute(Document.class)
                .toList();

        List<Document> expected = List.of(
                parse("{ '_id' : 1, 'reading' : 42, 'isNumber' : true, 'hasType' : 'int' }"),
                parse("{ '_id' : 2, 'reading' : 'slowly', 'isNumber' : false, 'hasType' : 'string' }"));

        assertListEquals(actual, expected);
    }

    @Test
    public void testToDate() {
        Date date = new Date(LocalDate.of(2018, 3, 3)
                .toEpochSecond(LocalTime.MIDNIGHT, ZoneOffset.UTC) * 1000);
        assertAndCheckDocShape("{$toDate: '2018-03-03' }", toDate(value("2018-03-03")), date);
    }

    @Test
    public void testToDecimal() {
        assertAndCheckDocShape("{$toDecimal: true }", toDecimal(value(true)), new Decimal128(1));
    }

    @Test
    public void testToDouble() {
        assertAndCheckDocShape("{$toDouble: true }", toDouble(value(true)), 1.0);
    }

    @Test
    public void testToInt() {
        assertAndCheckDocShape("{$toInt: true }", toInt(value(true)), 1);
    }

    @Test
    public void testToLong() {
        assertAndCheckDocShape("{$toLong: true }", toLong(value(true)), 1L);
    }

    @Test
    public void testToObjectId() {
        assertAndCheckDocShape("{$toObjectId: '5ab9cbfa31c2ab715d42129e'}", toObjectId(value("5ab9cbfa31c2ab715d42129e")),
                new ObjectId("5ab9cbfa31c2ab715d42129e"));
    }

    @Test
    public void testType() {
        assertAndCheckDocShape("{ $type: \"a\" }", type(value("a")), "string");
    }
}
