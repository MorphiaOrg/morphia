package dev.morphia.rewrite.recipes.test.datastore;

import dev.morphia.rewrite.recipes.datastore.FindAndModifyOptionsMigration;
import dev.morphia.rewrite.recipes.test.MorphiaRewriteTest;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.openrewrite.Recipe;

import static org.openrewrite.java.Assertions.java;

public class FindAndModifyOptionsMigrationTest extends MorphiaRewriteTest {

    @Override
    protected @NotNull Recipe getRecipe() {
        return new FindAndModifyOptionsMigration();
    }

    @Test
    public void testReturnNewLiteral() {
        rewriteRun(java(
                //language=java
                """
                        import dev.morphia.FindAndModifyOptions;
                        import dev.morphia.ModifyOptions;

                        public class Upgrades {
                            public ModifyOptions after() {
                                return new FindAndModifyOptions()
                                    .returnNew(true)
                                    .upsert(true);
                            }

                            public FindAndModifyOptions before() {
                                return new FindAndModifyOptions().returnNew(false);
                            }
                        }
                        """,
                //language=java
                """
                        import com.mongodb.client.model.ReturnDocument;
                        import dev.morphia.ModifyOptions;

                        public class Upgrades {
                            public ModifyOptions after() {
                                return new ModifyOptions()
                                    .returnDocument(ReturnDocument.AFTER)
                                    .upsert(true);
                            }

                            public ModifyOptions before() {
                                return new ModifyOptions().returnDocument(ReturnDocument.BEFORE);
                            }
                        }
                        """));
    }

    @Test
    public void testReturnNewExpression() {
        rewriteRun(java(
                //language=java
                """
                        import dev.morphia.FindAndModifyOptions;

                        public class Upgrades {
                            public FindAndModifyOptions options(boolean returnNew) {
                                FindAndModifyOptions options = new FindAndModifyOptions();
                                return options.returnNew(returnNew);
                            }
                        }
                        """,
                //language=java
                """
                        import com.mongodb.client.model.ReturnDocument;
                        import dev.morphia.ModifyOptions;

                        public class Upgrades {
                            public ModifyOptions options(boolean returnNew) {
                                ModifyOptions options = new ModifyOptions();
                                return options.returnDocument(returnNew ? ReturnDocument.AFTER : ReturnDocument.BEFORE);
                            }
                        }
                        """));
    }

    @Test
    public void testTypeOnly() {
        rewriteRun(java(
                //language=java
                """
                        import dev.morphia.FindAndModifyOptions;

                        public class Upgrades {
                            public FindAndModifyOptions options() {
                                return new FindAndModifyOptions();
                            }
                        }
                        """,
                //language=java
                """
                        import dev.morphia.ModifyOptions;

                        public class Upgrades {
                            public ModifyOptions options() {
                                return new ModifyOptions();
                            }
                        }
                        """));
    }
}
