package dev.morphia.test.query.updates;

import java.util.Map;

import dev.morphia.UpdateOptions;
import dev.morphia.query.filters.Filters;
import dev.morphia.test.TemplatedTestBase;
import dev.morphia.test.util.ActionTestOptions;

import org.testng.annotations.Test;

import static dev.morphia.query.updates.UpdateOperators.set;
import static dev.morphia.query.updates.UpdateOperators.setOnInsert;

public class TestSetOnInsert extends TemplatedTestBase {

    /**
     * test data: dev/morphia/test/query/updates/setOnInsert/example1
     */
    @Test(testName = "main")
    public void testExample1() {
        var options = new ActionTestOptions().skipDataCheck(true).updateOptions(new UpdateOptions().upsert(true));
        testUpdate(options, (query) -> query.filter(Filters.eq("_id", 1)), set("item", "apple"),
                setOnInsert(Map.of("defaultQty", 100)));
    }
}