package dev.morphia.test.aggregation.stages;

import java.util.List;

import dev.morphia.test.TemplatedTestBase;
import dev.morphia.test.aggregation.model.Score;
import dev.morphia.test.util.ActionTestOptions;

import org.bson.Document;
import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.AccumulatorExpressions.sum;
import static dev.morphia.aggregation.expressions.ArrayExpressions.array;
import static dev.morphia.aggregation.expressions.ArrayExpressions.concatArrays;
import static dev.morphia.aggregation.expressions.MathExpressions.add;
import static dev.morphia.aggregation.stages.AddFields.addFields;
import static dev.morphia.aggregation.stages.Match.match;
import static dev.morphia.query.filters.Filters.eq;
import static org.bson.Document.parse;
import static org.testng.Assert.assertEquals;

public class TestAddFields extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/stages/addFields/example1
     * 
     */
    @Test(testName = "Using Two ``$addFields`` Stages")
    public void testExample1() {
        List<Document> list = List.of(
                parse("{ _id: 1, student: 'Maya', homework: [ 10, 5, 10 ],quiz: [ 10, 8 ],extraCredit: 0 }"),
                parse("{ _id: 2, student: 'Ryan', homework: [ 5, 6, 5 ],quiz: [ 8, 8 ],extraCredit: 8 }"));

        insert("scores", list);

        List<Document> result = getDs().aggregate(Score.class)
                .addFields(addFields().field("totalHomework", sum("$homework")).field("totalQuiz", sum("$quiz")))
                .addFields(addFields().field("totalScore", add("$totalHomework", "$totalQuiz", "$extraCredit")))
                .execute(Document.class).toList();

        list = List.of(parse(
                "{ '_id' : 1, 'student' : 'Maya', 'homework' : [ 10, 5, 10 ],'quiz' : [ 10, 8 ],'extraCredit' : 0, 'totalHomework' : 25,"
                        + " 'totalQuiz' : 18, 'totalScore' : 43 }"),
                parse("{ '_id' : 2, 'student' : 'Ryan', 'homework' : [ 5, 6, 5 ],'quiz' : [ 8, 8 ],'extraCredit' : 8, 'totalHomework' : 16, "
                        + "'totalQuiz' : 16, 'totalScore' : 40 }"));

        assertEquals(result, list);
    }

    /**
     * test data: dev/morphia/test/aggregation/stages/addFields/example2
     * 
     */
    @Test(testName = "Adding Fields to an Embedded Document")
    public void testExample2() {
        testPipeline((aggregation) -> aggregation.pipeline(addFields().field("specs.fuel_type", "unleaded")));
    }

    /**
     * test data: dev/morphia/test/aggregation/stages/addFields/example3
     * 
     */
    @Test(testName = "Overwriting an existing field")
    public void testExample3() {
        testPipeline(new ActionTestOptions().orderMatters(false),
                (aggregation) -> aggregation.pipeline(addFields().field("cats", 20)));
    }

    /**
     * test data: dev/morphia/test/aggregation/stages/addFields/example4
     * 
     */
    @Test(testName = "Add Element to an Array")
    public void testExample4() {
        testPipeline((aggregation) -> aggregation.pipeline(match(eq("_id", 1)),
                addFields().field("homework", concatArrays("$homework", array(7)))));
    }

}
