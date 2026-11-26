package dev.morphia.test.aggregation.stages;

import dev.morphia.test.TemplatedTestBase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.aggregation.expressions.AccumulatorExpressions.sum;
import static dev.morphia.aggregation.expressions.Expressions.document;
import static dev.morphia.aggregation.stages.Documents.documents;
import static dev.morphia.aggregation.stages.Group.group;
import static dev.morphia.aggregation.stages.Group.id;
import static dev.morphia.aggregation.stages.Match.match;
import static dev.morphia.aggregation.stages.Projection.project;
import static dev.morphia.aggregation.stages.Set.set;
import static dev.morphia.aggregation.stages.Sort.sort;
import static dev.morphia.aggregation.stages.UnionWith.unionWith;
import static dev.morphia.query.filters.Filters.gt;
import static dev.morphia.query.filters.Filters.lt;

public class TestUnionWith extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/stages/unionWith/report1AllSalesByYearAndStoresAndItems
     */
    @Test
    @DisplayName("Report 1: All Sales by Year and Stores and Items")
    public void testReport1AllSalesByYearAndStoresAndItems() {
        testPipeline((aggregation) -> aggregation.pipeline(set().field("_id", "2017"),
                unionWith("sales_2018", set().field("_id", "2018")),
                unionWith("sales_2019", set().field("_id", "2019")),
                unionWith("sales_2020", set().field("_id", "2020")), sort().ascending("_id", "store", "item")));
    }

    /**
     * test data: dev/morphia/test/aggregation/stages/unionWith/report2AggregatedSalesByItems
     */
    @Test
    @DisplayName("Report 2: Aggregated Sales by Items")
    public void testReport2AggregatedSalesByItems() {
        testPipeline((aggregation) -> aggregation.pipeline(unionWith("sales_2018"), unionWith("sales_2019"),
                unionWith("sales_2020"), group(id("$item")).field("total", sum("$quantity")),
                sort().descending("total")));
    }

    /**
     * test data: dev/morphia/test/aggregation/stages/unionWith/createAUnionWithSpecifiedDocuments
     */
    @Test
    @DisplayName("Create a Union with Specified Documents")
    public void testCreateAUnionWithSpecifiedDocuments() {
        checkMinServerVersion("6.0.0");
        testPipeline(
                (aggregation) -> aggregation.pipeline(unionWith(documents(document("_id", 4).field("flavor", "orange"),
                        document("_id", 5).field("flavor", "vanilla").field("price", 20)))));
    }

    @Test
    @DisplayName("Report 1: All Movies by Category and Year")
    public void testReport1AllMoviesByCategoryAndYear() {
        testPipeline((aggregation) -> aggregation.pipeline(
                match(lt("year", 1910)),
                set().field("category", "Early Cinema"),
                unionWith("movies", match(gt("runtime", 1000)), set().field("category", "Long Runtime")),
                project().include("title").include("year").include("category").suppressId(),
                sort().ascending("category").ascending("year").ascending("title")));
    }

    @Test
    @DisplayName("Report 2: Aggregated Runtime by Genre")
    public void testReport2AggregatedRuntimeByGenre() {
        testPipeline((aggregation) -> aggregation.pipeline(
                match(lt("year", 1910)),
                unionWith("movies", match(gt("runtime", 1000))),
                group(id("$genres")).field("totalRuntime", sum("$runtime")),
                sort().descending("totalRuntime")));
    }
}
