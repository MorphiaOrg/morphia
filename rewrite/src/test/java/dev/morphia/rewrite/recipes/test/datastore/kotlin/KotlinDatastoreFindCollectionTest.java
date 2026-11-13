package dev.morphia.rewrite.recipes.test.datastore.kotlin;

import dev.morphia.rewrite.recipes.datastore.DatastoreFindCollection;
import dev.morphia.rewrite.recipes.test.KotlinRewriteTest;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.openrewrite.Recipe;

import static org.openrewrite.kotlin.Assertions.kotlin;

public class KotlinDatastoreFindCollectionTest extends KotlinRewriteTest {
    @Override
    protected @NotNull Recipe getRecipe() {
        return new DatastoreFindCollection();
    }

    @Test
    public void findWithCollection() {
        rewriteRun(kotlin(
                //language=kotlin
                """
                        import dev.morphia.Datastore
                        import dev.morphia.query.filters.Filters.eq

                        class Finds {
                            fun find(ds: Datastore, name: String): List<Any> {
                                ds.find("other", Any::class.java).first()
                                return ds.find(name, Any::class.java)
                                    .filter(eq("name", "value"))
                                    .iterator()
                                    .toList()
                            }
                        }
                        """,
                //language=kotlin
                """
                        import dev.morphia.Datastore
                        import dev.morphia.query.FindOptions
                        import dev.morphia.query.filters.Filters.eq

                        class Finds {
                            fun find(ds: Datastore, name: String): List<Any> {
                                ds.find(Any::class.java, FindOptions().collection("other")).first()
                                return ds.find(Any::class.java, FindOptions().collection(name))
                                    .filter(eq("name", "value"))
                                    .iterator()
                                    .toList()
                            }
                        }
                        """));
    }

    @Test
    public void otherFindsAreUnchanged() {
        rewriteRun(kotlin(
                //language=kotlin
                """
                        import dev.morphia.Datastore
                        import dev.morphia.query.FindOptions

                        class Finds {
                            fun find(ds: Datastore) {
                                ds.find(Any::class.java)
                                ds.find(Any::class.java, FindOptions())
                                ds.find<Any>("other")
                            }
                        }
                        """));
    }
}
