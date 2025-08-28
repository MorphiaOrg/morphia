package dev.morphia.rewrite.recipes.test.query.kotlin;

import dev.morphia.rewrite.recipes.query.ArraySliceMigration;
import dev.morphia.rewrite.recipes.test.KotlinRewriteTest;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openrewrite.Recipe;

import static org.openrewrite.kotlin.Assertions.kotlin;

@Disabled
public class KotlinArraySliceMigrationTest extends KotlinRewriteTest {

    @Override
    protected @NotNull Recipe getRecipe() {
        return new ArraySliceMigration();
    }

    @Test
    public void testConstructor() {
        rewriteRun(kotlin(
                """
                        import dev.morphia.query.ArraySlice

                        class Upgrades {
                            fun ctor() {
                                val slice = ArraySlice(10)
                            }
                        }
                        """,
                """
                        import dev.morphia.query.ArraySlice

                        class Upgrades {
                            fun ctor() {
                                val slice = ArraySlice.limit(10)
                            }
                        }
                        """,
                //language=kotlin
                spec -> spec.afterRecipe(cu -> {
                })));
    }

    @Test
    public void testReturn() {
        rewriteRun(kotlin(
                """
                        import dev.morphia.query.ArraySlice

                        class Upgrades {
                            fun ctor(): ArraySlice {
                                return ArraySlice(10)
                            }
                        }
                        """,
                """
                        import dev.morphia.query.ArraySlice

                        class Upgrades {
                            fun ctor(): ArraySlice {
                                return ArraySlice.limit(10)
                            }
                        }
                        """,
                //language=kotlin
                spec -> spec.afterRecipe(cu -> {
                })));
    }

    @Test
    public void testWithSkip() {
        rewriteRun(kotlin(
                """
                        import dev.morphia.query.ArraySlice

                        class Upgrades {
                            fun ctor() {
                                val slice = ArraySlice(10, 10)
                            }
                        }
                        """,
                """
                        import dev.morphia.query.ArraySlice

                        class Upgrades {
                            fun ctor() {
                                val slice = ArraySlice.limit(10).skip(10)
                            }
                        }
                        """,
                //language=kotlin
                spec -> spec.afterRecipe(cu -> {
                })));
    }

    @Test
    public void testReturnWithSkip() {
        rewriteRun(kotlin(
                """
                        import dev.morphia.query.ArraySlice

                        class Upgrades {
                            fun ctor(): ArraySlice {
                                return ArraySlice(10, 10)
                            }
                        }
                        """,
                """
                        import dev.morphia.query.ArraySlice

                        class Upgrades {
                            fun ctor(): ArraySlice {
                                return ArraySlice.limit(10).skip(10)
                            }
                        }
                        """,
                //language=kotlin
                spec -> spec.afterRecipe(cu -> {
                })));
    }
}