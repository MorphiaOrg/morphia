package dev.morphia.rewrite.recipes.test;

import org.junit.jupiter.api.Test;
import org.openrewrite.Recipe;

import static org.openrewrite.java.Assertions.java;

public class ArraySliceMigrationTest extends MorphiaRewriteTest {

    @Override
    protected Recipe getRecipe() {
        return new ArraySliceRefasterRecipes();
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
                                ArraySlice slice = new ArraySlice(10, 10);
                            }
                        }
                        """,
                """
                        import dev.morphia.query.ArraySlice;

                        public class Upgrades {
                            public void ctor() {
                                ArraySlice slice = ArraySlice.limit(10).skip(10);
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
                                return new ArraySlice(10, 10);
                            }
                        }
                        """,
                """
                        import dev.morphia.query.ArraySlice;

                        public class Upgrades {
                            public ArraySlice ctor() {
                                return ArraySlice.limit(10).skip(10);
                            }
                        }
                        """));
    }
}
