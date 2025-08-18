package dev.morphia.rewrite.recipes.test.query.kotlin;

import dev.morphia.rewrite.recipes.UpdateReturnType;
import dev.morphia.rewrite.recipes.test.KotlinRewriteTest;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.openrewrite.Recipe;

import static org.openrewrite.kotlin.Assertions.kotlin;

class KotlinUpdateReturnTypeTest extends KotlinRewriteTest {

    @NotNull
    @Override
    protected Recipe getRecipe() {
        return new UpdateReturnType();
    }

    @Test
    public void testAssignmentUpdates() {
        rewriteRun(kotlin(
                //language=kotlin
                """
                        import dev.morphia.Datastore
                        import dev.morphia.query.Update
                        import org.bson.types.ObjectId
                        import java.time.LocalDateTime
                        import dev.morphia.query.filters.Filters.eq
                        import dev.morphia.query.updates.UpdateOperators.set

                        class ReturnTypes {
                            fun example(ds: Datastore) {
                                val result: Update<String> = ds.find(String::class.java).filter(eq("_id", ObjectId.get()))
                                  .disableValidation()
                                  .update(set("last_updated", LocalDateTime.now()))
                                  .execute()
                            }
                        }
                        """,
                //language=kotlin
                """
                        import com.mongodb.client.result.UpdateResult
                        import dev.morphia.Datastore
                        import dev.morphia.query.Update
                        import org.bson.types.ObjectId
                        import java.time.LocalDateTime
                        import dev.morphia.query.filters.Filters.eq
                        import dev.morphia.query.updates.UpdateOperators.set

                        class ReturnTypes {
                            fun example(ds: Datastore) {
                                val result: UpdateResult = ds.find(String::class.java).filter(eq("_id", ObjectId.get()))
                                    .disableValidation()
                                    .update(set("last_updated", LocalDateTime.now()))
                                    .execute()
                            }
                        }
                        """));
    }
}