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
import static dev.morphia.aggregation.stages.Set.set;
import static org.testng.Assert.assertEquals;

public class TestSet extends AggregationTest {
    @Test
    @SuppressWarnings("deprecation")
    public void testSet() {
        checkMinServerVersion(4.2);
        List<Document> list = parseDocs(
                "{ _id: 1, student: 'Maya', homework: [ 10, 5, 10 ],quiz: [ 10, 8 ],extraCredit: 0 }",
                "{ _id: 2, student: 'Ryan', homework: [ 5, 6, 5 ],quiz: [ 8, 8 ],extraCredit: 8 }");

        insert("scores", list);

        List<Document> result = getDs().aggregate(Score.class)
                .set(addFields()
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

}
