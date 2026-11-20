package dev.morphia.test.query.filters;

import java.util.List;
import java.util.regex.Pattern;

import dev.morphia.test.TemplatedTestBase;
import dev.morphia.test.util.ActionTestOptions;

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
        testQuery(new ActionTestOptions().removeIds(true), (query) -> query.filter(in("quantity", List.of(5, 15))));
    }

    /**
     * test data: dev/morphia/test/query/filters/in/useTheInOperatorToMatchValuesInAnArray
     */
    @Test
    @DisplayName("Use the ``$in`` Operator to Match Values in an Array")
    public void testUseTheInOperatorToMatchValuesInAnArray() {
        testUpdate(new ActionTestOptions().removeIds(true).orderMatters(false),
                (query) -> query.filter(in("tags", List.of("home", "school"))), set("exclude", false));
    }

    /**
     * test data: dev/morphia/test/query/filters/in/useTheInOperatorWithARegularExpression
     * 
     */
    @Test
    @DisplayName("Use the ``$in`` Operator with a Regular Expression")
    public void testUseTheInOperatorWithARegularExpression() {
        testQuery(new ActionTestOptions().skipDataCheck(true),
                (query) -> query.filter(in("tags", List.of(Pattern.compile("^be"), Pattern.compile("^st")))));
    }
}