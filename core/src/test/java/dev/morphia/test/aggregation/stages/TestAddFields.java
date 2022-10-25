package dev.morphia.test.aggregation.stages;

import java.util.List;

import dev.morphia.test.aggregation.AggregationTest;
import dev.morphia.test.aggregation.model.Score;

import org.bson.Document;
import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.AccumulatorExpressions.sum;
import static dev.morphia.aggregation.expressions.Expressions.field;
import static dev.morphia.aggregation.expressions.MathExpressions.add;
import static dev.morphia.aggregation.stages.AddFields.addFields;
import static org.bson.Document.parse;
import static org.testng.Assert.assertEquals;

public class TestAddFields extends AggregationTest {
    @Test
    public void testAddFields() {
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
}
