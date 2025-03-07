package dev.morphia.rewrite.recipes;

import java.io.File;

public class RewriteUtils {
    public static File findMorphiaCore() {
        var repo = new File(System.getProperty("user.home"), ".m2/repository/dev/morphia/morphia/morphia-core");
        File[] files = repo.listFiles((dir, name) -> name.matches("\\d+\\.\\d+\\.\\d+"));
        String[] split = files[files.length - 1].getName().split("\\.");
        var version = "%s.%s.%s-SNAPSHOT".formatted(split[0], split[1], Integer.valueOf(split[2]) + 1);

        return new File(repo, "%s/morphia-core-%s.jar".formatted(version, version));
    }
}
