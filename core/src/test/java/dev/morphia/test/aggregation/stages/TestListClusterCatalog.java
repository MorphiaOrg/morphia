package dev.morphia.test.aggregation.stages;

import dev.morphia.test.TemplatedTestBase;

import org.testng.annotations.Test;

// this operation is more administrative than application oriented.  if someone needs it, we can add it but for now skip it.
@Test(enabled = false)
public class TestListClusterCatalog extends TemplatedTestBase {

    /**
     * test data: dev/morphia/test/aggregation/stages/listClusterCatalog/example1
     * 
     * use sample_mflix db.aggregate([ { $listClusterCatalog: {} } ])
     */
    @Test(testName = "List Information from All Collections", enabled = false)
    public void testExample1() {
    }

    /**
     * test data: dev/morphia/test/aggregation/stages/listClusterCatalog/example2
     * 
     * use sample_mflix db.aggregate([ { $listClusterCatalog: {
     * balancingConfiguration: true } } ])
     */
    @Test(testName = "Balancing Configuration", enabled = false)
    public void testExample2() {
    }
}