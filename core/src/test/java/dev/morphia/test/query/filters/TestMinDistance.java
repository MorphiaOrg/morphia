package dev.morphia.test.query.filters;

import dev.morphia.test.TemplatedTestBase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class TestMinDistance extends TemplatedTestBase {

    /**
     * test data: dev/morphia/test/query/filters/minDistance/useWithNear
     */
    @Test
    @DisplayName("Use with ``$near``")
    public void testUseWithNear() {
        // already tested elsewhere
    }

    /**
     * test data: dev/morphia/test/query/filters/minDistance/useWithNearSphere
     */
    @Test
    @DisplayName("Use with ``$nearSphere``")
    public void testUseWithNearSphere() {
        // legacy coordinates just won't be supported for now
    }
}