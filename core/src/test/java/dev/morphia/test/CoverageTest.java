package dev.morphia.test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.StringJoiner;

import dev.morphia.mapping.NamingStrategy;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static java.util.Arrays.stream;

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
            Assertions.fail("\n" + message);
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
                    // An operator whose every scenario still waits for its test is in the audits' baseline.
                    .filter(file -> !untestedOnly(file))
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

    private static boolean untestedOnly(File operator) {
        File[] stems = operator.listFiles(File::isDirectory);
        if (stems == null || stems.length == 0) {
            return false;
        }
        String prefix = operator.getPath().replace(File.separatorChar, '/').replaceAll(".*/dev/morphia/test/", "");
        return stream(stems).allMatch(stem -> UNTESTED.contains(prefix + "/" + stem.getName()));
    }

    private static final java.util.Set<String> UNTESTED = untested();

    private static java.util.Set<String> untested() {
        File baseline = new File(CORE_ROOT, "../audits/catalog/untested-examples.json");
        try {
            java.util.Set<String> names = new java.util.TreeSet<>();
            org.bson.BsonDocument.parse(Files.readString(baseline.toPath())).getArray("examples")
                    .forEach(entry -> names.add(entry.asDocument().getString("scenario").getValue()));
            return names;
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read " + baseline, e);
        }
    }
}
