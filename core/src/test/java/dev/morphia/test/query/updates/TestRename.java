package dev.morphia.test.query.updates;

import dev.morphia.test.TemplatedTestBase;

import org.testng.annotations.Test;

public class TestRename extends TemplatedTestBase {

    /**
     * test data: dev/morphia/test/query/updates/rename/example1
     * 
     * db.students.updateMany( { "nmae": { $ne: null } }, { $rename: { "nmae":
     * "name" } } )
     */
    @Test(testName = "Rename a Field")
    public void testExample1() {
        testUpdate((query) -> query.filter());
    }

    /**
     * test data: dev/morphia/test/query/updates/rename/example2
     * 
     * db.students.updateOne( { _id: 1 }, { $rename: { "name.first": "name.fname" }
     * } )
     */
    @Test(testName = "Rename a Field in an Embedded Document")
    public void testExample2() {
        testUpdate((query) -> query.filter());
    }

    /**
     * test data: dev/morphia/test/query/updates/rename/example3
     * 
     * db.students.updateOne( { _id: 1 }, { $rename: { 'wife': 'spouse' } } )
     */
    @Test(testName = "Rename a Field That Does Not Exist")
    public void testExample3() {
        testUpdate((query) -> query.filter());
    }
}