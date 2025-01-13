package dev.morphia.rewrite.recipes.test.internal;

import java.util.List;

import dev.morphia.rewrite.recipes.internal.RemoveMethodDeclaration;
import dev.morphia.rewrite.recipes.test.MorphiaRewriteTest;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.openrewrite.Recipe;

import static org.openrewrite.java.Assertions.java;

class RemoveMethodDeclarationTest extends MorphiaRewriteTest {

    @Override
    protected @NotNull Recipe getRecipe() {
        RemoveMethodDeclaration declaration = new RemoveMethodDeclaration();
        declaration.setMethodPatterns(List.of("Test deleteMe(String)"));
        return declaration;
    }

    @Test
    public void testRemoveMethod() {
        rewriteRun(java("""
                public class Test {
                  public String deleteMe() {
                    return "Not today!";
                  }

                  public void deleteMe(String param) {
                  }
                }
                """,
                """
                        public class Test {
                          public String deleteMe() {
                            return "Not today!";
                          }
                        }
                        """));
    }
}