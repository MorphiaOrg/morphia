package dev.morphia.test.query;

import java.util.Collections;
import java.util.List;

import dev.morphia.test.CoverageTest;

public class QueryTestCoverage extends CoverageTest {
    @Override
    public List<String> locations() {
        return Collections.emptyList(); //List.of("filters", "operators");
    }
}
