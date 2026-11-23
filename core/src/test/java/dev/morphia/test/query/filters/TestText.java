package dev.morphia.test.query.filters;

import dev.morphia.query.FindOptions;
import dev.morphia.test.TemplatedTestBase;
import dev.morphia.test.util.ActionTestOptions;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.query.Meta.textScore;
import static dev.morphia.query.filters.Filters.*;

public class TestText extends TemplatedTestBase {

    /**
     * test data: dev/morphia/test/query/filters/text/textWithASingleWord
     */
    @Test
    @DisplayName("``$text`` with a Single Word")
    public void testTextWithASingleWord() {
        testQuery((query) -> query.filter(text("coffee")));
    }

    /**
     * test data: dev/morphia/test/query/filters/text/matchAnyOfTheSearchTerms
     */
    @Test
    @DisplayName("Match Any of the ``$search`` Terms")
    public void testMatchAnyOfTheSearchTerms() {
        testQuery(new ActionTestOptions().orderMatters(false), (query) -> query.filter(text("bake coffee cake")

        ));
    }

    /**
     * test data: dev/morphia/test/query/filters/text/textWithAnExactString
     */
    @Test
    @DisplayName("``$text`` with an Exact String")
    public void testTextWithAnExactString() {
        testQuery(new ActionTestOptions().orderMatters(false), (query) -> query.filter(text("\"coffee shop\"")

        ));
    }

    /**
     * test data: dev/morphia/test/query/filters/text/excludeDocumentsThatContainATerm
     */
    @Test
    @DisplayName("Exclude Documents That Contain a Term")
    public void testExcludeDocumentsThatContainATerm() {
        testQuery(new ActionTestOptions().orderMatters(false), (query) -> query.filter(text("coffee -shop")

        ));
    }

    /**
     * test data: dev/morphia/test/query/filters/text/queryADifferentLanguage
     */
    @Test
    @DisplayName("Query a Different Language")
    public void testQueryADifferentLanguage() {
        testQuery(new ActionTestOptions().orderMatters(false), (query) -> query.filter(text("leche").language("es")));
    }

    /**
     * test data: dev/morphia/test/query/filters/text/caseAndDiacriticInsensitivity
     */
    @Test
    @DisplayName("Case and Diacritic Insensitivity")
    public void testCaseAndDiacriticInsensitivity() {
        testQuery(new ActionTestOptions().orderMatters(false), (query) -> query.filter(text("сы́рники CAFÉS")));
    }

    /**
     * test data: dev/morphia/test/query/filters/text/caseSensitivity
     */
    @Test
    @DisplayName("Case Sensitivity")
    public void testCaseSensitivity() {
        testQuery((query) -> query.filter(text("Coffee").caseSensitive(true)));
    }

    /**
     * test data: dev/morphia/test/query/filters/text/relevanceScoreExamples
     */
    @Test
    @DisplayName("Relevance Score Examples")
    public void testRelevanceScoreExamples() {
        ActionTestOptions options = new ActionTestOptions()
                .findOptions(new FindOptions().projection().project(textScore("score")));
        testQuery(options, (query) -> query.filter(text("cake")));
    }
}