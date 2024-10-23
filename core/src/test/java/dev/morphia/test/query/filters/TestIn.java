package dev.morphia.test.query.filters;

import java.util.List;
import java.util.regex.Pattern;

import dev.morphia.test.TemplatedTestBase;
import dev.morphia.test.util.ActionTestOptions;

import org.testng.annotations.Test;

import static dev.morphia.query.filters.Filters.in;
import static dev.morphia.query.updates.UpdateOperators.set;

public class TestIn extends TemplatedTestBase {

    /**
     * test data: dev/morphia/test/query/filters/in/example1
     */
    @Test(testName = "Use the ``$in`` Operator to Match Values")
    public void testExample1() {
        testQuery(new ActionTestOptions().removeIds(true), (query) -> query.filter(in("quantity", List.of(5, 15))));
    }

    /**
     * test data: dev/morphia/test/query/filters/in/example2
     */
    @Test(testName = "Use the ``$in`` Operator to Match Values in an Array")
    public void testExample2() {
        testUpdate(new ActionTestOptions().removeIds(true).orderMatters(false),
                (query) -> query.filter(in("tags", List.of("home", "school"))), set("exclude", false));
    }

    /**
     * test data: dev/morphia/test/query/filters/in/example3
     * 
     */
    @Test(testName = "Use the ``$in`` Operator with a Regular Expression")
    public void testExample3() {
        testQuery(new ActionTestOptions().skipDataCheck(true),
                (query) -> query.filter(in("tags", List.of(Pattern.compile("^be"), Pattern.compile("^st")))));
    }
}