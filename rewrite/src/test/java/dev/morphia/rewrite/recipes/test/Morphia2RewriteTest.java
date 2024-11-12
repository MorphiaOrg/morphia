package dev.morphia.rewrite.recipes.test;

import org.jetbrains.annotations.NotNull;

public abstract class Morphia2RewriteTest extends MorphiaRewriteTest {
    @Override
    protected @NotNull String findMorphiaCore() {
        var core = runtimeClasspath.stream()
                                   .filter(uri -> {
                                       String string = uri.toString();
                                       return string.contains("morphia") && string.contains("core");
                                   })
                                   .findFirst().orElseThrow().toString();

        final String artifact = core.contains("morphia-core") ? "morphia-core" : "morphia/core";
        return artifact;
    }
}
