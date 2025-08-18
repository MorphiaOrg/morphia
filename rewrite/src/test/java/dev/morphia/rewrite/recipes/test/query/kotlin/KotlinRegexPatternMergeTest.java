package dev.morphia.rewrite.recipes.test.query.kotlin;

import dev.morphia.rewrite.recipes.RegexPatternMerge;
import dev.morphia.rewrite.recipes.test.KotlinRewriteTest;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.openrewrite.Recipe;

import static org.openrewrite.kotlin.Assertions.kotlin;

class KotlinRegexPatternMergeTest extends KotlinRewriteTest {

    @NotNull
    @Override
    protected Recipe getRecipe() {
        return new RegexPatternMerge();
    }

    @Test
    public void testPatternFirstCall() {
        rewriteRun(kotlin(
                //language=kotlin
                """
                        import dev.morphia.Datastore
                        import dev.morphia.query.filters.Filters.regex
                        import java.util.regex.Pattern.quote

                        class Regexes {
                            fun find(ds: Datastore) {
                                ds.find(Any::class.java)
                                  .filter(regex("name")
                                              .pattern(quote("'>   FISH BONES") + "$")
                                              .multiline()
                                              .options("options")
                                              )
                            }
                        }
                        """,
                //language=kotlin
                """
                        import dev.morphia.Datastore
                        import dev.morphia.query.filters.Filters.regex
                        import java.util.regex.Pattern.quote

                        class Regexes {
                            fun find(ds: Datastore) {
                                ds.find(Any::class.java)
                                  .filter(regex("name", quote("'>   FISH BONES") + "$")
                                    .multiline()
                                    .options("options")
                                              )
                            }
                        }
                        """));
    }

    @Test
    public void testPatternMerge() {
        rewriteRun(kotlin(
                //language=kotlin
                """
                        import dev.morphia.Datastore
                        import dev.morphia.query.filters.Filters.regex
                        import java.util.regex.Pattern.quote

                        class Regexes {
                            fun find(ds: Datastore) {
                                ds.find(Any::class.java)
                                  .filter(regex("name")
                                              .multiline()
                                              .pattern(quote("'>   FISH BONES") + "$")
                                              .options("options")
                                              )
                            }
                        }
                        """,
                //language=kotlin
                """
                        import dev.morphia.Datastore
                        import dev.morphia.query.filters.Filters.regex
                        import java.util.regex.Pattern.quote

                        class Regexes {
                            fun find(ds: Datastore) {
                                ds.find(Any::class.java)
                                  .filter(regex("name", quote("'>   FISH BONES") + "$")
                                    .multiline()
                                    .options("options")
                                              )
                            }
                        }
                        """));
    }

    @Test
    public void testOnlyPatternCalled() {
        rewriteRun(kotlin(
                //language=kotlin
                """
                        import dev.morphia.Datastore
                        import dev.morphia.query.filters.Filters.regex
                        import java.util.regex.Pattern.quote

                        class Regexes {
                            fun find(ds: Datastore) {
                                ds.find(Any::class.java)
                                  .filter(regex("name")
                                              .pattern(quote("'>   FISH BONES") + "$")
                                              )
                            }
                        }
                        """,
                //language=kotlin
                """
                        import dev.morphia.Datastore
                        import dev.morphia.query.filters.Filters.regex
                        import java.util.regex.Pattern.quote

                        class Regexes {
                            fun find(ds: Datastore) {
                                ds.find(Any::class.java)
                                  .filter(regex("name", quote("'>   FISH BONES") + "$")
                                              )
                            }
                        }
                        """));
    }

}