package dev.morphia.rewrite.recipes.test.lifecycle;

import dev.morphia.rewrite.recipes.lifecycle.AnnotatedListenerMethodMigration;
import dev.morphia.rewrite.recipes.lifecycle.EntityListenerMigration;
import dev.morphia.rewrite.recipes.test.MorphiaRewriteTest;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.openrewrite.Recipe;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.TypeValidation;

import static org.openrewrite.java.Assertions.java;

class AnnotatedListenerMethodMigrationTest extends MorphiaRewriteTest {
    //language=java
    private static final String ORDER = """
            package app;

            import dev.morphia.annotations.Entity;
            import dev.morphia.annotations.EntityListeners;
            import dev.morphia.annotations.Id;

            @Entity
            @EntityListeners(Stamping.class)
            public class Order {
                @Id
                String id;
            }
            """;

    @NotNull
    @Override
    protected Recipe getRecipe() {
        return new AnnotatedListenerMethodMigration();
    }

    @Override
    public void defaults(RecipeSpec spec) {
        super.defaults(spec);
        // the new interfaces aren't on the pre-3.0 classpath the sources are parsed against
        spec.afterTypeValidationOptions(TypeValidation.none());
    }

    @Test
    void movesTheMethodsOfAListedClassToTheInterfaces() {
        rewriteRun(java(ORDER),
                java(
                        //language=java
                        """
                                package app;

                                import dev.morphia.Datastore;
                                import dev.morphia.annotations.PostLoad;
                                import dev.morphia.annotations.PrePersist;
                                import org.bson.Document;

                                public class Stamping {
                                    @PrePersist
                                    void stamp(Order order) {
                                        System.out.println(order);
                                    }

                                    @PostLoad
                                    void check(Document document, Order order, Datastore datastore) {
                                        System.out.println(document);
                                    }
                                }
                                """,
                        //language=java
                        """
                                package app;

                                import dev.morphia.Datastore;
                                import dev.morphia.PostLoadDocumentListener;
                                import dev.morphia.PrePersistListener;
                                import org.bson.Document;

                                public class Stamping implements PrePersistListener<Order>, PostLoadDocumentListener<Order> {
                                    void stamp(Order order) {
                                        System.out.println(order);
                                    }

                                    void check(Document document, Order order, Datastore datastore) {
                                        System.out.println(document);
                                    }

                                    @Override
                                    public void prePersist(Order entity, Datastore datastore) {
                                        stamp(entity);
                                    }

                                    @Override
                                    public void postLoad(Order entity, Document document, Datastore datastore) {
                                        check(document, entity, datastore);
                                    }
                                }
                                """));
    }

    @Test
    void callsEachMethodForAnEventInOrder() {
        rewriteRun(java(ORDER),
                java(
                        //language=java
                        """
                                package app;

                                import dev.morphia.annotations.PostLoad;

                                public class Stamping {
                                    @PostLoad
                                    void first() {
                                    }

                                    @PostLoad
                                    void postLoad(Order order) {
                                    }
                                }
                                """,
                        //language=java
                        """
                                package app;

                                import dev.morphia.Datastore;
                                import dev.morphia.PostLoadListener;

                                public class Stamping implements PostLoadListener<Order> {
                                    void first() {
                                    }

                                    void postLoad(Order order) {
                                    }

                                    @Override
                                    public void postLoad(Order entity, Datastore datastore) {
                                        first();
                                        postLoad(entity);
                                    }
                                }
                                """));
    }

    @Test
    void replacesTheEntityListenerInterfaceAndUsesItsTypeArgument() {
        rewriteRun(java(
                //language=java
                """
                        package app;

                        import dev.morphia.EntityListener;
                        import dev.morphia.annotations.PreLoad;
                        import org.bson.Document;

                        public class Cleaning implements EntityListener<Object> {
                            @PreLoad
                            void clean(Document document) {
                                document.remove("legacy");
                            }

                            @Override
                            public boolean hasAnnotation(Class<? extends java.lang.annotation.Annotation> type) {
                                return true;
                            }
                        }
                        """,
                //language=java
                """
                        package app;

                        import dev.morphia.Datastore;
                        import dev.morphia.PreLoadListener;
                        import org.bson.Document;

                        public class Cleaning implements PreLoadListener<Object> {
                            void clean(Document document) {
                                document.remove("legacy");
                            }

                            @Override
                            public boolean hasAnnotation(Class<? extends java.lang.annotation.Annotation> type) {
                                return true;
                            }

                            @Override
                            public void preLoad(Object entity, Document document, Datastore datastore) {
                                clean(document);
                            }
                        }
                        """));
    }

    @Test
    void reusesAMethodThatIsAlreadyTheInterfacesMethod() {
        rewriteRun(java(ORDER),
                java(
                        //language=java
                        """
                                package app;

                                import dev.morphia.Datastore;
                                import dev.morphia.annotations.PostPersist;

                                public class Stamping {
                                    @PostPersist
                                    public void postPersist(Order order, Datastore datastore) {
                                    }
                                }
                                """,
                        //language=java
                        """
                                package app;

                                import dev.morphia.Datastore;
                                import dev.morphia.PostPersistListener;

                                public class Stamping implements PostPersistListener<Order> {
                                    public void postPersist(Order order, Datastore datastore) {
                                    }
                                }
                                """));
    }

    @Test
    void passesAnEmptyDocumentToAPrePersistMethodThatIgnoresIt() {
        rewriteRun(java(ORDER),
                java(
                        //language=java
                        """
                                package app;

                                import dev.morphia.annotations.PrePersist;
                                import org.bson.Document;

                                public class Stamping {
                                    @PrePersist
                                    void stamp(Order order, Document document) {
                                    }
                                }
                                """,
                        //language=java
                        """
                                package app;

                                import dev.morphia.Datastore;
                                import dev.morphia.PrePersistListener;
                                import org.bson.Document;

                                public class Stamping implements PrePersistListener<Order> {
                                    void stamp(Order order, Document document) {
                                    }

                                    @Override
                                    public void prePersist(Order entity, Datastore datastore) {
                                        stamp(entity, new Document());
                                    }
                                }
                                """));
    }

    @Test
    void flagsAPrePersistMethodThatUsesTheDocument() {
        rewriteRun(java(ORDER),
                java(
                        //language=java
                        """
                                package app;

                                import dev.morphia.annotations.PrePersist;
                                import org.bson.Document;

                                public class Stamping {
                                    @PrePersist
                                    void stamp(Order order, Document document) {
                                        document.put("stamp", 1);
                                    }
                                }
                                """,
                        //language=java
                        """
                                package app;

                                import dev.morphia.annotations.PrePersist;
                                import org.bson.Document;

                                public class Stamping {
                                    /*~~(PrePersistListener takes no Document: the one this method was given was always empty, as the entity isn't encoded yet. To change what's saved, implement PostPersistDocumentListener)~~>*/@PrePersist
                                    void stamp(Order order, Document document) {
                                        document.put("stamp", 1);
                                    }
                                }
                                """));
    }

    @Test
    void leavesAnEntitysOwnMethods() {
        rewriteRun(java(
                //language=java
                """
                        package app;

                        import dev.morphia.annotations.Entity;
                        import dev.morphia.annotations.EntityListeners;
                        import dev.morphia.annotations.Id;
                        import dev.morphia.annotations.PrePersist;

                        @Entity
                        @EntityListeners(Order.class)
                        public class Order {
                            @Id
                            String id;

                            @PrePersist
                            void stamp() {
                            }
                        }
                        """));
    }

    @Test
    void leavesAClassThatIsntAListener() {
        rewriteRun(java(
                //language=java
                """
                        package app;

                        import dev.morphia.annotations.PrePersist;

                        public class Base {
                            @PrePersist
                            void stamp() {
                            }
                        }
                        """));
    }

    @Test
    void flagsAListenerThatInheritsAnnotatedMethods() {
        rewriteRun(java(ORDER),
                java(
                        //language=java
                        """
                                package app;

                                import dev.morphia.annotations.PrePersist;

                                public class Base {
                                    @PrePersist
                                    void stamp() {
                                    }
                                }
                                """),
                java(
                        //language=java
                        """
                                package app;

                                import dev.morphia.annotations.PostLoad;

                                public class Stamping extends Base {
                                    @PostLoad
                                    void check() {
                                    }
                                }
                                """,
                        //language=java
                        """
                                package app;

                                import dev.morphia.annotations.PostLoad;

                                /*~~(extends app.Base, which has lifecycle methods of its own, and only an entity's own annotated methods are called now: implement the per-event listener interfaces by hand, calling the superclass's)~~>*/public class Stamping extends Base {
                                    @PostLoad
                                    void check() {
                                    }
                                }
                                """));
    }

    @Test
    void flagsMethodsThatTakeUnrelatedEntityTypes() {
        rewriteRun(java(ORDER),
                java(
                        //language=java
                        """
                                package app;

                                import dev.morphia.annotations.PostLoad;
                                import dev.morphia.annotations.PrePersist;

                                public class Stamping {
                                    @PrePersist
                                    void stamp(Order order) {
                                    }

                                    @PostLoad
                                    void check(String other) {
                                    }
                                }
                                """,
                        //language=java
                        """
                                package app;

                                import dev.morphia.annotations.PostLoad;
                                import dev.morphia.annotations.PrePersist;

                                /*~~(its lifecycle methods take unrelated entity types, and only an entity's own annotated methods are called now: implement the per-event listener interfaces by hand)~~>*/public class Stamping {
                                    @PrePersist
                                    void stamp(Order order) {
                                    }

                                    @PostLoad
                                    void check(String other) {
                                    }
                                }
                                """));
    }

    @Test
    void flagsAMethodThatThrowsACheckedException() {
        rewriteRun(java(ORDER),
                java(
                        //language=java
                        """
                                package app;

                                import dev.morphia.annotations.PostLoad;

                                public class Stamping {
                                    @PostLoad
                                    void check(Order order) throws java.io.IOException {
                                    }
                                }
                                """,
                        //language=java
                        """
                                package app;

                                import dev.morphia.annotations.PostLoad;

                                public class Stamping {
                                    /*~~(it throws a checked exception, which PostLoadListener's method can't: call it from there by hand)~~>*/@PostLoad
                                    void check(Order order) throws java.io.IOException {
                                    }
                                }
                                """));
    }

    @Test
    void runsAfterTheEntityListenerMigration() {
        rewriteRun(spec -> spec.recipes(new EntityListenerMigration(), new AnnotatedListenerMethodMigration()),
                java(ORDER),
                java(
                        //language=java
                        """
                                package app;

                                import dev.morphia.Datastore;
                                import dev.morphia.EntityListener;
                                import dev.morphia.annotations.PrePersist;
                                import org.bson.Document;

                                import java.lang.annotation.Annotation;

                                public class Stamping implements EntityListener<Order> {
                                    @Override
                                    public boolean hasAnnotation(Class<? extends Annotation> type) {
                                        return true;
                                    }

                                    @Override
                                    public void postLoad(Order entity, Document document, Datastore datastore) {
                                        System.out.println(entity);
                                    }

                                    @PrePersist
                                    void stamp(Order order) {
                                    }
                                }
                                """,
                        //language=java
                        """
                                package app;

                                import dev.morphia.Datastore;
                                import dev.morphia.PostLoadListener;
                                import dev.morphia.PrePersistListener;

                                public class Stamping implements PostLoadListener<Order>, PrePersistListener<Order> {
                                    @Override
                                    public void postLoad(Order entity, Datastore datastore) {
                                        System.out.println(entity);
                                    }

                                    void stamp(Order order) {
                                    }

                                    @Override
                                    public void prePersist(Order entity, Datastore datastore) {
                                        stamp(entity);
                                    }
                                }
                                """));
    }

    @Test
    void flagsASubclassOfAListener() {
        rewriteRun(java(ORDER),
                java(
                        //language=java
                        """
                                package app;

                                import dev.morphia.EntityListener;

                                public class Base implements EntityListener<Order> {
                                    @Override
                                    public boolean hasAnnotation(Class<? extends java.lang.annotation.Annotation> type) {
                                        return false;
                                    }
                                }
                                """),
                java(
                        //language=java
                        """
                                package app;

                                import dev.morphia.annotations.PrePersist;

                                public class Stamping extends Base {
                                    @PrePersist
                                    void stamp(Order order) {
                                    }
                                }
                                """,
                        //language=java
                        """
                                package app;

                                import dev.morphia.annotations.PrePersist;

                                /*~~(extends app.Base, which has lifecycle methods of its own, and only an entity's own annotated methods are called now: implement the per-event listener interfaces by hand, calling the superclass's)~~>*/public class Stamping extends Base {
                                    @PrePersist
                                    void stamp(Order order) {
                                    }
                                }
                                """));
    }

    @Test
    void flagsAMethodWhoseEventTheClassAlreadyImplements() {
        rewriteRun(spec -> spec.recipes(new EntityListenerMigration(), new AnnotatedListenerMethodMigration()),
                java(ORDER),
                java(
                        //language=java
                        """
                                package app;

                                import dev.morphia.Datastore;
                                import dev.morphia.EntityListener;
                                import dev.morphia.annotations.PostLoad;
                                import org.bson.Document;

                                public class Stamping implements EntityListener<Order> {
                                    @Override
                                    public boolean hasAnnotation(Class<? extends java.lang.annotation.Annotation> type) {
                                        return true;
                                    }

                                    @Override
                                    public void postLoad(Order entity, Document document, Datastore datastore) {
                                        System.out.println(document);
                                    }

                                    @PostLoad
                                    void check(Order order) {
                                    }
                                }
                                """,
                        //language=java
                        """
                                package app;

                                import dev.morphia.Datastore;
                                import dev.morphia.PostLoadDocumentListener;
                                import dev.morphia.annotations.PostLoad;
                                import org.bson.Document;

                                public class Stamping implements PostLoadDocumentListener<Order> {
                                    @Override
                                    public void postLoad(Order entity, Document document, Datastore datastore) {
                                        System.out.println(document);
                                    }

                                    /*~~(the class already implements postLoad() for this event, so this method can't be called from PostLoadListener's: call it from there by hand)~~>*/@PostLoad
                                    void check(Order order) {
                                    }
                                }
                                """));
    }

    @Test
    void callsAMethodFromEachEventItsAnnotatedFor() {
        rewriteRun(java(ORDER),
                java(
                        //language=java
                        """
                                package app;

                                import dev.morphia.annotations.PostLoad;
                                import dev.morphia.annotations.PrePersist;

                                public class Stamping {
                                    @PrePersist
                                    @PostLoad
                                    void touch(Order order) {
                                    }
                                }
                                """,
                        //language=java
                        """
                                package app;

                                import dev.morphia.Datastore;
                                import dev.morphia.PostLoadListener;
                                import dev.morphia.PrePersistListener;

                                public class Stamping implements PrePersistListener<Order>, PostLoadListener<Order> {
                                    void touch(Order order) {
                                    }

                                    @Override
                                    public void prePersist(Order entity, Datastore datastore) {
                                        touch(entity);
                                    }

                                    @Override
                                    public void postLoad(Order entity, Datastore datastore) {
                                        touch(entity);
                                    }
                                }
                                """));
    }

    @Test
    void doesNotReuseAMethodThatReturnsAValue() {
        rewriteRun(java(ORDER),
                java(
                        //language=java
                        """
                                package app;

                                import dev.morphia.Datastore;
                                import dev.morphia.annotations.PostLoad;

                                public class Stamping {
                                    @PostLoad
                                    public int postLoad(Order order, Datastore datastore) {
                                        return 1;
                                    }
                                }
                                """,
                        //language=java
                        """
                                package app;

                                import dev.morphia.Datastore;
                                import dev.morphia.annotations.PostLoad;

                                public class Stamping {
                                    /*~~(the class already declares postLoad() with 2 parameters, so this method can't be called from PostLoadListener's: call it from there by hand)~~>*/@PostLoad
                                    public int postLoad(Order order, Datastore datastore) {
                                        return 1;
                                    }
                                }
                                """));
    }

    @Test
    void flagsAListenerThatOnlyInheritsAnnotatedMethods() {
        rewriteRun(java(ORDER),
                java(
                        //language=java
                        """
                                package app;

                                import dev.morphia.annotations.PrePersist;

                                public class Base {
                                    @PrePersist
                                    void stamp() {
                                    }
                                }
                                """),
                java(
                        //language=java
                        """
                                package app;

                                public class Stamping extends Base {
                                }
                                """,
                        //language=java
                        """
                                package app;

                                /*~~(inherits annotated lifecycle methods from app.Base, which isn't a listener, and only an entity's own annotated methods are called now: implement the per-event listener interfaces by hand)~~>*/public class Stamping extends Base {
                                }
                                """));
    }

    @Test
    void doesNotReuseAMethodWhenTheClassImplementsTheOtherInterface() {
        rewriteRun(spec -> spec.recipes(new EntityListenerMigration(), new AnnotatedListenerMethodMigration()),
                java(ORDER),
                java(
                        //language=java
                        """
                                package app;

                                import dev.morphia.Datastore;
                                import dev.morphia.EntityListener;
                                import dev.morphia.annotations.PostLoad;
                                import org.bson.Document;

                                public class Stamping implements EntityListener<Order> {
                                    @Override
                                    public boolean hasAnnotation(Class<? extends java.lang.annotation.Annotation> type) {
                                        return true;
                                    }

                                    @Override
                                    public void postLoad(Order entity, Document document, Datastore datastore) {
                                        System.out.println(document);
                                    }

                                    @PostLoad
                                    public void postLoad(Order order, Datastore datastore) {
                                    }
                                }
                                """,
                        //language=java
                        """
                                package app;

                                import dev.morphia.Datastore;
                                import dev.morphia.PostLoadDocumentListener;
                                import dev.morphia.annotations.PostLoad;
                                import org.bson.Document;

                                public class Stamping implements PostLoadDocumentListener<Order> {
                                    @Override
                                    public void postLoad(Order entity, Document document, Datastore datastore) {
                                        System.out.println(document);
                                    }

                                    /*~~(the class already implements postLoad() for this event, so this method can't be called from PostLoadListener's: call it from there by hand)~~>*/@PostLoad
                                    public void postLoad(Order order, Datastore datastore) {
                                    }
                                }
                                """));
    }

    @Test
    void castsTheEntityToKeepTheAnnotatedOverload() {
        rewriteRun(java(ORDER),
                java(
                        //language=java
                        """
                                package app;

                                import dev.morphia.EntityListener;
                                import dev.morphia.annotations.PostLoad;

                                public class Stamping implements EntityListener<Order> {
                                    @PostLoad
                                    void touch(Object entity) {
                                    }

                                    void touch(Order order) {
                                    }

                                    @Override
                                    public boolean hasAnnotation(Class<? extends java.lang.annotation.Annotation> type) {
                                        return false;
                                    }
                                }
                                """,
                        //language=java
                        """
                                package app;

                                import dev.morphia.Datastore;
                                import dev.morphia.PostLoadListener;

                                public class Stamping implements PostLoadListener<Order> {
                                    void touch(Object entity) {
                                    }

                                    void touch(Order order) {
                                    }

                                    @Override
                                    public boolean hasAnnotation(Class<? extends java.lang.annotation.Annotation> type) {
                                        return false;
                                    }

                                    @Override
                                    public void postLoad(Order entity, Datastore datastore) {
                                        touch((Object) entity);
                                    }
                                }
                                """));
    }
}
