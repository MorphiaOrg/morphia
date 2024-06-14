package dev.morphia.test.query.filters;

import java.util.List;
import java.util.regex.Pattern;

import org.testng.annotations.Test;

import static dev.morphia.query.filters.Filters.in;

public class TestIn extends FilterTest {

    /**
     * test data: dev/morphia/test/query/filters/in/example1
     * 
     * db.inventory.find( { quantity: { $in: [ 5, 15 ] } }, { _id: 0 } )
     */
    @Test(testName = "Use the ``$in`` Operator to Match Values")
    public void testExample1() {
        testQuery(new QueryTestOptions().removeIds(true),
                (query) -> query.filter(
                        in("quantity", List.of(5, 15))));
    }

    /**
     * test data: dev/morphia/test/query/filters/in/example2
     * 
     * db.inventory.find( { tags: { $in: [ /^be/, /^st/ ] } } )
     */
    @Test(testName = "Use the ``$in`` Operator with a Regular Expression")
    public void testExample2() {
        testQuery(new QueryTestOptions().skipDataCheck(true),
                (query) -> query.filter(
                        in("tags", List.of(Pattern.compile("^be"), Pattern.compile("^st")))));
    }
}