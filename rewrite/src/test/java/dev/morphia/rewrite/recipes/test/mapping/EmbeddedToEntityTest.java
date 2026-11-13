package dev.morphia.rewrite.recipes.test.mapping;

import dev.morphia.rewrite.recipes.mapping.EmbeddedToEntity;
import dev.morphia.rewrite.recipes.test.MorphiaRewriteTest;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.openrewrite.Recipe;

import static org.openrewrite.java.Assertions.java;

public class EmbeddedToEntityTest extends MorphiaRewriteTest {
    @Override
    protected @NotNull Recipe getRecipe() {
        return new EmbeddedToEntity();
    }

    @Test
    public void embeddedAlone() {
        rewriteRun(java(
                //language=java
                """
                        import dev.morphia.annotations.Embedded;

                        @Embedded
                        public class Address {
                            private String street;
                        }
                        """,
                //language=java
                """
                        import dev.morphia.annotations.Entity;

                        @Entity
                        public class Address {
                            private String street;
                        }
                        """));
    }

    @Test
    public void embeddedWithUseDiscriminator() {
        rewriteRun(java(
                //language=java
                """
                        import dev.morphia.annotations.Embedded;

                        @Embedded(useDiscriminator = false)
                        public class Address {
                        }
                        """,
                //language=java
                """
                        import dev.morphia.annotations.Entity;

                        @Entity(useDiscriminator = false)
                        public class Address {
                        }
                        """));
    }

    @Test
    public void embeddedWithDiscriminatorKey() {
        rewriteRun(java(
                //language=java
                """
                        import dev.morphia.annotations.Embedded;

                        @Embedded(discriminatorKey = "type")
                        public class Address {
                        }
                        """,
                //language=java
                """
                        import dev.morphia.annotations.Entity;

                        @Entity(discriminatorKey = "type")
                        public class Address {
                        }
                        """));
    }

    @Test
    public void embeddedWithDiscriminator() {
        rewriteRun(java(
                //language=java
                """
                        import dev.morphia.annotations.Embedded;

                        @Embedded(discriminator = "addr")
                        public class Address {
                        }
                        """,
                //language=java
                """
                        import dev.morphia.annotations.Entity;

                        @Entity(discriminator = "addr")
                        public class Address {
                        }
                        """));
    }

    @Test
    public void embeddedWithValue() {
        rewriteRun(java(
                //language=java
                """
                        import dev.morphia.annotations.Embedded;

                        @Embedded("addresses")
                        public class Address {
                        }
                        """,
                //language=java
                """
                        import dev.morphia.annotations.Entity;

                        @Entity
                        public class Address {
                        }
                        """));
    }

    @Test
    public void embeddedWithAllAttributes() {
        rewriteRun(java(
                //language=java
                """
                        import dev.morphia.annotations.Embedded;

                        @Embedded(value = "addresses", useDiscriminator = false, discriminatorKey = "type", discriminator = "addr")
                        public class Address {
                        }
                        """,
                //language=java
                """
                        import dev.morphia.annotations.Entity;

                        @Entity(useDiscriminator = false, discriminatorKey = "type", discriminator = "addr")
                        public class Address {
                        }
                        """));
    }

    @Test
    public void embeddedAndEntity() {
        rewriteRun(java(
                //language=java
                """
                        import dev.morphia.annotations.Embedded;
                        import dev.morphia.annotations.Entity;

                        @Entity
                        @Embedded(discriminator = "addr")
                        public class Address {
                        }
                        """,
                //language=java
                """
                        import dev.morphia.annotations.Entity;

                        @Entity(discriminator = "addr")
                        public class Address {
                        }
                        """));
    }

    @Test
    public void embeddedAndEntityMerged() {
        rewriteRun(java(
                //language=java
                """
                        import dev.morphia.annotations.Embedded;
                        import dev.morphia.annotations.Entity;

                        @Embedded(discriminatorKey = "type", discriminator = "addr")
                        @Entity(value = "addresses", discriminator = "addr")
                        public class Address {
                        }
                        """,
                //language=java
                """
                        import dev.morphia.annotations.Entity;

                        @Entity(value = "addresses", discriminator = "addr", discriminatorKey = "type")
                        public class Address {
                        }
                        """));
    }

    @Test
    public void embeddedAndEntityConflict() {
        rewriteRun(java(
                //language=java
                """
                        import dev.morphia.annotations.Embedded;
                        import dev.morphia.annotations.Entity;

                        @Entity(discriminator = "address")
                        @Embedded(useDiscriminator = false, discriminator = "addr")
                        public class Address {
                        }
                        """,
                //language=java
                """
                        import dev.morphia.annotations.Entity;

                        /*~~(@Embedded also set discriminator to a different value, which was dropped in favor of @Entity's)~~>*/@Entity(discriminator = "address", useDiscriminator = false)
                        public class Address {
                        }
                        """));
    }

    @Test
    public void embeddedOnField() {
        rewriteRun(java(
                //language=java
                """
                        import dev.morphia.annotations.Embedded;
                        import dev.morphia.annotations.Entity;

                        @Entity
                        public class Person {
                            @Embedded
                            private Object address;
                        }
                        """,
                //language=java
                """
                        import dev.morphia.annotations.Entity;

                        @Entity
                        public class Person {
                            private Object address;
                        }
                        """));
    }

    @Test
    public void embeddedNested() {
        rewriteRun(java(
                //language=java
                """
                        import dev.morphia.annotations.Embedded;
                        import dev.morphia.annotations.Entity;
                        import dev.morphia.annotations.Id;

                        @Entity("people")
                        public class Person {
                            @Id
                            private String id;

                            @Embedded(useDiscriminator = false)
                            public static class Address {
                            }
                        }
                        """,
                //language=java
                """
                        import dev.morphia.annotations.Entity;
                        import dev.morphia.annotations.Id;

                        @Entity("people")
                        public class Person {
                            @Id
                            private String id;

                            @Entity(useDiscriminator = false)
                            public static class Address {
                            }
                        }
                        """));
    }

    @Test
    public void otherEntityImported() {
        rewriteRun(
                java(
                        //language=java
                        """
                                package other;

                                public @interface Entity {
                                }
                                """),
                java(
                        //language=java
                        """
                                import dev.morphia.annotations.Embedded;
                                import other.Entity;

                                @Embedded(discriminator = "addr")
                                public class Address {
                                }
                                """,
                        //language=java
                        """
                                import other.Entity;

                                @dev.morphia.annotations.Entity(discriminator = "addr")
                                public class Address {
                                }
                                """));
    }

    @Test
    public void embeddedAndEntityWithUnnamedValue() {
        rewriteRun(java(
                //language=java
                """
                        import dev.morphia.annotations.Embedded;
                        import dev.morphia.annotations.Entity;

                        @Entity("addresses")
                        @Embedded(useDiscriminator = false)
                        public class Address {
                        }
                        """,
                //language=java
                """
                        import dev.morphia.annotations.Entity;

                        @Entity(value = "addresses", useDiscriminator = false)
                        public class Address {
                        }
                        """));
    }

    @Test
    public void embeddedAfterModifiers() {
        rewriteRun(java(
                //language=java
                """
                        import dev.morphia.annotations.Embedded;

                        public @Embedded(useDiscriminator = false) class Address {
                        }
                        """,
                //language=java
                """
                        import dev.morphia.annotations.Entity;

                        public @Entity(useDiscriminator = false) class Address {
                        }
                        """));
    }

    @Test
    public void entityAfterModifiers() {
        rewriteRun(java(
                //language=java
                """
                        import dev.morphia.annotations.Embedded;
                        import dev.morphia.annotations.Entity;

                        @Embedded(discriminator = "addr")
                        public @Entity class Address {
                        }
                        """,
                //language=java
                """
                        import dev.morphia.annotations.Entity;

                        public @Entity(discriminator = "addr") class Address {
                        }
                        """));
    }

    @Test
    public void typeNamedEntity() {
        rewriteRun(java(
                //language=java
                """
                        import dev.morphia.annotations.Embedded;

                        @Embedded(useDiscriminator = false)
                        public class Entity {
                        }
                        """,
                //language=java
                """
                        @dev.morphia.annotations.Entity(useDiscriminator = false)
                        public class Entity {
                        }
                        """));
    }

    @Test
    public void nestedTypeNamedEntity() {
        rewriteRun(java(
                //language=java
                """
                        import dev.morphia.annotations.Embedded;

                        public class Outer {
                            @Embedded
                            public static class Address {
                            }

                            public static class Entity {
                            }
                        }
                        """,
                //language=java
                """
                        public class Outer {
                            @dev.morphia.annotations.Entity
                            public static class Address {
                            }

                            public static class Entity {
                            }
                        }
                        """));
    }
}
