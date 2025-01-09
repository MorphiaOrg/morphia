package dev.morphia.rewrite.recipes.test;

import dev.morphia.rewrite.recipes.RegexPatternMerge;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.openrewrite.Recipe;

import static org.openrewrite.java.Assertions.java;

class RegexPatternMergeTest extends MorphiaRewriteTest {

    @NotNull
    @Override
    protected Recipe getRecipe() {
        return new RegexPatternMerge();
    }

    @Test
    public void testPatternFirstCall() {
        rewriteRun(java(
                //language=java
                """
                        import dev.morphia.Datastore;

                        import static dev.morphia.query.filters.Filters.regex;
                        import static java.util.regex.Pattern.quote;

                        public class Regexes {
                            public void find(Datastore ds) {
                                ds.find(Object.class)
                                  .filter(regex("name")
                                              .pattern(quote("'>   FISH BONES") + "$")
                                              .multiline()
                                              .options("options")
                                              );
                            }
                        }
                        """,
                //language=java
                """
                        import dev.morphia.Datastore;

                        import static dev.morphia.query.filters.Filters.regex;
                        import static java.util.regex.Pattern.quote;

                        public class Regexes {
                            public void find(Datastore ds) {
                                ds.find(Object.class)
                                  .filter(regex("name", quote("'>   FISH BONES") + "$")
                                        .multiline()
                                        .options("options")
                                              );
                            }
                        }
                        """));
    }

    @Test
    public void testPatternMerge() {
        rewriteRun(java(
                //language=java
                """
                        import dev.morphia.Datastore;

                        import static dev.morphia.query.filters.Filters.regex;
                        import static java.util.regex.Pattern.quote;

                        public class Regexes {
                            public void find(Datastore ds) {
                                ds.find(Object.class)
                                  .filter(regex("name")
                                              .multiline()
                                              .pattern(quote("'>   FISH BONES") + "$")
                                              .options("options")
                                              );
                            }
                        }
                        """,
                //language=java
                """
                        import dev.morphia.Datastore;

                        import static dev.morphia.query.filters.Filters.regex;
                        import static java.util.regex.Pattern.quote;

                        public class Regexes {
                            public void find(Datastore ds) {
                                ds.find(Object.class)
                                  .filter(regex("name", quote("'>   FISH BONES") + "$")
                                        .multiline()
                                        .options("options")
                                              );
                            }
                        }
                        """));
    }

    @Test
    public void testOnlyPatternCalled() {
        rewriteRun(java(
                //language=java
                """
                        import dev.morphia.Datastore;

                        import static dev.morphia.query.filters.Filters.regex;
                        import static java.util.regex.Pattern.quote;

                        public class Regexes {
                            public void find(Datastore ds) {
                                ds.find(Object.class)
                                  .filter(regex("name")
                                              .pattern(quote("'>   FISH BONES") + "$")
                                              );
                            }
                        }
                        """,
                //language=java
                """
                        import dev.morphia.Datastore;

                        import static dev.morphia.query.filters.Filters.regex;
                        import static java.util.regex.Pattern.quote;

                        public class Regexes {
                            public void find(Datastore ds) {
                                ds.find(Object.class)
                                  .filter(regex("name", quote("'>   FISH BONES") + "$")
                                              );
                            }
                        }
                        """));
    }

}