package dev.morphia.test.aggregation.stages;

import dev.morphia.test.ServerVersion;
import dev.morphia.test.TemplatedTestBase;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.AccumulatorExpressions.sum;
import static dev.morphia.aggregation.expressions.Expressions.document;
import static dev.morphia.aggregation.stages.Documents.documents;
import static dev.morphia.aggregation.stages.Group.group;
import static dev.morphia.aggregation.stages.Group.id;
import static dev.morphia.aggregation.stages.Set.set;
import static dev.morphia.aggregation.stages.Sort.sort;
import static dev.morphia.aggregation.stages.UnionWith.unionWith;

public class TestUnionWith extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/stages/unionWith/example1
     */
    @Test(testName = "Report 1: All Sales by Year and Stores and Items")
    public void testExample1() {
        loadData("sales_2018", 1);
        loadData("sales_2019", 2);
        loadData("sales_2020", 3);
        testPipeline((aggregation) -> aggregation.pipeline(set().field("_id", "2017"),
                unionWith("sales_2018", set().field("_id", "2018")),
                unionWith("sales_2019", set().field("_id", "2019")),
                unionWith("sales_2020", set().field("_id", "2020")), sort().ascending("_id", "store", "item")));
    }

    /**
     * test data: dev/morphia/test/aggregation/stages/unionWith/example2
     */
    @Test(testName = "Report 2: Aggregated Sales by Items")
    public void testExample2() {
        loadData("sales_2018", 1);
        loadData("sales_2019", 2);
        loadData("sales_2020", 3);
        testPipeline((aggregation) -> aggregation.pipeline(unionWith("sales_2018"), unionWith("sales_2019"),
                unionWith("sales_2020"), group(id("$item")).field("total", sum("$quantity")),
                sort().descending("total")));
    }

    /**
     * test data: dev/morphia/test/aggregation/stages/unionWith/example3
     */
    @Test(testName = "Create a Union with Specified Documents")
    public void testExample3() {
        checkMinServerVersion(ServerVersion.v60);
        testPipeline(
                (aggregation) -> aggregation.pipeline(unionWith(documents(document("_id", 4).field("flavor", "orange"),
                        document("_id", 5).field("flavor", "vanilla").field("price", 20)))));
    }

    /**
     * test data: dev/morphia/test/aggregation/stages/unionWith/example4
     */
    @Test(testName = "Namespaces in Subpipelines")
    public void testExample4() {
        // this is just an error case in the docs. nothing to test.
    }

}
