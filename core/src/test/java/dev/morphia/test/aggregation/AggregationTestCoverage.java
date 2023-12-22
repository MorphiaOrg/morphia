package dev.morphia.test.aggregation;

import java.util.List;

import dev.morphia.test.CoverageTest;

public class AggregationTestCoverage extends CoverageTest {
    @Override
    public List<String> locations() {
        return List.of("expressions", "stages");
    }
}
