package dev.morphia.rewrite.recipes.test;

import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.openrewrite.Recipe;
import org.openrewrite.config.CompositeRecipe;
import org.openrewrite.config.Environment;

import static org.openrewrite.java.Assertions.java;

/**
 * Runs the full {@code dev.morphia.UpgradeToMorphia30} recipe to check that the query and datastore recipes are
 * ordered so that each one's output is picked up by the next.
 */
public class QueryDatastoreUpgradeTest extends MorphiaRewriteTest {
    @Override
    protected @NotNull Recipe getRecipe() {
        return upgradeRecipe();
    }

    public static @NotNull Recipe upgradeRecipe() {
        Recipe upgrade = Environment.builder()
                .scanRuntimeClasspath()
                .build()
                .activateRecipes("dev.morphia.UpgradeToMorphia30");
        // the Maven recipes aren't on the test classpath, so run the rest of the list, in order, without them
        while (!upgrade.getName().equals("dev.morphia.UpgradeToMorphia30")) {
            upgrade = upgrade.getRecipeList().get(0);
        }
        List<Recipe> recipes = upgrade.getRecipeList().stream()
                .filter(recipe -> !recipe.getName().startsWith("org.openrewrite.maven"))
                .toList();
        return new CompositeRecipe(recipes);
    }

    @Test
    public void executeWithOptionsMovesToFind() {
        rewriteRun(java(
                //language=java
                """
                        import java.util.List;

                        import dev.morphia.Datastore;
                        import dev.morphia.query.FindOptions;

                        import static dev.morphia.query.filters.Filters.eq;

                        public class Executes {
                            public List<Object> execute(Datastore ds) {
                                return ds.find(Object.class)
                                         .filter(eq("name", "value"))
                                         .execute(new FindOptions().limit(1))
                                         .toList();
                            }
                        }
                        """,
                //language=java
                """
                        import java.util.List;

                        import dev.morphia.Datastore;
                        import dev.morphia.query.FindOptions;

                        import static dev.morphia.query.filters.Filters.eq;

                        public class Executes {
                            public List<Object> execute(Datastore ds) {
                                return ds.find(Object.class, new FindOptions().limit(1))
                                        .filter(eq("name", "value"))
                                        .iterator()
                                        .toList();
                            }
                        }
                        """));
    }

    @Test
    public void alternateCollectionMergesWithMovedOptions() {
        rewriteRun(java(
                //language=java
                """
                        import java.util.List;

                        import dev.morphia.Datastore;
                        import dev.morphia.query.FindOptions;

                        import static dev.morphia.query.filters.Filters.eq;

                        public class Executes {
                            public Object execute(Datastore ds, FindOptions options) {
                                ds.find("other", Object.class)
                                  .filter(eq("name", "value"))
                                  .execute(new FindOptions().limit(1))
                                  .toList();
                                return ds.find("other", Object.class)
                                         .first(options);
                            }
                        }
                        """,
                //language=java
                """
                        import java.util.List;

                        import dev.morphia.Datastore;
                        import dev.morphia.query.FindOptions;

                        import static dev.morphia.query.filters.Filters.eq;

                        public class Executes {
                            public Object execute(Datastore ds, FindOptions options) {
                                ds.find(Object.class, new FindOptions().limit(1).collection("other"))
                                        .filter(eq("name", "value"))
                                        .iterator()
                                        .toList();
                                return ds.find(Object.class, new FindOptions().collection("other"))
                                         .first(options);
                            }
                        }
                        """));
    }

    @Test
    public void conflictingOptionsAreLeftForManualMigration() {
        rewriteRun(java(
                //language=java
                """
                        import dev.morphia.Datastore;
                        import dev.morphia.query.FindOptions;

                        public class Executes {
                            public Object execute(Datastore ds) {
                                return ds.find(Object.class, new FindOptions().skip(10))
                                         .first(new FindOptions().limit(1));
                            }
                        }
                        """));
    }

    @Test
    public void datastoreOverloads() {
        rewriteRun(java(
                //language=java
                """
                        import com.mongodb.WriteConcern;
                        import dev.morphia.Datastore;
                        import dev.morphia.InsertOptions;
                        import dev.morphia.query.Query;

                        public class Overloads {
                            public void update(Datastore ds, Query<Object> query, Object entity) {
                                ds.delete(query);
                                ds.merge(entity, WriteConcern.MAJORITY);
                                ds.save(entity, new InsertOptions().writeConcern(WriteConcern.MAJORITY));
                            }
                        }
                        """,
                //language=java
                """
                        import com.mongodb.WriteConcern;
                        import dev.morphia.Datastore;
                        import dev.morphia.InsertOneOptions;
                        import dev.morphia.query.Query;

                        public class Overloads {
                            public void update(Datastore ds, Query<Object> query, Object entity) {
                                query.delete();
                                ds.merge(entity, new InsertOneOptions().writeConcern(WriteConcern.MAJORITY));
                                ds.save(entity, new InsertOneOptions().writeConcern(WriteConcern.MAJORITY));
                            }
                        }
                        """));
    }

    @Test
    public void movedCollectionTakesPrecedence() {
        rewriteRun(java(
                //language=java
                """
                        import dev.morphia.Datastore;
                        import dev.morphia.query.FindOptions;

                        public class Executes {
                            public Object execute(Datastore ds) {
                                return ds.find("base", Object.class)
                                         .first(new FindOptions().collection("override"));
                            }
                        }
                        """,
                //language=java
                """
                        import dev.morphia.Datastore;
                        import dev.morphia.query.FindOptions;

                        public class Executes {
                            public Object execute(Datastore ds) {
                                return ds.find(Object.class, new FindOptions().collection("override"))
                                        .first();
                            }
                        }
                        """));
    }
}
