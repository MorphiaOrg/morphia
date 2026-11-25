package dev.morphia.test.aggregation.expressions;

import dev.morphia.query.FindOptions;
import dev.morphia.query.Meta;
import dev.morphia.test.TemplatedTestBase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.query.filters.Filters.text;

public class TestMeta extends TemplatedTestBase {

    @Test
    @DisplayName("$meta: \"textScore\"")
    public void testMetaTextScore() {
        testQuery((query) -> query.filter(text("cake")),
                new FindOptions().projection().project(Meta.textScore("score")));
    }
}
