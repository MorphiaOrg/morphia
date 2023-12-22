package dev.morphia.test;

import java.io.File;
import java.util.List;
import java.util.StringJoiner;

import dev.morphia.mapping.NamingStrategy;
import dev.morphia.test.aggregation.AggregationTest;

import org.testng.annotations.Test;

import static java.util.Arrays.stream;
import static org.testng.Assert.fail;

public abstract class CoverageTest extends TestBase {
    public abstract List<String> locations();

    @Test
    public void noMissingTestCases() {
        var message = new StringJoiner("\n");
        var type = getClass();
        locations().forEach(location -> {
            findMissing("src/test/resources/%s/%s".formatted(type.getPackageName().replace('.', '/'), location), message);
        });

        if (message.length() != 0) {
            fail("\n" + message);
        }
    }

    private void findMissing(String root, StringJoiner message) {
        File path = AggregationTest.rootToCore(root);
        try {
            stream(path.listFiles())
                    .map(file -> {
                        var parent = new File(file.getPath().replace("resources", "java")).getParentFile();
                        return new File(parent, "Test%s.java".formatted(NamingStrategy.title().apply(file.getName())));
                    })
                    .filter(file -> !file.exists())
                    .forEach(file -> {
                        message.add("Missing a test case: " + file);
                    });
        } catch (Exception e) {
            throw new RuntimeException("failure on path: " + path, e);
        }
    }
}
