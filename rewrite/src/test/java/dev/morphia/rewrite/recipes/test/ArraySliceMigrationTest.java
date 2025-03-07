package dev.morphia.rewrite.recipes.test;

import dev.morphia.rewrite.recipes.ArraySliceRefasterRecipe;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

public class ArraySliceMigrationTest implements RewriteTest {
    /*
     * @Override
     * protected @NotNull Recipe getRecipe() {
     * return new ArraySliceRefasterRecipe();
     * }
     */

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new ArraySliceRefasterRecipe());
    }

    @Test
    public void testConstructorMigration() {
        rewriteRun(java(
                """
                        import dev.morphia.query.ArraySlice;

                        public class Upgrades {
                            public void ctor() {
                                ArraySlice slice = new ArraySlice(10);
                            }
                        }
                        """,
                """
                        import dev.morphia.query.ArraySlice;

                        public class Upgrades {
                            public void ctor() {
                                ArraySlice slice = ArraySlice.limit(10);
                            }
                        }
                        """));
    }
}
