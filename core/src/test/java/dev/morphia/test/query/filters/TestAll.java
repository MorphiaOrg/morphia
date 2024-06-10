package dev.morphia.test.query.filters;

import java.util.List;

import dev.morphia.test.ServerVersion;

import org.testng.annotations.Test;

import static dev.morphia.query.filters.Filters.all;
import static dev.morphia.query.filters.Filters.elemMatch;
import static dev.morphia.query.filters.Filters.eq;
import static dev.morphia.query.filters.Filters.gt;

public class TestAll extends FilterTest {
    @Test
    public void testExample1() {
        testQuery(ServerVersion.ANY, false, true, (query) -> query.filter(all("tags", List.of("appliance", "school", "book"))));
    }

    @Test
    public void testExample2() {
        testQuery(ServerVersion.ANY, false, true, (query) -> query.filter(all("qty",
                List.of(elemMatch(eq("size", "M"), gt("num", 50)),
                        elemMatch(eq("num", 100), eq("color", "green"))))));
    }

}
