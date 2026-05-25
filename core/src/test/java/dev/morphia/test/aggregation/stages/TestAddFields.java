package dev.morphia.test.aggregation.stages;

import java.util.List;

import dev.morphia.test.TemplatedTestBase;
import dev.morphia.test.aggregation.model.Score;
import dev.morphia.test.util.ActionTestOptions;

import org.bson.Document;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.aggregation.expressions.AccumulatorExpressions.sum;
import static dev.morphia.aggregation.expressions.ArrayExpressions.array;
import static dev.morphia.aggregation.expressions.ArrayExpressions.concatArrays;
import static dev.morphia.aggregation.expressions.MathExpressions.add;
import static dev.morphia.aggregation.expressions.SystemVariables.REMOVE;
import static dev.morphia.aggregation.stages.AddFields.addFields;
import static dev.morphia.aggregation.stages.Match.match;
import static dev.morphia.query.filters.Filters.eq;
import static org.bson.Document.parse;

public class TestAddFields extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/stages/addFields/example1
     * 
     */
    @Test
    @DisplayName("Using Two ``$addFields`` Stages")
    public void testExample1() {
        List<Document> list = List.of(
                parse("{ _id: 1, student: 'Maya', homework: [ 10, 5, 10 ],quiz: [ 10, 8 ],extraCredit: 0 }"),
                parse("{ _id: 2, student: 'Ryan', homework: [ 5, 6, 5 ],quiz: [ 8, 8 ],extraCredit: 8 }"));

        insert("scores", list);

        List<Document> result = getDs().aggregate(Score.class, Document.class)
                .pipeline(addFields().field("totalHomework", sum("$homework")).field("totalQuiz", sum("$quiz")),
                        addFields().field("totalScore", add("$totalHomework", "$totalQuiz", "$extraCredit")))
                .toList();

        list = List.of(parse(
                "{ '_id' : 1, 'student' : 'Maya', 'homework' : [ 10, 5, 10 ],'quiz' : [ 10, 8 ],'extraCredit' : 0, 'totalHomework' : 25,"
                        + " 'totalQuiz' : 18, 'totalScore' : 43 }"),
                parse("{ '_id' : 2, 'student' : 'Ryan', 'homework' : [ 5, 6, 5 ],'quiz' : [ 8, 8 ],'extraCredit' : 8, 'totalHomework' : 16, "
                        + "'totalQuiz' : 16, 'totalScore' : 40 }"));

        assertDocumentListEquals(list, result);
    }

    /**
     * test data: dev/morphia/test/aggregation/stages/addFields/example2
     * 
     */
    @Test
    @DisplayName("Adding Fields to an Embedded Document")
    public void testExample2() {
        testPipeline((aggregation) -> aggregation.pipeline(addFields().field("specs.fuel_type", "unleaded")));
    }

    /**
     * test data: dev/morphia/test/aggregation/stages/addFields/example3
     * 
     */
    @Test
    @DisplayName("Overwriting an existing field")
    public void testExample3() {
        testPipeline(new ActionTestOptions().orderMatters(false),
                (aggregation) -> aggregation.pipeline(addFields().field("cats", 20)));
    }

    /**
     * test data: dev/morphia/test/aggregation/stages/addFields/example4
     * 
     */
    @Test
    @DisplayName("Add Element to an Array")
    public void testExample4() {
        testPipeline((aggregation) -> aggregation.pipeline(match(eq("_id", 1)),
                addFields().field("homework", concatArrays("$homework", array(7)))));
    }

    /**
     * test data: dev/morphia/test/aggregation/stages/addFields/example5
     * 
     * db.labReadings.aggregate( [ { $addFields: { date: "$$REMOVE" } } ] )
     */
    @Test
    @DisplayName("Remove Fields")
    public void testExample5() {
        testPipeline(new ActionTestOptions().removeIds(true),
                aggregation -> aggregation.pipeline(addFields().field("date", REMOVE)));
    }

}
