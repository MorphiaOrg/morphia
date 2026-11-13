package dev.morphia.rewrite.recipes.test.datastore.kotlin;

import dev.morphia.rewrite.recipes.datastore.FindAndModifyOptionsMigration;
import dev.morphia.rewrite.recipes.test.KotlinRewriteTest;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.openrewrite.Recipe;

import static org.openrewrite.kotlin.Assertions.kotlin;

public class KotlinFindAndModifyOptionsMigrationTest extends KotlinRewriteTest {

    @Override
    protected @NotNull Recipe getRecipe() {
        return new FindAndModifyOptionsMigration();
    }

    @Test
    public void testReturnNewLiteral() {
        rewriteRun(kotlin(
                //language=kotlin
                """
                        import dev.morphia.FindAndModifyOptions
                        import dev.morphia.ModifyOptions

                        class Upgrades {
                            fun after(): ModifyOptions {
                                return FindAndModifyOptions().returnNew(true)
                            }
                        }
                        """,
                //language=kotlin
                """
                        import com.mongodb.client.model.ReturnDocument
                        import dev.morphia.ModifyOptions

                        class Upgrades {
                            fun after(): ModifyOptions {
                                return ModifyOptions().returnDocument(ReturnDocument.AFTER)
                            }
                        }
                        """));
    }

    @Test
    public void testImplicitReceiver() {
        rewriteRun(kotlin(
                //language=kotlin
                """
                        import dev.morphia.FindAndModifyOptions

                        class Upgrades {
                            fun options(): FindAndModifyOptions {
                                return FindAndModifyOptions().apply { returnNew(false) }
                            }
                        }
                        """,
                //language=kotlin
                """
                        import com.mongodb.client.model.ReturnDocument
                        import dev.morphia.ModifyOptions

                        class Upgrades {
                            fun options(): ModifyOptions {
                                return ModifyOptions().apply { returnDocument(ReturnDocument.BEFORE) }
                            }
                        }
                        """));
    }

    @Test
    public void testReturnNewExpression() {
        // a computed flag can't be rewritten in Kotlin, but the type still changes so the compiler points at the call
        rewriteRun(kotlin(
                //language=kotlin
                """
                        import dev.morphia.FindAndModifyOptions

                        class Upgrades {
                            fun options(flag: Boolean): FindAndModifyOptions {
                                return FindAndModifyOptions().returnNew(flag)
                            }
                        }
                        """,
                //language=kotlin
                """
                        import dev.morphia.ModifyOptions

                        class Upgrades {
                            fun options(flag: Boolean): ModifyOptions {
                                return ModifyOptions().returnNew(flag)
                            }
                        }
                        """));
    }

    @Test
    public void testTypeOnly() {
        rewriteRun(kotlin(
                //language=kotlin
                """
                        import dev.morphia.FindAndModifyOptions

                        class Upgrades {
                            fun options(): FindAndModifyOptions {
                                return FindAndModifyOptions()
                            }
                        }
                        """,
                //language=kotlin
                """
                        import dev.morphia.ModifyOptions

                        class Upgrades {
                            fun options(): ModifyOptions {
                                return ModifyOptions()
                            }
                        }
                        """));
    }
}
