package dev.morphia.rewrite.recipes.test.query;

import dev.morphia.rewrite.recipes.query.ArraySliceMigration;
import dev.morphia.rewrite.recipes.test.MorphiaRewriteTest;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.openrewrite.Recipe;

import static org.openrewrite.java.Assertions.java;

public class ArraySliceMigrationTest extends MorphiaRewriteTest {

    @Override
    protected @NotNull Recipe getRecipe() {
        return new ArraySliceMigration();
    }

    @Test
    public void testConstructor() {
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

    @Test
    public void testReturn() {
        rewriteRun(java(
                """
                        import dev.morphia.query.ArraySlice;

                        public class Upgrades {
                            public ArraySlice ctor() {
                                return new ArraySlice(10);
                            }
                        }
                        """,
                """
                        import dev.morphia.query.ArraySlice;

                        public class Upgrades {
                            public ArraySlice ctor() {
                                return ArraySlice.limit(10);
                            }
                        }
                        """));
    }

    @Test
    public void testWithSkip() {
        rewriteRun(java(
                """
                        import dev.morphia.query.ArraySlice;

                        public class Upgrades {
                            public void ctor() {
                                ArraySlice slice = new ArraySlice(5, 10);
                            }
                        }
                        """,
                """
                        import dev.morphia.query.ArraySlice;

                        public class Upgrades {
                            public void ctor() {
                                ArraySlice slice = ArraySlice.limit(10).skip(5);
                            }
                        }
                        """));
    }

    @Test
    public void testReturnWithSkip() {
        rewriteRun(java(
                """
                        import dev.morphia.query.ArraySlice;

                        public class Upgrades {
                            public ArraySlice ctor() {
                                return new ArraySlice(5, 10);
                            }
                        }
                        """,
                """
                        import dev.morphia.query.ArraySlice;

                        public class Upgrades {
                            public ArraySlice ctor() {
                                return ArraySlice.limit(10).skip(5);
                            }
                        }
                        """));
    }

    @Test
    public void testWithSkipVariables() {
        rewriteRun(java(
                """
                        import dev.morphia.query.ArraySlice;

                        public class Upgrades {
                            private int limit = 10;

                            public ArraySlice ctor(int skip) {
                                return new ArraySlice(skip, this.limit);
                            }
                        }
                        """,
                """
                        import dev.morphia.query.ArraySlice;

                        public class Upgrades {
                            private int limit = 10;

                            public ArraySlice ctor(int skip) {
                                return ArraySlice.limit(this.limit).skip(skip);
                            }
                        }
                        """));
    }

    @Test
    public void testWithSkipOneLiteral() {
        rewriteRun(java(
                """
                        import dev.morphia.query.ArraySlice;

                        public class Upgrades {
                            public ArraySlice ctor() {
                                return new ArraySlice(5, limit());
                            }

                            private int limit() {
                                return 10;
                            }
                        }
                        """,
                """
                        import dev.morphia.query.ArraySlice;

                        public class Upgrades {
                            public ArraySlice ctor() {
                                return ArraySlice.limit(limit()).skip(5);
                            }

                            private int limit() {
                                return 10;
                            }
                        }
                        """));
    }

    @Test
    public void testWithSkipSideEffects() {
        rewriteRun(java(
                """
                        import dev.morphia.query.ArraySlice;

                        public class Upgrades {
                            private int next;

                            public ArraySlice ctor() {
                                return new ArraySlice(next(), next());
                            }

                            private int next() {
                                return next++;
                            }
                        }
                        """));
    }

    @Test
    public void testWithSkipProperties() {
        rewriteRun(java(
                """
                        import dev.morphia.query.ArraySlice;

                        public class Upgrades {
                            private int skip = 5;
                            private int limit = 10;

                            public ArraySlice ctor() {
                                return new ArraySlice(skip, limit);
                            }
                        }
                        """,
                """
                        import dev.morphia.query.ArraySlice;

                        public class Upgrades {
                            private int skip = 5;
                            private int limit = 10;

                            public ArraySlice ctor() {
                                return ArraySlice.limit(limit).skip(skip);
                            }
                        }
                        """));
    }
}
