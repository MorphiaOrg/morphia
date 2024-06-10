package dev.morphia.test.query;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import dev.morphia.test.TemplatedTestBase;

import org.testng.annotations.AfterClass;

import static java.util.Arrays.stream;
import static org.testng.Assert.fail;

public class QueryTest extends TemplatedTestBase {
    @AfterClass
    public void testCoverage() {
        var type = getClass();
        var methods = stream(type.getDeclaredMethods())
                .filter(m -> m.getName().startsWith("testExample"))
                .map(m -> {
                    String name = m.getName().substring(4);
                    return Character.toLowerCase(name.charAt(0)) + name.substring(1);
                })
                .toList();
        String path = type.getPackageName();
        String simpleName = type.getSimpleName().substring(4);
        var operatorName = Character.toLowerCase(simpleName.charAt(0)) + simpleName.substring(1);
        var resourceFolder = rootToCore("src/test/resources/%s/%s".formatted(path.replace('.', '/'), operatorName));

        if (!resourceFolder.exists()) {
            throw new IllegalStateException("%s does not exist inside %s".formatted(resourceFolder,
                    new File(".").getAbsolutePath()));
        }
        List<File> list = Arrays.stream(resourceFolder.list())
                .map(s -> new File(resourceFolder, s))
                .toList();

        List<String> examples = list.stream()
                .filter(d -> new File(d, "expected.json").exists())
                .map(File::getName)
                .toList();
        var missing = examples.stream()
                .filter(example -> !methods.contains(example))
                .collect(Collectors.joining(", "));
        if (!missing.isEmpty()) {
            fail("Missing test cases for $%s: %s".formatted(operatorName, missing));
        }
    }

}
