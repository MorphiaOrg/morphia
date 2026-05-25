package dev.morphia.test.aggregation.stages;

import dev.morphia.test.TemplatedTestBase;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

// this operation is more administrative than application oriented.  if someone needs it, we can add it but for now skip it.
public class TestListClusterCatalog extends TemplatedTestBase {

    /**
     * test data: dev/morphia/test/aggregation/stages/listClusterCatalog/example1
     *
     * use sample_mflix db.aggregate([ { $listClusterCatalog: {} } ])
     */
    @Disabled
    @Test
    @DisplayName("List Information from All Collections")
    public void testExample1() {
    }

    /**
     * test data: dev/morphia/test/aggregation/stages/listClusterCatalog/example2
     *
     * use sample_mflix db.aggregate([ { $listClusterCatalog: {
     * balancingConfiguration: true } } ])
     */
    @Disabled
    @Test
    @DisplayName("Balancing Configuration")
    public void testExample2() {
    }
}