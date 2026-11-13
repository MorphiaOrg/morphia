package dev.morphia.rewrite.recipes.test.query.kotlin;

import dev.morphia.rewrite.recipes.query.QueryExecute;
import dev.morphia.rewrite.recipes.test.KotlinRewriteTest;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.openrewrite.Recipe;

import static org.openrewrite.kotlin.Assertions.kotlin;

public class KotlinQueryExecuteTest extends KotlinRewriteTest {
    @Override
    protected @NotNull Recipe getRecipe() {
        return new QueryExecute();
    }

    @Test
    public void execute() {
        rewriteRun(kotlin(
                //language=kotlin
                """
                        import dev.morphia.Datastore
                        import dev.morphia.query.FindOptions
                        import dev.morphia.query.Query

                        class Executes {
                            fun execute(ds: Datastore, query: Query<Any>): List<Any> {
                                val cursor = query.execute(FindOptions().limit(1))
                                return ds.find(Any::class.java).execute().toList()
                            }
                        }
                        """,
                //language=kotlin
                """
                        import dev.morphia.Datastore
                        import dev.morphia.query.FindOptions
                        import dev.morphia.query.Query

                        class Executes {
                            fun execute(ds: Datastore, query: Query<Any>): List<Any> {
                                val cursor = query.iterator(FindOptions().limit(1))
                                return ds.find(Any::class.java).iterator().toList()
                            }
                        }
                        """));
    }
}
