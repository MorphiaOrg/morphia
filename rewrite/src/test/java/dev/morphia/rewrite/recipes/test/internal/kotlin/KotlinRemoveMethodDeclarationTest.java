package dev.morphia.rewrite.recipes.test.internal.kotlin;

import java.util.List;

import dev.morphia.rewrite.recipes.internal.RemoveMethodDeclaration;
import dev.morphia.rewrite.recipes.test.KotlinRewriteTest;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.openrewrite.Recipe;

import static org.openrewrite.kotlin.Assertions.kotlin;

class KotlinRemoveMethodDeclarationTest extends KotlinRewriteTest {

    @Override
    protected @NotNull Recipe getRecipe() {
        RemoveMethodDeclaration declaration = new RemoveMethodDeclaration();
        declaration.setMethodPatterns(List.of("Test deleteMe(kotlin.String)"));
        return declaration;
    }

    @Test
    public void testRemoveMethod() {
        rewriteRun(kotlin("""
                class Test {
                  fun deleteMe(): String {
                    return "Not today!"
                  }

                  fun deleteMe(param: String) {
                  }
                }
                """,
                """
                        class Test {
                          fun deleteMe(): String {
                            return "Not today!"
                          }
                        }
                        """,
                //language=kotlin
                spec -> spec.afterRecipe(cu -> {
                })));
    }
}