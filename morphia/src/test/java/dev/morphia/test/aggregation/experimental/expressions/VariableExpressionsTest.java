package dev.morphia.test.aggregation.experimental.expressions;

import dev.morphia.aggregation.experimental.stages.Projection;
import org.bson.Document;
import org.junit.jupiter.api.Test;

import java.util.List;

import static dev.morphia.aggregation.experimental.expressions.ConditionalExpressions.condition;
import static dev.morphia.aggregation.experimental.expressions.Expressions.field;
import static dev.morphia.aggregation.experimental.expressions.Expressions.value;
import static dev.morphia.aggregation.experimental.expressions.MathExpressions.add;
import static dev.morphia.aggregation.experimental.expressions.MathExpressions.multiply;
import static dev.morphia.aggregation.experimental.expressions.VariableExpressions.let;
import static org.bson.Document.parse;

public class VariableExpressionsTest extends ExpressionsTestBase {
    @Test
    public void testLet() {
        insert("sales", List.of(
            parse("{ _id: 1, price: 10, tax: 0.50, applyDiscount: true }"),
            parse("{ _id: 2, price: 10, tax: 0.25, applyDiscount: false }")));

        List<Document> actual = getDs().aggregate("sales")
                                       .project(Projection.of()
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

        assertDocumentEquals(expected, actual);
    }

}