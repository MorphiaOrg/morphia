package dev.morphia.indexes;

import dev.morphia.TestBase;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Field;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Index;
import dev.morphia.annotations.Indexed;
import dev.morphia.annotations.Indexes;
import dev.morphia.mapping.codec.pojo.EntityModel;
import org.bson.types.ObjectId;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class TestIndexInheritance extends TestBase {

    @Test
    public void testClassIndexInherit() {
        getMapper().map(Shape.class);
        final EntityModel entityModel = getMapper().getEntityModel(Circle.class);
        assertNotNull(entityModel);

        assertNotNull(entityModel.getAnnotation(Indexes.class));

        getDs().ensureIndexes();

        assertEquals(4, getIndexInfo(Circle.class).size());
    }

    @Test
    public void testInheritedFieldIndex() {
        getMapper().map(Shape.class);
        getMapper().getEntityModel(Circle.class);

        getDs().ensureIndexes();

        assertEquals(4, getIndexInfo(Circle.class).size());
    }

    @Entity
    @Indexes(@Index(fields = @Field("description")))
    public abstract static class Shape {
        @Id
        private ObjectId id;
        private String description;
        @Indexed
        private String foo;

        public String getDescription() {
            return description;
        }

        void setDescription(String description) {
            this.description = description;
        }

        public String getFoo() {
            return foo;
        }

        public void setFoo(String foo) {
            this.foo = foo;
        }

        public ObjectId getId() {
            return id;
        }

        public void setId(ObjectId id) {
            this.id = id;
        }
    }

    @Indexes(@Index(fields = @Field("radius")))
    private static class Circle extends Shape {
        private final double radius = 1;

        Circle() {
            setDescription("Circles are round and can be rolled along the ground.");
        }
    }

}
