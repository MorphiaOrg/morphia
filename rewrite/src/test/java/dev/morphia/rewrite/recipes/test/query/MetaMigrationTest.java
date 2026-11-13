package dev.morphia.rewrite.recipes.test.query;

import dev.morphia.rewrite.recipes.query.MetaMigration;
import dev.morphia.rewrite.recipes.test.MorphiaRewriteTest;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.openrewrite.Recipe;

import static org.openrewrite.java.Assertions.java;

public class MetaMigrationTest extends MorphiaRewriteTest {

    @Override
    protected @NotNull Recipe getRecipe() {
        return new MetaMigration();
    }

    @Test
    public void testKeywordString() {
        rewriteRun(java(
                //language=java
                """
                        import dev.morphia.query.Meta;

                        public class Upgrades {
                            public Meta indexKey() {
                                return new Meta("indexKey", "key");
                            }

                            public Meta score(String field) {
                                return new Meta("searchScore", field);
                            }
                        }
                        """,
                //language=java
                """
                        import dev.morphia.query.Meta;

                        public class Upgrades {
                            public Meta indexKey() {
                                return Meta.indexKey("key");
                            }

                            public Meta score(String field) {
                                return Meta.searchScore(field);
                            }
                        }
                        """));
    }

    @Test
    public void testKeywordEnum() {
        rewriteRun(java(
                //language=java
                """
                        import dev.morphia.query.Meta;
                        import dev.morphia.query.Meta.MetaDataKeyword;

                        public class Upgrades {
                            public Meta keyword() {
                                return new Meta(MetaDataKeyword.textScore);
                            }

                            public Meta keywordAndField() {
                                return new Meta(Meta.MetaDataKeyword.textScore, "score");
                            }
                        }
                        """,
                //language=java
                """
                        import dev.morphia.query.Meta;

                        public class Upgrades {
                            public Meta keyword() {
                                return Meta.textScore("textScore");
                            }

                            public Meta keywordAndField() {
                                return Meta.textScore("score");
                            }
                        }
                        """));
    }

    @Test
    public void testTextScoreNoArgs() {
        rewriteRun(java(
                //language=java
                """
                        import dev.morphia.query.Meta;

                        public class Upgrades {
                            public Meta score() {
                                return Meta.textScore();
                            }
                        }
                        """,
                //language=java
                """
                        import dev.morphia.query.Meta;

                        public class Upgrades {
                            public Meta score() {
                                return Meta.textScore("textScore");
                            }
                        }
                        """));
    }

    @Test
    public void testComputedKeywordIsLeftAlone() {
        rewriteRun(java(
                //language=java
                """
                        import dev.morphia.query.Meta;
                        import dev.morphia.query.Meta.MetaDataKeyword;

                        public class Upgrades {
                            private MetaDataKeyword textScore = MetaDataKeyword.textScore;

                            public Upgrades holder() {
                                return this;
                            }

                            public Meta field() {
                                return new Meta(holder().textScore, "score");
                            }

                            public Meta qualifiedConstant() {
                                return new Meta(keywordHolder().textScore, "score");
                            }

                            public Meta qualifiedFactory() {
                                return loadMeta().textScore();
                            }

                            public MetaDataKeyword keywordHolder() {
                                return MetaDataKeyword.textScore;
                            }

                            public Meta loadMeta() {
                                return Meta.textScore("score");
                            }

                            public Meta meta(String keyword) {
                                return new Meta(keyword, "field");
                            }

                            public Meta unknown() {
                                return new Meta("unknown", "field");
                            }
                        }
                        """));
    }
}
