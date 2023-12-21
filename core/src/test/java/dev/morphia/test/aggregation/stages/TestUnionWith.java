package dev.morphia.test.aggregation.stages;

import dev.morphia.test.ServerVersion;
import dev.morphia.test.aggregation.AggregationTest;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.AccumulatorExpressions.sum;
import static dev.morphia.aggregation.expressions.Expressions.field;
import static dev.morphia.aggregation.expressions.Expressions.value;
import static dev.morphia.aggregation.stages.Group.group;
import static dev.morphia.aggregation.stages.Group.id;
import static dev.morphia.aggregation.stages.Set.set;
import static dev.morphia.aggregation.stages.Sort.sort;
import static dev.morphia.aggregation.stages.UnionWith.unionWith;

public class TestUnionWith extends AggregationTest {
    @Test
    public void testExample1() {
        // the examples here are in subsections which the parser doesn't handle
    }

    @Test
    public void testExample2() {
        loadData("sales_2018", "data2.json");
        loadData("sales_2019", "data3.json");
        loadData("sales_2020", "data4.json");
        testPipeline(ServerVersion.ANY, false, true, (aggregation) -> aggregation.pipeline(
                set().field("_id", value("2017")),
                unionWith("sales_2018",
                        set().field("_id", value("2018"))),
                unionWith("sales_2019",
                        set().field("_id", value("2019"))),
                unionWith("sales_2020",
                        set().field("_id", value("2020"))),
                sort().ascending("_id", "store", "item")));
    }

    @Test
    public void testExample3() {
        loadData("sales_2018", "data2.json");
        loadData("sales_2019", "data3.json");
        loadData("sales_2020", "data4.json");
        testPipeline(ServerVersion.ANY, false, true, (aggregation) -> aggregation.pipeline(
                unionWith("sales_2018"),
                unionWith("sales_2019"),
                unionWith("sales_2020"),
                group(id(field("item")))
                        .field("total", sum(field("quantity"))),
                sort().descending("total")));
    }
}
