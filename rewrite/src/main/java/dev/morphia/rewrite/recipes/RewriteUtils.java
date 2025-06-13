package dev.morphia.rewrite.recipes;

import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;

import org.jetbrains.annotations.NotNull;
import org.openrewrite.java.MethodMatcher;
import org.semver4j.Semver;

public class RewriteUtils {
    private static Path morphiaCore;

    public static Path findMorphiaCore() {
        if (morphiaCore == null) {
            var repo = new File(System.getProperty("user.home"), ".m2/repository/dev/morphia/morphia/morphia-core");
            morphiaCore = Arrays.stream(repo.listFiles((dir, name) -> name.matches("\\d+\\.\\d+\\.\\d+.*")))
                    .map(File::getName)
                    .map(Semver::parse)
                    .sorted(Comparator.reverseOrder())
                    .filter(v -> v.isLowerThan("3.0.0-SNAPSHOT"))
                    .map(v -> new File(repo, "%s/morphia-core-%s.jar".formatted(v, v)))
                    .filter(File::exists)
                    .map(File::toPath)
                    .findFirst().orElseThrow();
        }

        return morphiaCore;
    }

    public static @NotNull MethodMatcher methodMatcher(String type, String pattern) {
        return new MethodMatcher(type + " " + pattern);
    }
}
