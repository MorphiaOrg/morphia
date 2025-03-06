package dev.morphia.rewrite.recipes.test;

import dev.morphia.rewrite.recipes.ArraySliceMigration;

import org.jetbrains.annotations.NotNull;
import org.openrewrite.Recipe;
import org.testng.annotations.Test;

import static org.openrewrite.java.Assertions.java;

public class ArraySliceMigrationTest extends MorphiaRewriteTest {
    @Override
    protected @NotNull Recipe getRecipe() {
        return new ArraySliceMigration();
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
