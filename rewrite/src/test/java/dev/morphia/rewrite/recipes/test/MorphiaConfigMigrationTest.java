package dev.morphia.rewrite.recipes.test;

import dev.morphia.rewrite.recipes.config.MorphiaConfigMigration;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.openrewrite.Recipe;

import static org.openrewrite.java.Assertions.java;

@SuppressWarnings("removal")
public class MorphiaConfigMigrationTest extends MorphiaRewriteTest {
    @Override
    protected @NotNull Recipe getRecipe() {
        return new MorphiaConfigMigration();
    }

    @Test
    void asAMethodParam() {
        rewriteRun(
                //language=java
                java(
                        """
                                import dev.morphia.mapping.MapperOptions;
                                import dev.morphia.mapping.NamingStrategy;

                                public class UnwrapTest {
                                    public void foo(MapperOptions options) {}

                                    public void update() {
                                        foo(MapperOptions.builder()
                                            .collectionNaming(NamingStrategy.camelCase())
                                            .build());
                                    }
                                }
                                """,
                        """
                                import dev.morphia.config.MorphiaConfig;
                                import dev.morphia.mapping.NamingStrategy;

                                public class UnwrapTest {
                                    public void foo(MorphiaConfig options) {}

                                    public void update() {
                                        foo( MorphiaConfig.load()
                                                .collectionNaming(NamingStrategy.camelCase()));
                                    }
                                }
                                """));
    }

    @Test
    void update() {
        rewriteRun(
                //language=java
                java(
                        """
                                import dev.morphia.mapping.MapperOptions;
                                import dev.morphia.mapping.NamingStrategy;

                                public class UnwrapTest {
                                    public void update() {
                                        MapperOptions options = MapperOptions.builder()
                                            .collectionNaming(NamingStrategy.camelCase())
                                            .build();
                                    }
                                }
                                """,
                        """
                                import dev.morphia.config.MorphiaConfig;
                                import dev.morphia.mapping.NamingStrategy;

                                public class UnwrapTest {
                                    public void update() {
                                        MorphiaConfig options = MorphiaConfig.load()
                                                .collectionNaming(NamingStrategy.camelCase());
                                    }
                                }
                                """));
    }
}
