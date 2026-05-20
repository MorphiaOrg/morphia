package dev.morphia.test;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import static java.lang.Character.toLowerCase;
import static java.util.Arrays.stream;

public class TestCoverageExtension implements AfterAllCallback {

    @Override
    public void afterAll(ExtensionContext context) {
        Class<?> type = context.getRequiredTestClass();
        var methods = stream(type.getDeclaredMethods())
                .filter(m -> m.getName().startsWith("testExample"))
                .map(m -> {
                    String name = m.getName().substring(4);
                    return toLowerCase(name.charAt(0)) + name.substring(1);
                })
                .toList();
        String path = type.getPackageName();
        String simpleName = type.getSimpleName().substring(4);
        var operatorName = toLowerCase(simpleName.charAt(0)) + simpleName.substring(1);
        var resourceFolder = TemplatedTestBase.rootToCore(
                "src/test/resources/%s/%s".formatted(path.replace('.', '/'), operatorName));

        if (!resourceFolder.exists() || methods.isEmpty()) {
            return;
        }
        List<File> list = Arrays.stream(resourceFolder.list())
                .map(s -> new File(resourceFolder, s))
                .toList();

        List<String> examples = list.stream()
                .filter(d -> new File(d, "action.json").exists() || new File(d, "expected.json").exists())
                .map(File::getName)
                .toList();
        var missing = examples.stream()
                .filter(example -> !methods.contains(example))
                .collect(Collectors.joining(", "));
        if (!missing.isEmpty()) {
            Assertions.fail("Missing test cases for $%s: %s".formatted(operatorName, missing));
        }
    }
}
