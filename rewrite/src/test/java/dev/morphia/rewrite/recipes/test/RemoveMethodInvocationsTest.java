package dev.morphia.rewrite.recipes.test;

import dev.morphia.rewrite.recipes.openrewrite.RemoveMethodInvocations;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.openrewrite.Recipe;

import static org.openrewrite.java.Assertions.java;

@SuppressWarnings("removal")
public class RemoveMethodInvocationsTest extends MorphiaRewriteTest {
    @Override
    protected @NotNull Recipe getRecipe() {
        RemoveMethodInvocations invocations = new RemoveMethodInvocations();
        invocations.setMethodPattern("dev.morphia.mapping.MapperOptions.Builder build()");
        return invocations;
    }

    @Test
    void update() {
        rewriteRun(
                //language=java
                java(
                        """
                                import dev.morphia.mapping.MapperOptions;

                                public class UnwrapTest {
                                    public void update() {
                                        MapperOptions options = MapperOptions.builder()
                                            .build();
                                    }
                                }
                                """,
                        """
                                import dev.morphia.mapping.MapperOptions;

                                public class UnwrapTest {
                                    public void update() {
                                        MapperOptions options = MapperOptions.builder();
                                    }
                                }
                                """));
    }

}
