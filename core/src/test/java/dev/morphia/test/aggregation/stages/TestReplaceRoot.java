package dev.morphia.test.aggregation.stages;

import dev.morphia.test.ServerVersion;
import dev.morphia.test.TemplatedTestBase;
import dev.morphia.test.util.ActionTestOptions;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.Expressions.document;
import static dev.morphia.aggregation.expressions.ObjectExpressions.mergeObjects;
import static dev.morphia.aggregation.expressions.StringExpressions.concat;
import static dev.morphia.aggregation.expressions.SystemVariables.ROOT;
import static dev.morphia.aggregation.stages.Match.match;
import static dev.morphia.aggregation.stages.ReplaceRoot.replaceRoot;
import static dev.morphia.aggregation.stages.Unwind.unwind;
import static dev.morphia.query.filters.Filters.gte;

public class TestReplaceRoot extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/stages/replaceRoot/example1
     * 
     */
    @Test(testName = "``$replaceRoot`` with an Embedded Document Field")
    public void testExample1() {
        testPipeline(new ActionTestOptions().serverVersion(ServerVersion.ANY).removeIds(false).orderMatters(true),
                (aggregation) -> aggregation.pipeline(replaceRoot(mergeObjects()
                        .add(document().field("dogs", 0).field("cats", 0).field("birds", 0).field("fish", 0))
                        .add("$pets"))));
    }

    /**
     * test data: dev/morphia/test/aggregation/stages/replaceRoot/example2
     * 
     */
    @Test(testName = "``$replaceRoot`` with a Document Nested in an Array")
    public void testExample2() {
        testPipeline((aggregation) -> aggregation.pipeline(unwind("grades"), match(gte("grades.grade", 90)),
                replaceRoot("$grades")));
    }

    /**
     * test data: dev/morphia/test/aggregation/stages/replaceRoot/example3
     * 
     */
    @Test(testName = "``$replaceRoot`` with a newly created document")
    public void testExample3() {
        testPipeline(new ActionTestOptions().serverVersion(ServerVersion.ANY).removeIds(false).orderMatters(true),
                (aggregation) -> aggregation
                        .pipeline(replaceRoot().field("full_name", concat("$first_name", " ", "$last_name"))));
    }

    /**
     * test data: dev/morphia/test/aggregation/stages/replaceRoot/example4
     * 
     */
    @Test(testName = "``$replaceRoot`` with a New Document Created from ``$$ROOT`` and a Default Document")
    public void testExample4() {
        testPipeline((aggregation) -> aggregation.pipeline(replaceRoot(mergeObjects().add(
                document().field("_id", "").field("name", "").field("email", "").field("cell", "").field("home", ""))
                .add(ROOT))));
    }
}
