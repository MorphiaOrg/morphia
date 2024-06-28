package dev.morphia.test.query.updates;

import java.util.List;

import dev.morphia.query.filters.Filters;
import dev.morphia.test.TemplatedTestBase;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.Expressions.document;
import static dev.morphia.query.updates.UpdateOperators.set;

public class TestSet extends TemplatedTestBase {

    /**
     * test data: dev/morphia/test/query/updates/set/example1
     */
    @Test(testName = "Set Top-Level Fields")
    public void testExample1() {
        testUpdate((query) -> query.filter(
                Filters.eq("_id", 100)),
                set("quantity", 500),
                set("details", document("model", "2600")
                        .field("make", "Fashionaires")),
                set("tags", List.of("coats", "outerwear", "clothing")));
    }

    /**
     * test data: dev/morphia/test/query/updates/set/example2
     */
    @Test(testName = "Set Fields in Embedded Documents")
    public void testExample2() {
        testUpdate((query) -> query.filter(
                Filters.eq("_id", 100)),
                set("details.make", "Kustom Kidz"));
    }

    /**
     * test data: dev/morphia/test/query/updates/set/example3
     */
    @Test(testName = "Set Elements in Arrays")
    public void testExample3() {
        testUpdate((query) -> query.filter(
                Filters.eq("_id", 100)),
                set("tags.1", "rain gear"),
                set("ratings.0.rating", 2));
    }
}