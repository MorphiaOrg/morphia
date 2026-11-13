package dev.morphia.rewrite.recipes.test.datastore;

import dev.morphia.rewrite.recipes.datastore.DatastoreFindCollection;
import dev.morphia.rewrite.recipes.test.MorphiaRewriteTest;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.openrewrite.Recipe;

import static org.openrewrite.java.Assertions.java;

public class DatastoreFindCollectionTest extends MorphiaRewriteTest {
    @Override
    protected @NotNull Recipe getRecipe() {
        return new DatastoreFindCollection();
    }

    @Test
    public void findWithCollection() {
        rewriteRun(java(
                //language=java
                """
                        import java.util.List;

                        import dev.morphia.Datastore;

                        import static dev.morphia.query.filters.Filters.eq;

                        public class Finds {
                            public List<Object> find(Datastore ds, String name) {
                                ds.find("other", Object.class).first();
                                return ds.find(name, Object.class)
                                         .filter(eq("name", "value"))
                                         .iterator()
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

                        public class Finds {
                            public List<Object> find(Datastore ds, String name) {
                                ds.find(Object.class, new FindOptions().collection("other")).first();
                                return ds.find(Object.class, new FindOptions().collection(name))
                                         .filter(eq("name", "value"))
                                         .iterator()
                                         .toList();
                            }
                        }
                        """));
    }

    @Test
    public void otherFindsAreUnchanged() {
        rewriteRun(java(
                //language=java
                """
                        import dev.morphia.Datastore;
                        import dev.morphia.query.FindOptions;

                        public class Finds {
                            public void find(Datastore ds) {
                                ds.find(Object.class);
                                ds.find(Object.class, new FindOptions());
                                ds.find("other");
                            }
                        }
                        """));
    }
}
