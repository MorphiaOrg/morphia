package dev.morphia.rewrite.recipes;

import java.io.File;
import java.util.Arrays;
import java.util.Comparator;

import org.jetbrains.annotations.NotNull;
import org.openrewrite.java.MethodMatcher;
import org.semver4j.Semver;

public class RewriteUtils {
    public static File findMorphiaCore() {
        var repo = new File(System.getProperty("user.home"), ".m2/repository/dev/morphia/morphia/morphia-core");
        Semver semver = Arrays.stream(repo.listFiles((dir, name) -> name.matches("\\d+\\.\\d+\\.\\d+.*")))
                .map(File::getName)
                .map(Semver::parse)
                .sorted(Comparator.reverseOrder())
                .filter(v -> v.isLowerThan("3.0.0-SNAPSHOT"))
                .findFirst().get();

        return new File(repo, "%s/morphia-core-%s.jar".formatted(semver, semver));
    }

    public static @NotNull MethodMatcher methodMatcher(String type, String pattern) {
        return new MethodMatcher(type + " " + pattern);
    }
}
