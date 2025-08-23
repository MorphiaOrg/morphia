package dev.morphia.rewrite.recipes.test.internal.kotlin;

import java.util.List;

import dev.morphia.rewrite.recipes.RenameMethod;
import dev.morphia.rewrite.recipes.test.KotlinRewriteTest;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.openrewrite.Recipe;

import static org.openrewrite.kotlin.Assertions.kotlin;

class KotlinRenameMethodTest extends KotlinRewriteTest {

    @Override
    protected @NotNull Recipe getRecipe() {
        RenameMethod rename = new RenameMethod();
        rename.setMethodPatterns(List.of("Test oldName() newName"));
        return rename;
    }

    @Test
    public void testRenameMethod() {
        rewriteRun(kotlin("""
                class Test {
                  fun oldName(): String {
                    return "Not today!"
                  }

                  fun newName(): String {
                    return "That's what I'm talking about!"
                  }

                  fun testDeleteMe() {
                      this.oldName()
                  }
                }
                """,
                """
                        class Test {
                          fun oldName(): String {
                            return "Not today!"
                          }

                          fun newName(): String {
                            return "That's what I'm talking about!"
                          }

                          fun testDeleteMe() {
                              this.newName()
                          }
                        }
                        """,
                //language=kotlin
                spec -> spec.afterRecipe(cu -> {
                })));
    }
}