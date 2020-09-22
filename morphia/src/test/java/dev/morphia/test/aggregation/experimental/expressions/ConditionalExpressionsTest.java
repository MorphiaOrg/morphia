package dev.morphia.test.aggregation.experimental.expressions;

import dev.morphia.aggregation.experimental.stages.Projection;
import org.bson.Document;
import org.testng.annotations.Test;

import java.util.List;

import static dev.morphia.aggregation.experimental.expressions.AccumulatorExpressions.avg;
import static dev.morphia.aggregation.experimental.expressions.BooleanExpressions.and;
import static dev.morphia.aggregation.experimental.expressions.ComparisonExpressions.gte;
import static dev.morphia.aggregation.experimental.expressions.ComparisonExpressions.lte;
import static dev.morphia.aggregation.experimental.expressions.ConditionalExpressions.switchExpression;
import static dev.morphia.aggregation.experimental.expressions.Expressions.field;
import static dev.morphia.aggregation.experimental.expressions.Expressions.value;
import static org.bson.Document.parse;

public class ConditionalExpressionsTest extends ExpressionsTestBase {

    @Test
    public void testSwitchExpression() {
        insert("grades", List.of(
            parse("{ '_id' : 1, 'name' : 'Susan Wilkes', 'scores' : [ 87, 86, 78 ] }"),
            parse("{ '_id' : 2, 'name' : 'Bob Hanna', 'scores' : [ 71, 64, 81 ] }"),
            parse("{ '_id' : 3, 'name' : 'James Torrelio', 'scores' : [ 91, 84, 97 ] }")));

        List<Document> actual = getDs().aggregate("grades")
                                       .project(Projection.of()
                                                          .include("name")
                                                          .include("summary", switchExpression()
                                                                                  .branch(gte(avg(field("scores")), value(80)),
                                                                                      value("Doing great!"))
                                                                                  .branch(and(
                                                                                      gte(avg(field("scores")), value(80)),
                                                                                      gte(avg(field("scores")), value(90))),
                                                                                      value("Doing pretty well."))
                                                                                  .branch(lte(avg(field("scores")), value(80)),
                                                                                      value("Needs improvement."))
                                                                                  .defaultCase(value("No Scores found."))))
                                       .execute(Document.class)
                                       .toList();

        List<Document> expected = List.of(
            parse("{ '_id' : 1, 'name' : 'Susan Wilkes', 'summary' : 'Doing great!' }"),
            parse("{ '_id' : 2, 'name' : 'Bob Hanna', 'summary' : 'Needs improvement.' }"),
            parse("{ '_id' : 3, 'name' : 'James Torrelio', 'summary' : 'Doing great!' }"));

        assertDocumentEquals(expected, actual);
    }
}