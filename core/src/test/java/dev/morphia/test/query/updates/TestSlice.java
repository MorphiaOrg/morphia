package dev.morphia.test.query.updates;

import java.util.List;

import dev.morphia.test.TemplatedTestBase;

import org.bson.Document;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.query.Sort.descending;
import static dev.morphia.query.filters.Filters.eq;
import static dev.morphia.query.updates.UpdateOperators.push;

public class TestSlice extends TemplatedTestBase {

    /**
     * test data: dev/morphia/test/query/updates/slice/sliceFromTheEndOfTheArray
     */
    @Test
    @DisplayName("Slice from the End of the Array")
    public void testSliceFromTheEndOfTheArray() {
        testUpdate((query) -> query.filter(eq("_id", 1)), push("scores", List.of(80, 78, 86)).slice(-5));
    }

    /**
     * test data: dev/morphia/test/query/updates/slice/sliceFromTheFrontOfTheArray
     */
    @Test
    @DisplayName("Slice from the Front of the Array")
    public void testSliceFromTheFrontOfTheArray() {
        testUpdate((query) -> query.filter(eq("_id", 2)), push("scores", List.of(100, 20)).slice(3));
    }

    /**
     * test data: dev/morphia/test/query/updates/slice/updateArrayUsingSliceOnly
     */
    @Test
    @DisplayName("Update Array Using Slice Only")
    public void testUpdateArrayUsingSliceOnly() {
        testUpdate((query) -> query.filter(eq("_id", 3)), push("scores", List.of()).slice(-3));
    }

    /**
     * test data: dev/morphia/test/query/updates/slice/useSliceWithOtherPushModifiers
     */
    @Test
    @DisplayName("Use ``$slice`` with Other ``$push`` Modifiers")
    public void testUseSliceWithOtherPushModifiers() {
        testUpdate((query) -> query.filter(eq("_id", 5)),
                push("quizzes",
                        List.of(new Document("wk", 5).append("score", 8), new Document("wk", 6).append("score", 7),
                                new Document("wk", 7).append("score", 6)))
                        .sort(descending("score")).slice(3));
    }
}