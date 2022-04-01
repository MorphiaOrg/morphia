package dev.morphia.test.aggregation.expressions;

import dev.morphia.aggregation.stages.Projection;
import org.bson.Document;
import org.testng.annotations.Test;

import java.util.List;

import static dev.morphia.aggregation.expressions.ConditionalExpressions.condition;
import static dev.morphia.aggregation.expressions.Expressions.field;
import static dev.morphia.aggregation.expressions.Expressions.value;
import static dev.morphia.aggregation.expressions.MathExpressions.add;
import static dev.morphia.aggregation.expressions.MathExpressions.multiply;
import static dev.morphia.aggregation.expressions.VariableExpressions.let;
import static org.bson.Document.parse;

public class VariableExpressionsTest extends ExpressionsTestBase {
    @Test
    public void testLet() {
        insert("sales", List.of(
            parse("{ _id: 1, price: 10, tax: 0.50, applyDiscount: true }"),
            parse("{ _id: 2, price: 10, tax: 0.25, applyDiscount: false }")));

        List<Document> actual = getDs().aggregate("sales")
                                       .project(Projection.project()
                                                          .include("finalTotal",
                                                              let(multiply(value("$$total"), value("$$discounted")))
                                                                  .variable("total", add(field("price"), field("tax")))
                                                                  .variable("discounted",
                                                                      condition(field("applyDiscount"), value(0.9), value(1)))))
                                       .execute(Document.class)
                                       .toList();

        List<Document> expected = List.of(
            parse("{ '_id' : 1, 'finalTotal' : 9.450000000000001 }"),
            parse("{ '_id' : 2, 'finalTotal' : 10.25 }"));

        assertDocumentEquals(actual, expected);
    }

}