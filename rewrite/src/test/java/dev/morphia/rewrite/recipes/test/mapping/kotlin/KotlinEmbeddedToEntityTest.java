package dev.morphia.rewrite.recipes.test.mapping.kotlin;

import dev.morphia.rewrite.recipes.mapping.EmbeddedToEntity;
import dev.morphia.rewrite.recipes.test.KotlinRewriteTest;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.openrewrite.Recipe;

import static org.openrewrite.kotlin.Assertions.kotlin;

public class KotlinEmbeddedToEntityTest extends KotlinRewriteTest {
    @Override
    protected @NotNull Recipe getRecipe() {
        return new EmbeddedToEntity();
    }

    @Test
    public void embeddedAlone() {
        rewriteRun(kotlin(
                //language=kotlin
                """
                        import dev.morphia.annotations.Embedded

                        @Embedded
                        class Address {
                            var street: String? = null
                        }
                        """,
                //language=kotlin
                """
                        import dev.morphia.annotations.Entity

                        @Entity
                        class Address {
                            var street: String? = null
                        }
                        """));
    }

    @Test
    public void embeddedWithUseDiscriminator() {
        rewriteRun(kotlin(
                //language=kotlin
                """
                        import dev.morphia.annotations.Embedded

                        @Embedded(useDiscriminator = false)
                        class Address
                        """,
                //language=kotlin
                """
                        import dev.morphia.annotations.Entity

                        @Entity(useDiscriminator = false)
                        class Address
                        """));
    }

    @Test
    public void embeddedWithDiscriminatorKey() {
        rewriteRun(kotlin(
                //language=kotlin
                """
                        import dev.morphia.annotations.Embedded

                        @Embedded(discriminatorKey = "type")
                        class Address
                        """,
                //language=kotlin
                """
                        import dev.morphia.annotations.Entity

                        @Entity(discriminatorKey = "type")
                        class Address
                        """));
    }

    @Test
    public void embeddedWithDiscriminator() {
        rewriteRun(kotlin(
                //language=kotlin
                """
                        import dev.morphia.annotations.Embedded

                        @Embedded(discriminator = "addr")
                        class Address
                        """,
                //language=kotlin
                """
                        import dev.morphia.annotations.Entity

                        @Entity(discriminator = "addr")
                        class Address
                        """));
    }

    @Test
    public void embeddedWithValue() {
        rewriteRun(kotlin(
                //language=kotlin
                """
                        import dev.morphia.annotations.Embedded

                        @Embedded("addresses")
                        class Address
                        """,
                //language=kotlin
                """
                        import dev.morphia.annotations.Entity

                        @Entity
                        class Address
                        """));
    }

    @Test
    public void embeddedWithAllAttributes() {
        rewriteRun(kotlin(
                //language=kotlin
                """
                        import dev.morphia.annotations.Embedded

                        @Embedded(value = "addresses", useDiscriminator = false, discriminatorKey = "type", discriminator = "addr")
                        class Address
                        """,
                //language=kotlin
                """
                        import dev.morphia.annotations.Entity

                        @Entity(useDiscriminator = false, discriminatorKey = "type", discriminator = "addr")
                        class Address
                        """));
    }

    @Test
    public void embeddedAndEntity() {
        rewriteRun(kotlin(
                //language=kotlin
                """
                        import dev.morphia.annotations.Embedded
                        import dev.morphia.annotations.Entity

                        @Entity
                        @Embedded(discriminator = "addr")
                        class Address
                        """,
                //language=kotlin
                """
                        import dev.morphia.annotations.Entity

                        @Entity(discriminator = "addr")
                        class Address
                        """));
    }

    @Test
    public void embeddedAndEntityMerged() {
        rewriteRun(kotlin(
                //language=kotlin
                """
                        import dev.morphia.annotations.Embedded
                        import dev.morphia.annotations.Entity

                        @Embedded(discriminatorKey = "type", discriminator = "addr")
                        @Entity(value = "addresses", discriminator = "addr")
                        class Address
                        """,
                //language=kotlin
                """
                        import dev.morphia.annotations.Entity

                        @Entity(value = "addresses", discriminator = "addr", discriminatorKey = "type")
                        class Address
                        """));
    }

    @Test
    public void embeddedAndEntityConflict() {
        rewriteRun(kotlin(
                //language=kotlin
                """
                        import dev.morphia.annotations.Embedded
                        import dev.morphia.annotations.Entity

                        @Entity(discriminator = "address")
                        @Embedded(useDiscriminator = false, discriminator = "addr")
                        class Address
                        """,
                //language=kotlin
                """
                        import dev.morphia.annotations.Entity

                        /*~~(@Embedded also set discriminator to a different value, which was dropped in favor of @Entity's)~~>*/@Entity(discriminator = "address", useDiscriminator = false)
                        class Address
                        """));
    }

    @Test
    public void embeddedOnField() {
        rewriteRun(kotlin(
                //language=kotlin
                """
                        import dev.morphia.annotations.Embedded
                        import dev.morphia.annotations.Entity

                        @Entity
                        class Person {
                            @Embedded
                            var address: Any? = null
                        }
                        """,
                //language=kotlin
                """
                        import dev.morphia.annotations.Entity

                        @Entity
                        class Person {
                            var address: Any? = null
                        }
                        """));
    }

    @Test
    public void embeddedNested() {
        rewriteRun(kotlin(
                //language=kotlin
                """
                        import dev.morphia.annotations.Embedded
                        import dev.morphia.annotations.Entity
                        import dev.morphia.annotations.Id

                        @Entity("people")
                        class Person {
                            @Id
                            var id: String? = null

                            @Embedded(useDiscriminator = false)
                            class Address
                        }
                        """,
                //language=kotlin
                """
                        import dev.morphia.annotations.Entity
                        import dev.morphia.annotations.Id

                        @Entity("people")
                        class Person {
                            @Id
                            var id: String? = null

                            @Entity(useDiscriminator = false)
                            class Address
                        }
                        """));
    }

    @Test
    public void otherEntityImported() {
        rewriteRun(
                kotlin(
                        //language=kotlin
                        """
                                package other

                                annotation class Entity
                                """),
                kotlin(
                        //language=kotlin
                        """
                                import dev.morphia.annotations.Embedded
                                import other.Entity

                                @Embedded(discriminator = "addr")
                                class Address
                                """,
                        //language=kotlin
                        """
                                import other.Entity

                                @dev.morphia.annotations.Entity(discriminator = "addr")
                                class Address
                                """));
    }

    @Test
    public void embeddedAndEntityWithUnnamedValue() {
        rewriteRun(kotlin(
                //language=kotlin
                """
                        import dev.morphia.annotations.Embedded
                        import dev.morphia.annotations.Entity

                        @Entity("addresses")
                        @Embedded(useDiscriminator = false)
                        class Address
                        """,
                //language=kotlin
                """
                        import dev.morphia.annotations.Entity

                        @Entity(value = "addresses", useDiscriminator = false)
                        class Address
                        """));
    }

    @Test
    public void embeddedAfterModifiers() {
        rewriteRun(kotlin(
                //language=kotlin
                """
                        import dev.morphia.annotations.Embedded

                        public @Embedded(useDiscriminator = false) class Address
                        """,
                //language=kotlin
                """
                        import dev.morphia.annotations.Entity

                        public @Entity(useDiscriminator = false) class Address
                        """));
    }

    @Test
    public void entityAfterModifiers() {
        rewriteRun(kotlin(
                //language=kotlin
                """
                        import dev.morphia.annotations.Embedded
                        import dev.morphia.annotations.Entity

                        @Embedded(discriminator = "addr")
                        public @Entity class Address
                        """,
                //language=kotlin
                """
                        import dev.morphia.annotations.Entity

                        public @Entity(discriminator = "addr") class Address
                        """));
    }

    @Test
    public void typeNamedEntity() {
        rewriteRun(kotlin(
                //language=kotlin
                """
                        import dev.morphia.annotations.Embedded

                        @Embedded(useDiscriminator = false)
                        class Entity
                        """,
                //language=kotlin
                """
                        @dev.morphia.annotations.Entity(useDiscriminator = false)
                        class Entity
                        """));
    }

    @Test
    public void nestedTypeNamedEntity() {
        rewriteRun(kotlin(
                //language=kotlin
                """
                        import dev.morphia.annotations.Embedded

                        class Outer {
                            @Embedded
                            class Address

                            class Entity
                        }
                        """,
                //language=kotlin
                """
                        class Outer {
                            @dev.morphia.annotations.Entity
                            class Address

                            class Entity
                        }
                        """));
    }

    @Test
    public void typeParameterNamedEntity() {
        rewriteRun(kotlin(
                //language=kotlin
                """
                        import dev.morphia.annotations.Embedded

                        class Outer<Entity> {
                            @Embedded
                            inner class Address
                        }
                        """,
                //language=kotlin
                """
                        class Outer<Entity> {
                            @dev.morphia.annotations.Entity
                            inner class Address
                        }
                        """));
    }

    @Test
    public void importAliasedToEntity() {
        rewriteRun(kotlin(
                //language=kotlin
                """
                        import dev.morphia.annotations.Embedded
                        import dev.morphia.annotations.Id as Entity

                        @Embedded
                        class Address
                        """,
                //language=kotlin
                """
                        import dev.morphia.annotations.Id as Entity

                        @dev.morphia.annotations.Entity
                        class Address
                        """));
    }
}
