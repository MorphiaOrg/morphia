package dev.morphia.test.query.filters;

import java.util.List;
import java.util.regex.Pattern;

import dev.morphia.query.FindOptions;
import dev.morphia.test.TemplatedTestBase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.query.filters.Filters.in;
import static dev.morphia.query.updates.UpdateOperators.set;

public class TestIn extends TemplatedTestBase {

    /**
     * test data: dev/morphia/test/query/filters/in/useTheInOperatorToMatchValues
     */
    @Test
    @DisplayName("Use the ``$in`` Operator to Match Values")
    public void testUseTheInOperatorToMatchValues() {
        testQuery((query) -> query.filter(in("quantity", List.of(5, 15))), new FindOptions().projection().exclude("_id"));
    }

    /**
     * test data: dev/morphia/test/query/filters/in/useTheInOperatorToMatchValuesInAnArray
     */
    @Test
    @DisplayName("Use the ``$in`` Operator to Match Values in an Array")
    public void testUseTheInOperatorToMatchValuesInAnArray() {
        testUpdate((query) -> query.filter(in("tags", List.of("home", "school"))), set("exclude", false));
    }

    /**
     * test data: dev/morphia/test/query/filters/in/useTheInOperatorWithARegularExpression
     * 
     */
    @Test
    @DisplayName("Use the ``$in`` Operator with a Regular Expression")
    public void testUseTheInOperatorWithARegularExpression() {
        testQuery((query) -> query.filter(in("tags", List.of(Pattern.compile("^be"), Pattern.compile("^st")))));
    }

    @Test
    @DisplayName("Match Values")
    public void testMatchValues() {
        testQuery((query) -> query.filter(in("rated", List.of("G", "TV-G"))),
                new FindOptions().projection().include("title").projection().exclude("_id"));
    }

    @Test
    @DisplayName("Match Values in an Array")
    public void testMatchValuesInAnArray() {
        testUpdate((query) -> query.filter(in("rated", List.of("G", "TV-G"))),
                set("familyFriendly", true));
    }

    /**
     * test data: dev/morphia/test/query/filters/in/useInWithARegularExpression
     */
    @Test
    @DisplayName("Use $in with a Regular Expression")
    public void testUseInWithARegularExpression() {
        testQuery((query) -> query.filter(in("plot", List.of(Pattern.compile("^Alien"), Pattern.compile("sci-fi")))),
                new FindOptions().projection().include("title", "plot").projection().exclude("_id"));
    }
}
