package dev.morphia.test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.StringJoiner;

import dev.morphia.mapping.NamingStrategy;

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
        File path = TemplatedTestBase.rootToCore(root);
        try {
            stream(path.listFiles())
                    .filter(file -> new File(file, "ignored").exists())
                    .map(file -> new File(file, "ignored"))
                    .filter(file -> {
                        try {
                            return Files.readAllLines(file.toPath()).isEmpty();
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .forEach(file -> {
                        message.add("Missing an ignore reason: " + file);
                    });
            stream(path.listFiles())
                    .filter(file -> !new File(file, "ignored").exists())
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
