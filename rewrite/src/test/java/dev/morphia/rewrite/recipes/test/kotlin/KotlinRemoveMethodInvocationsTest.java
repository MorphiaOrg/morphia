package dev.morphia.rewrite.recipes.test.kotlin;

import dev.morphia.rewrite.recipes.openrewrite.RemoveMethodInvocations;
import dev.morphia.rewrite.recipes.test.KotlinRewriteTest;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.openrewrite.Recipe;

import static org.openrewrite.kotlin.Assertions.kotlin;

/**
 * This is for the internal testing recipes only
 */
public class KotlinRemoveMethodInvocationsTest extends KotlinRewriteTest {
    @Override
    protected @NotNull Recipe getRecipe() {
        RemoveMethodInvocations invocations = new RemoveMethodInvocations();
        invocations.setMethodPattern("dev.morphia.mapping.MapperOptions.Builder build()");
        return invocations;
    }

    @Test
    void update() {
        rewriteRun(
                //language=kotlin
                kotlin(
                        """
                                import dev.morphia.mapping.MapperOptions

                                class UnwrapTest {
                                    fun update() {
                                        val options: MapperOptions = MapperOptions.builder()
                                            .build()
                                    }
                                }
                                """,
                        """
                                import dev.morphia.mapping.MapperOptions

                                class UnwrapTest {
                                    fun update() {
                                        val options: MapperOptions = MapperOptions.builder()
                                    }
                                }
                                """,
                        //language=kotlin
                        spec -> spec.afterRecipe(cu -> {
                        })));
    }

}