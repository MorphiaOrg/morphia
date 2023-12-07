package dev.morphia.test.aggregation;

import java.io.File;
import java.util.StringJoiner;

import dev.morphia.mapping.NamingStrategy;

import org.testng.annotations.Test;

import static java.util.Arrays.stream;
import static org.testng.Assert.fail;

public class TestCoverage {
    @Test
    public void noMissingTestCases() {
        var message = new StringJoiner("\n");
        findMissing("src/test/resources/%s/expressions", message);
        findMissing("src/test/resources/%s/stages", message);

        if (message.length() != 0) {
            fail("\n" + message);
        }
    }

    private void findMissing(String root, StringJoiner message) {
        var type = getClass();
        File path = new File(root.formatted(type.getPackageName().replace('.', '/')));
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
