package dev.morphia.test.query.filters;

import dev.morphia.query.FindOptions;
import dev.morphia.test.TemplatedTestBase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.query.Meta.textScore;
import static dev.morphia.query.filters.Filters.*;
import static dev.morphia.query.filters.Filters.gt;
import static dev.morphia.query.filters.Filters.text;

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
        testQuery((query) -> query.filter(text("bake coffee cake")

        ));
    }

    /**
     * test data: dev/morphia/test/query/filters/text/textWithAnExactString
     */
    @Test
    @DisplayName("``$text`` with an Exact String")
    public void testTextWithAnExactString() {
        testQuery((query) -> query.filter(text("\"coffee shop\"")

        ));
    }

    /**
     * test data: dev/morphia/test/query/filters/text/excludeDocumentsThatContainATerm
     */
    @Test
    @DisplayName("Exclude Documents That Contain a Term")
    public void testExcludeDocumentsThatContainATerm() {
        testQuery((query) -> query.filter(text("coffee -shop")

        ));
    }

    /**
     * test data: dev/morphia/test/query/filters/text/queryADifferentLanguage
     */
    @Test
    @DisplayName("Query a Different Language")
    public void testQueryADifferentLanguage() {
        testQuery((query) -> query.filter(text("leche").language("es")));
    }

    /**
     * test data: dev/morphia/test/query/filters/text/caseAndDiacriticInsensitivity
     */
    @Test
    @DisplayName("Case and Diacritic Insensitivity")
    public void testCaseAndDiacriticInsensitivity() {
        testQuery((query) -> query.filter(text("сы́рники CAFÉS")));
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
        testQuery((query) -> query.filter(text("cake")), new FindOptions().projection().project(textScore("score")));
    }

    @Test
    @DisplayName("Case-Sensitive Exact String Search")
    public void testCaseSensitiveExactStringSearch() {
        testQuery((query) -> query.filter(text("\"Caf\u00e9 Con Leche\"").caseSensitive(true)));
    }

    @Test
    @DisplayName("Case-Sensitive Negated Term Search")
    public void testCaseSensitiveNegatedTermSearch() {
        testQuery((query) -> query.filter(text("Coffee -shop").caseSensitive(true)));
    }

    @Test
    @DisplayName("Diacritic-Sensitive Negated Term Search")
    public void testDiacriticSensitiveNegatedTermSearch() {
        testQuery((query) -> query.filter(text("leches -caf\u00e9s").diacriticSensitive(true)));
    }

    @Test
    @DisplayName("Diacritic-Sensitive Term Search")
    public void testDiacriticSensitiveTermSearch() {
        testQuery((query) -> query.filter(text("CAF\u00c9").diacriticSensitive(true)));
    }

    @Test
    @DisplayName("Match Any Search Term")
    public void testMatchAnySearchTerm() {
        testQuery((query) -> query.filter(text("baseball colorado"), gt("runtime", 1000)),
                new FindOptions().projection().include("title", "year", "runtime", "fullplot").projection().exclude("_id"));
    }

    @Test
    @DisplayName("Search for a Single Word")
    public void testSearchForASingleWord() {
        testQuery((query) -> query.filter(text("baseball"), gt("runtime", 1000)),
                new FindOptions().projection().include("title", "year", "runtime").projection().exclude("_id"));
    }

    @Test
    @DisplayName("Search for One Exact Phrase")
    public void testSearchForOneExactPhrase() {
        testQuery((query) -> query.filter(text("\"ken burns\""), gt("runtime", 1000)),
                new FindOptions().projection().include("title", "year", "runtime", "fullplot").projection().exclude("_id"));
    }

    @Test
    @DisplayName("Search for Two Exact Phrases")
    public void testSearchForTwoExactPhrases() {
        testQuery((query) -> query.filter(text("'ken burns' 'centennial'"), gt("runtime", 1000)),
                new FindOptions().projection().include("title", "year", "runtime", "fullplot").projection().exclude("_id"));
    }
}
