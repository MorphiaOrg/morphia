package dev.morphia.test.query.filters;

import dev.morphia.query.FindOptions;
import dev.morphia.test.util.ActionTestOptions;

import org.testng.annotations.Test;

import static dev.morphia.query.Meta.textScore;
import static dev.morphia.query.filters.Filters.*;

public class TestText extends FilterTest {

    /**
     * test data: dev/morphia/test/query/filters/text/example1
     */
    @Test(testName = "Search for a Single Word")
    public void testExample1() {
        testQuery((query) -> query.filter(text("coffee")));
    }

    /**
     * test data: dev/morphia/test/query/filters/text/example2
     */
    @Test(testName = "Match Any of the Search Terms")
    public void testExample2() {
        testQuery(new ActionTestOptions().orderMatters(false), (query) -> query.filter(text("bake coffee cake")

        ));
    }

    /**
     * test data: dev/morphia/test/query/filters/text/example3
     */
    @Test(testName = "Search for a Phrase")
    public void testExample3() {
        testQuery(new ActionTestOptions().orderMatters(false), (query) -> query.filter(text("\"coffee shop\"")

        ));
    }

    /**
     * test data: dev/morphia/test/query/filters/text/example4
     */
    @Test(testName = "Exclude Documents That Contain a Term")
    public void testExample4() {
        testQuery(new ActionTestOptions().orderMatters(false), (query) -> query.filter(text("coffee -shop")

        ));
    }

    /**
     * test data: dev/morphia/test/query/filters/text/example5
     */
    @Test(testName = "Search a Different Language")
    public void testExample5() {
        testQuery(new ActionTestOptions().orderMatters(false), (query) -> query.filter(text("leche").language("es")));
    }

    /**
     * test data: dev/morphia/test/query/filters/text/example6
     */
    @Test(testName = "Case and Diacritic Insensitive Search")
    public void testExample6() {
        testQuery(new ActionTestOptions().orderMatters(false), (query) -> query.filter(text("сы́рники CAFÉS")));
    }

    /**
     * test data: dev/morphia/test/query/filters/text/example7
     */
    @Test(testName = "Perform Case Sensitive Search")
    public void testExample7() {
        testQuery((query) -> query.filter(text("Coffee").caseSensitive(true)));
    }

    /**
     * test data: dev/morphia/test/query/filters/text/example8
     */
    @Test(testName = "Diacritic Sensitive Search")
    public void testExample8() {
        testQuery(new ActionTestOptions().orderMatters(false),
                (query) -> query.filter(text("CAFÉ").diacriticSensitive(true)));
    }

    /**
     * test data: dev/morphia/test/query/filters/text/example9
     */
    @Test(testName = "Text Search Score Examples")
    public void testExample9() {
        ActionTestOptions options = new ActionTestOptions()
                .findOptions(new FindOptions().projection().project(textScore("score")));
        testQuery(options, (query) -> query.filter(text("cake")));
    }
}