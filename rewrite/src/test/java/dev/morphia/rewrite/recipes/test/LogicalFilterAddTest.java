package dev.morphia.rewrite.recipes.test;

import dev.morphia.rewrite.recipes.LogicalFilterAdd;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.openrewrite.Recipe;

import static org.junit.jupiter.api.Assertions.*;
import static org.openrewrite.java.Assertions.java;

public class LogicalFilterAddTest extends MorphiaRewriteTest {
    @Override
    protected @NotNull Recipe getRecipe() {
        return new LogicalFilterAdd();
    }

    @Test
    public void collapseLogicalFilter() {
        rewriteRun(java(
                //language:java
                """
                        import dev.morphia.Datastore;

                        import static dev.morphia.query.filters.Filters.gt;
                        import static dev.morphia.query.filters.Filters.lt;
                        import static dev.morphia.query.filters.Filters.or;

                        public class LogicalFilters {
                            public void filter(Datastore datastore) {
                                datastore.find(Object.class)
                                        .filter(or()
                                                .add(lt("budget", 10000))
                                                .add(gt("budget", 12)));
                            }
                        }
                        """,
                //language:java
                """
                        import dev.morphia.Datastore;

                        import static dev.morphia.query.filters.Filters.gt;
                        import static dev.morphia.query.filters.Filters.lt;
                        import static dev.morphia.query.filters.Filters.or;

                        public class LogicalFilters {
                            public void filter(Datastore datastore) {
                                datastore.find(Object.class)
                                        .filter(or(lt("budget", 10000), gt("budget", 12)));
                            }
                        }
                        """));
    }
}