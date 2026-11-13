package dev.morphia.rewrite.recipes.test.query.kotlin;

import dev.morphia.rewrite.recipes.query.ArraySliceMigration;
import dev.morphia.rewrite.recipes.test.KotlinRewriteTest;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.openrewrite.Recipe;

import static org.openrewrite.kotlin.Assertions.kotlin;

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
                                val slice = ArraySlice(5, 10)
                            }
                        }
                        """,
                """
                        import dev.morphia.query.ArraySlice

                        class Upgrades {
                            fun ctor() {
                                val slice = ArraySlice.limit(10).skip(5)
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
                                return ArraySlice(5, 10)
                            }
                        }
                        """,
                """
                        import dev.morphia.query.ArraySlice

                        class Upgrades {
                            fun ctor(): ArraySlice {
                                return ArraySlice.limit(10).skip(5)
                            }
                        }
                        """,
                //language=kotlin
                spec -> spec.afterRecipe(cu -> {
                })));
    }

    @Test
    public void testWithSkipVariables() {
        rewriteRun(kotlin(
                """
                        import dev.morphia.query.ArraySlice

                        class Upgrades {
                            fun ctor(skip: Int, limit: Int): ArraySlice {
                                return ArraySlice(skip, limit)
                            }
                        }
                        """,
                """
                        import dev.morphia.query.ArraySlice

                        class Upgrades {
                            fun ctor(skip: Int, limit: Int): ArraySlice {
                                return ArraySlice.limit(limit).skip(skip)
                            }
                        }
                        """,
                spec -> spec.afterRecipe(cu -> {
                })));
    }

    @Test
    public void testWithSkipOneLiteral() {
        rewriteRun(kotlin(
                """
                        import dev.morphia.query.ArraySlice

                        class Upgrades {
                            fun ctor(): ArraySlice {
                                return ArraySlice(5, limit())
                            }

                            private fun limit(): Int {
                                return 10
                            }
                        }
                        """,
                """
                        import dev.morphia.query.ArraySlice

                        class Upgrades {
                            fun ctor(): ArraySlice {
                                return ArraySlice.limit(limit()).skip(5)
                            }

                            private fun limit(): Int {
                                return 10
                            }
                        }
                        """,
                spec -> spec.afterRecipe(cu -> {
                })));
    }

    @Test
    public void testWithSkipSideEffects() {
        rewriteRun(kotlin(
                """
                        import dev.morphia.query.ArraySlice

                        class Upgrades {
                            private var next = 0

                            fun ctor(): ArraySlice {
                                return ArraySlice(next(), next())
                            }

                            private fun next(): Int {
                                return next++
                            }
                        }
                        """));
    }

    @Test
    public void testWithSkipProperties() {
        rewriteRun(kotlin(
                """
                        import dev.morphia.query.ArraySlice

                        class Upgrades {
                            private var next = 0
                            private val skip: Int
                                get() = next++
                            private val limit: Int
                                get() = next++

                            fun ctor(): ArraySlice {
                                return ArraySlice(skip, limit)
                            }
                        }
                        """));
    }

    @Test
    public void testWithSkipShadowedProperties() {
        rewriteRun(kotlin(
                """
                        import dev.morphia.query.ArraySlice

                        class Upgrades {
                            private var next = 0
                            private val skip: Int
                                get() = next++
                            private val limit: Int
                                get() = next++

                            fun ctor(flag: Boolean): ArraySlice {
                                if (flag) {
                                    val skip = 5
                                    val limit = 10
                                }
                                return ArraySlice(skip, limit)
                            }
                        }
                        """));
    }

    @Test
    public void testWithSkipLocals() {
        rewriteRun(kotlin(
                """
                        import dev.morphia.query.ArraySlice

                        class Upgrades {
                            private val skip: Int
                                get() = 1

                            fun ctor(limit: Int): ArraySlice {
                                val skip = 5
                                return ArraySlice(skip, limit)
                            }
                        }
                        """,
                """
                        import dev.morphia.query.ArraySlice

                        class Upgrades {
                            private val skip: Int
                                get() = 1

                            fun ctor(limit: Int): ArraySlice {
                                val skip = 5
                                return ArraySlice.limit(limit).skip(skip)
                            }
                        }
                        """,
                spec -> spec.afterRecipe(cu -> {
                })));
    }

    @Test
    public void testWithSkipVolatile() {
        rewriteRun(kotlin(
                """
                        import dev.morphia.query.ArraySlice

                        class Upgrades {
                            @Volatile
                            private var skip = 0
                            @Volatile
                            private var limit = 0

                            fun ctor(): ArraySlice {
                                return ArraySlice(skip, this.limit)
                            }
                        }
                        """));
    }
}
