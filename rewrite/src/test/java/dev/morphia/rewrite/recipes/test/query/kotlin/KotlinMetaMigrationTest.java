package dev.morphia.rewrite.recipes.test.query.kotlin;

import dev.morphia.rewrite.recipes.query.MetaMigration;
import dev.morphia.rewrite.recipes.test.KotlinRewriteTest;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.openrewrite.Recipe;

import static org.openrewrite.kotlin.Assertions.kotlin;

public class KotlinMetaMigrationTest extends KotlinRewriteTest {

    @Override
    protected @NotNull Recipe getRecipe() {
        return new MetaMigration();
    }

    @Test
    public void testKeywordString() {
        rewriteRun(kotlin(
                //language=kotlin
                """
                        import dev.morphia.query.Meta

                        class Upgrades {
                            fun score(field: String): Meta {
                                return Meta("searchScore", field)
                            }
                        }
                        """,
                //language=kotlin
                """
                        import dev.morphia.query.Meta

                        class Upgrades {
                            fun score(field: String): Meta {
                                return Meta.searchScore(field)
                            }
                        }
                        """));
    }

    @Test
    public void testTextScoreNoArgs() {
        rewriteRun(kotlin(
                //language=kotlin
                """
                        import dev.morphia.query.Meta

                        class Upgrades {
                            fun score(): Meta {
                                return Meta.textScore()
                            }
                        }
                        """,
                //language=kotlin
                """
                        import dev.morphia.query.Meta

                        class Upgrades {
                            fun score(): Meta {
                                return Meta.textScore("textScore")
                            }
                        }
                        """));
    }

    @Test
    public void testKeywordEnum() {
        rewriteRun(kotlin(
                //language=kotlin
                """
                        import dev.morphia.query.Meta
                        import dev.morphia.query.Meta.MetaDataKeyword

                        class Upgrades {
                            fun keyword(): Meta {
                                return Meta(MetaDataKeyword.textScore)
                            }

                            fun keywordAndField(): Meta {
                                return Meta(Meta.MetaDataKeyword.textScore, "score")
                            }
                        }
                        """,
                //language=kotlin
                """
                        import dev.morphia.query.Meta

                        class Upgrades {
                            fun keyword(): Meta {
                                return Meta.textScore("textScore")
                            }

                            fun keywordAndField(): Meta {
                                return Meta.textScore("score")
                            }
                        }
                        """));
    }

    @Test
    public void testComputedKeywordIsLeftAlone() {
        rewriteRun(kotlin(
                //language=kotlin
                """
                        import dev.morphia.query.Meta
                        import dev.morphia.query.Meta.MetaDataKeyword

                        class Upgrades {
                            val textScore: MetaDataKeyword = MetaDataKeyword.textScore

                            fun holder(): Upgrades {
                                return this
                            }

                            fun field(): Meta {
                                return Meta(holder().textScore, "score")
                            }

                            fun meta(keyword: String): Meta {
                                return Meta(keyword, "field")
                            }

                            fun unknown(): Meta {
                                return Meta("unknown", "field")
                            }
                        }
                        """));
    }
}
