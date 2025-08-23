package dev.morphia.rewrite.recipes.test.kotlin;

import dev.morphia.rewrite.recipes.config.MorphiaConfigMigration;
import dev.morphia.rewrite.recipes.test.KotlinRewriteTest;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.openrewrite.Recipe;

import static org.openrewrite.kotlin.Assertions.kotlin;

@SuppressWarnings("removal")
public class KotlinMorphiaConfigMigrationTest extends KotlinRewriteTest {
    @Override
    protected @NotNull Recipe getRecipe() {
        return new MorphiaConfigMigration();
    }

    @Test
    void asAMethodParam() {
        rewriteRun(
                //language=kotlin
                kotlin(
                        """
                                import dev.morphia.mapping.MapperOptions
                                import dev.morphia.mapping.NamingStrategy

                                class UnwrapTest {
                                    fun foo(options: MapperOptions) {}

                                    fun update() {
                                        foo(MapperOptions.builder()
                                            .collectionNaming(NamingStrategy.camelCase())
                                            .build())
                                    }
                                }
                                """,
                        """
                                import dev.morphia.config.MorphiaConfig
                                import dev.morphia.mapping.NamingStrategy

                                class UnwrapTest {
                                    fun foo(options: MorphiaConfig) {}

                                    fun update() {
                                        foo( MorphiaConfig.load()
                                            .collectionNaming(NamingStrategy.camelCase()))
                                    }
                                }
                                """,
                        //language=kotlin
                        spec -> spec.afterRecipe(cu -> {
                        })));
    }

    @Test
    void update() {
        rewriteRun(
                //language=kotlin
                kotlin(
                        """
                                import dev.morphia.mapping.MapperOptions
                                import dev.morphia.mapping.NamingStrategy

                                class UnwrapTest {
                                    fun update() {
                                        val options: MapperOptions = MapperOptions.builder()
                                            .collectionNaming(NamingStrategy.camelCase())
                                            .build()
                                    }
                                }
                                """,
                        """
                                import dev.morphia.config.MorphiaConfig
                                import dev.morphia.mapping.NamingStrategy

                                class UnwrapTest {
                                    fun update() {
                                        val options: MorphiaConfig = MorphiaConfig.load()
                                            .collectionNaming(NamingStrategy.camelCase())
                                    }
                                }
                                """,
                        //language=kotlin
                        spec -> spec.afterRecipe(cu -> {
                        })));
    }
}