package dev.morphia.test.mapping;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Map;

import dev.morphia.Datastore;
import dev.morphia.PrePersistListener;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.EntityListeners;
import dev.morphia.annotations.Field;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Index;
import dev.morphia.annotations.IndexOptions;
import dev.morphia.annotations.Indexes;
import dev.morphia.mapping.CritterMapper;
import dev.morphia.mapping.codec.pojo.ClassHierarchy;
import dev.morphia.mapping.codec.pojo.EntityModel;
import dev.morphia.mapping.codec.pojo.critter.CritterEntityModel;
import dev.morphia.test.TestBase;

import org.bson.types.ObjectId;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Both mappers collect type-level annotations from the same places, with the same precedence.
 */
public class TestTypeAnnotations extends TestBase {

    @Test
    public void interfaceAnnotations() {
        withTestConfig(List.of(AuditedEntity.class), () -> {
            EntityModel model = model(AuditedEntity.class);
            Assertions.assertEquals("tracked", indexName(model));
            Assertions.assertNotNull(model.getAnnotation(EntityListeners.class));

            AuditedEntity entity = new AuditedEntity();
            getDs().save(entity);
            Assertions.assertTrue(entity.audited, "the interface's @EntityListeners should run");
        });
    }

    @Test
    public void precedence() {
        withTestConfig(List.of(AnnotatedChild.class, AnnotatedSibling.class), () -> {
            // @Indexes is @Inherited, so the superclass's is the type's own and comes before the type's interface
            Assertions.assertEquals("base", indexName(model(AnnotatedChild.class)));
            // the interfaces of a superclass are searched too
            Assertions.assertEquals("base-marked", indexName(model(AnnotatedSibling.class)));
        });
    }

    @Test
    public void sharedInterfaceVisitedOnce() {
        Map<Class<? extends Annotation>, Annotation> annotations = ClassHierarchy.annotations(AnnotatedDiamond.class);
        Assertions.assertEquals(List.of(Entity.class, Indexes.class), List.copyOf(annotations.keySet()));
        Assertions.assertEquals("left", ((Indexes) annotations.get(Indexes.class)).value()[0].options().name());
    }

    private EntityModel model(Class<?> type) {
        EntityModel model = getMapper().getEntityModel(type);
        if (getMapper() instanceof CritterMapper) {
            // a fallback to reflection would test the reflective model twice
            Assertions.assertInstanceOf(CritterEntityModel.class, model, type.getName());
        }
        return model;
    }

    private static String indexName(EntityModel model) {
        return model.getAnnotation(Indexes.class).value()[0].options().name();
    }

    @EntityListeners(Auditor.class)
    @Indexes(@Index(fields = @Field("name"), options = @IndexOptions(name = "tracked")))
    public interface Tracked {
        void audit();
    }

    public static class Auditor implements PrePersistListener<Tracked> {
        @Override
        public void prePersist(Tracked entity, Datastore datastore) {
            entity.audit();
        }
    }

    @Entity
    public static class AuditedEntity implements Tracked {
        @Id
        ObjectId id;
        String name;
        transient boolean audited;

        @Override
        public void audit() {
            audited = true;
        }
    }

    @Indexes(@Index(fields = @Field("name"), options = @IndexOptions(name = "marked")))
    public interface Marked {
    }

    @Indexes(@Index(fields = @Field("name"), options = @IndexOptions(name = "base-marked")))
    public interface BaseMarked {
    }

    @Indexes(@Index(fields = @Field("name"), options = @IndexOptions(name = "base")))
    public static class Base {
        @Id
        ObjectId id;
        String name;
    }

    public static class Unindexed implements BaseMarked {
        @Id
        ObjectId id;
        String name;
    }

    @Entity
    public static class AnnotatedChild extends Base implements Marked {
    }

    @Entity
    public static class AnnotatedSibling extends Unindexed {
    }

    @Indexes(@Index(fields = @Field("name"), options = @IndexOptions(name = "left")))
    public interface Left extends Marked {
    }

    @Indexes(@Index(fields = @Field("name"), options = @IndexOptions(name = "right")))
    public interface Right extends Marked {
    }

    @Entity
    public static class AnnotatedDiamond implements Left, Right {
    }
}
