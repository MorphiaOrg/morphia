package dev.morphia.rewrite.recipes.test.query.kotlin;

import dev.morphia.rewrite.recipes.LogicalFilterAdd;
import dev.morphia.rewrite.recipes.test.KotlinRewriteTest;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.openrewrite.Recipe;

import static org.openrewrite.kotlin.Assertions.kotlin;

public class KotlinLogicalFilterAddTest extends KotlinRewriteTest {
    @Override
    protected @NotNull Recipe getRecipe() {
        return new LogicalFilterAdd();
    }

    @Test
    public void collapseLogicalFilter() {
        rewriteRun(kotlin(
                //language=kotlin
                """
                        import dev.morphia.Datastore
                        import dev.morphia.query.filters.Filters.gt
                        import dev.morphia.query.filters.Filters.lt
                        import dev.morphia.query.filters.Filters.or

                        class LogicalFilters {
                            fun filter(datastore: Datastore) {
                                datastore.find(Any::class.java)
                                    .filter(or()
                                        .add(lt("budget", 10000))
                                        .add(gt("budget", 12)))
                            }
                        }
                        """,
                //language=kotlin
                """
                        import dev.morphia.Datastore
                        import dev.morphia.query.filters.Filters.gt
                        import dev.morphia.query.filters.Filters.lt
                        import dev.morphia.query.filters.Filters.or

                        class LogicalFilters {
                            fun filter(datastore: Datastore) {
                                datastore.find(Any::class.java)
                                    .filter(or(lt("budget", 10000), gt("budget", 12)))
                            }
                        }
                        """));
    }
}