package dev.morphia.test.query.filters;

import dev.morphia.test.ServerVersion;

import org.testng.annotations.Test;

import static dev.morphia.query.filters.Filters.elemMatch;
import static dev.morphia.query.filters.Filters.eq;
import static dev.morphia.query.filters.Filters.gte;
import static dev.morphia.query.filters.Filters.lt;

public class TestElemMatch extends FilterTest {
    @Test(description = "Element Match")
    public void testExample1() {
        testQuery(ServerVersion.ANY, false, true, (query) -> query.filter(
                elemMatch("results", gte(80), lt(85))));
    }

    @Test
    public void testExample2() {
        testQuery(ServerVersion.ANY, false, true, (query) -> query.filter(
                elemMatch("results", eq("product", "xyz"), gte("score", 8))));
    }

    @Test
    public void testExample3() {
        testQuery(ServerVersion.ANY, false, true, (query) -> query.filter(
                elemMatch("results", eq("product", "xyz"))));
    }

}
