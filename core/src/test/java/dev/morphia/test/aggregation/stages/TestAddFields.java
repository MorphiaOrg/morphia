package dev.morphia.test.aggregation.stages;

import java.util.List;

import dev.morphia.test.ServerVersion;
import dev.morphia.test.aggregation.AggregationTest;
import dev.morphia.test.aggregation.model.Score;

import org.bson.Document;
import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.AccumulatorExpressions.sum;
import static dev.morphia.aggregation.expressions.ArrayExpressions.array;
import static dev.morphia.aggregation.expressions.ArrayExpressions.concatArrays;
import static dev.morphia.aggregation.expressions.Expressions.field;
import static dev.morphia.aggregation.expressions.Expressions.value;
import static dev.morphia.aggregation.expressions.MathExpressions.add;
import static dev.morphia.aggregation.stages.AddFields.addFields;
import static dev.morphia.aggregation.stages.Match.match;
import static dev.morphia.query.filters.Filters.eq;
import static org.bson.Document.parse;
import static org.testng.Assert.assertEquals;

public class TestAddFields extends AggregationTest {
    @Test
    public void testExample1() {
        List<Document> list = List.of(
                parse("{ _id: 1, student: 'Maya', homework: [ 10, 5, 10 ],quiz: [ 10, 8 ],extraCredit: 0 }"),
                parse("{ _id: 2, student: 'Ryan', homework: [ 5, 6, 5 ],quiz: [ 8, 8 ],extraCredit: 8 }"));

        insert("scores", list);

        List<Document> result = getDs().aggregate(Score.class)
                .addFields(addFields()
                        .field("totalHomework", sum(field("homework")))
                        .field("totalQuiz", sum(field("quiz"))))
                .addFields(addFields()
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
    public void testExample2() {
        testPipeline(ServerVersion.ANY, false, true, (aggregation) -> aggregation.pipeline(
                addFields()
                        .field("specs.fuel_type", value("unleaded"))));
    }

    @Test
    public void testExample3() {
        testPipeline(ServerVersion.ANY, false, false, (aggregation) -> aggregation.pipeline(
                addFields()
                        .field("cats", value(20))));
    }

    @Test
    public void testExample4() {
        testPipeline(ServerVersion.ANY, false, true, (aggregation) -> aggregation.pipeline(
                match(eq("_id", 1)),
                addFields()
                        .field("homework", concatArrays(field("homework"), array(value(7))))));
    }

}
