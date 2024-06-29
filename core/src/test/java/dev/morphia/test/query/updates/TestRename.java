package dev.morphia.test.query.updates;

import dev.morphia.test.TemplatedTestBase;
import dev.morphia.test.util.ActionTestOptions;

import org.testng.annotations.Test;

import static dev.morphia.query.filters.Filters.eq;
import static dev.morphia.query.filters.Filters.ne;
import static dev.morphia.query.updates.UpdateOperators.rename;

public class TestRename extends TemplatedTestBase {

    /**
     * test data: dev/morphia/test/query/updates/rename/example1
     */
    @Test(testName = "Rename a Field")
    public void testExample1() {
        testUpdate(new ActionTestOptions().skipDataCheck(true),
                (query) -> query.filter(
                        ne("nmae", null)),
                rename("nmae", "name"));
    }

    /**
     * test data: dev/morphia/test/query/updates/rename/example2
     */
    @Test(testName = "Rename a Field in an Embedded Document")
    public void testExample2() {
        testUpdate((query) -> query.filter(
                eq("_id", 1)),
                rename("name.first", "name.fname"));
    }

    /**
     * test data: dev/morphia/test/query/updates/rename/example3
     * 
     * db.students.updateOne( { _id: 1 }, { $rename: { 'wife': 'spouse' } } )
     */
    @Test(testName = "Rename a Field That Does Not Exist")
    public void testExample3() {
        testUpdate((query) -> query.filter(
                eq("_id", 1)),
                rename("wife", "spouse"));
    }
}