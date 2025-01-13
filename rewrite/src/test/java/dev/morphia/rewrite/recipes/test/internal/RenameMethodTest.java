package dev.morphia.rewrite.recipes.test.internal;

import java.util.List;

import dev.morphia.rewrite.recipes.RenameMethod;
import dev.morphia.rewrite.recipes.test.MorphiaRewriteTest;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.openrewrite.Recipe;

import static org.openrewrite.java.Assertions.java;

class RenameMethodTest extends MorphiaRewriteTest {

    @Override
    protected @NotNull Recipe getRecipe() {
        RenameMethod rename = new RenameMethod();
        rename.setMethodPatterns(List.of("Test oldName() newName"));
        return rename;
    }

    @Test
    public void testRenameMethod() {
        rewriteRun(java("""
                public class Test {
                  public String oldName() {
                    return "Not today!";
                  }

                  public String newName() {
                    return "That's what I'm talking about!";
                  }

                  public void testDeleteMe() {
                      this.oldName();
                  }
                }
                """,
                """
                        public class Test {
                          public String oldName() {
                            return "Not today!";
                          }

                          public String newName() {
                            return "That's what I'm talking about!";
                          }

                          public void testDeleteMe() {
                              this.newName();
                          }
                        }
                        """));
    }
}