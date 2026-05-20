package dev.morphia.test.query.filters;

import dev.morphia.test.TemplatedTestBase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class TestMinDistance extends TemplatedTestBase {

    /**
     * test data: dev/morphia/test/query/filters/minDistance/example1
     */
    @Test
    @DisplayName("Use with ``$near``")
    public void testExample1() {
        // already tested elsewhere
    }

    /**
     * test data: dev/morphia/test/query/filters/minDistance/example2
     */
    @Test
    @DisplayName("Use with ``$nearSphere``")
    public void testExample2() {
        // legacy coordinates just won't be supported for now
    }
}