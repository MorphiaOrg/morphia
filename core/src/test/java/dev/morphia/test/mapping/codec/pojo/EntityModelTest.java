package dev.morphia.test.mapping.codec.pojo;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.mapping.codec.pojo.EntityModel;
import dev.morphia.mapping.codec.pojo.PropertyModel;
import dev.morphia.test.TestBase;

import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static java.util.stream.Collectors.joining;

public class EntityModelTest extends TestBase {
    @Test
    public void testFindParameterization() {
        EntityModel model = getMapper().mapEntity(Child.class);
        Assertions.assertEquals(LocalDate.class, model.getProperty("someField").getType());
    }

    @Test
    public void testGenericFields() {
        EntityModel model = getDs().getMapper().map(Base.class).get(0);
        Assertions.assertEquals(3, model.getProperties().size(), model.getProperties().stream()
                .map(PropertyModel::getName)
                .collect(joining(", ")));

        model = getDs().getMapper().map(Parent.class).get(0);
        Assertions.assertEquals(4, model.getProperties().size(), model.getProperties().stream()
                .map(PropertyModel::getName)
                .collect(joining(", ")));

        model = getDs().getMapper().map(Child.class).get(0);
        Assertions.assertEquals(5, model.getProperties().size(), model.getProperties().stream()
                .map(PropertyModel::getName)
                .collect(joining(", ")));

        Assertions.assertEquals(String.class, model.getProperty("t").getType());
        Assertions.assertEquals(LocalDate.class, model.getProperty("someField").getType());
    }

    @Test
    public void testInheritedTypes() {
        EntityModel model = getDs().getMapper().map(MoreSpecificEntity.class).get(0);
        MoreSpecificEntity beforeDB = new MoreSpecificEntity();
        beforeDB.setId(UUID.randomUUID());
        beforeDB.setTest("a string");
        beforeDB.setTest2(UUID.randomUUID());
        beforeDB.setNumber(13);
        beforeDB.setNumber2(14);
        getDs().save(beforeDB);

        Assertions.assertEquals(UUID.class, model.getProperty("id").getType());
        Assertions.assertEquals(String.class, model.getProperty("test").getType());
        Assertions.assertEquals(UUID.class, model.getProperty("test2").getType());

        getDs().getDatabase()
                .getCollection("specificEntity")
                .deleteMany(new Document());
    }

    @Test
    public void testGenericRoundTrip() {
        EntityModel model = getDs().getMapper().map(GenericLeaf.class).get(0);
        Assertions.assertEquals(LocalDate.class, model.getProperty("baseField").getType());
        Assertions.assertEquals(String.class, model.getProperty("midField").getType());

        GenericLeaf leaf = new GenericLeaf();
        leaf.setBaseField(LocalDate.of(2024, 6, 1));
        leaf.setMidField("hello");
        leaf.setLeafValue(42);
        getDs().save(leaf);

        GenericLeaf found = getDs().find(GenericLeaf.class).first();
        Assertions.assertNotNull(found);
        Assertions.assertEquals(LocalDate.of(2024, 6, 1), found.getBaseField());
        Assertions.assertEquals("hello", found.getMidField());
        Assertions.assertEquals(42, found.getLeafValue());

        getDs().getDatabase().getCollection("genericLeaf").deleteMany(new Document());
    }

    @Entity
    private static class Base<T> {
        @Id
        private ObjectId id;
        private Map<String, Integer> map;
        private T someField;
    }

    private static class Child extends Parent<String> {
        private int age;
    }

    @Entity
    private static class GenericBase<T> {
        @Id
        private ObjectId id;
        private T baseField;

        public ObjectId getId() {
            return id;
        }

        public void setId(ObjectId id) {
            this.id = id;
        }

        public T getBaseField() {
            return baseField;
        }

        public void setBaseField(T baseField) {
            this.baseField = baseField;
        }
    }

    private static class GenericMid<T> extends GenericBase<LocalDate> {
        private T midField;

        public T getMidField() {
            return midField;
        }

        public void setMidField(T midField) {
            this.midField = midField;
        }
    }

    @Entity
    private static class GenericLeaf extends GenericMid<String> {
        private int leafValue;

        public int getLeafValue() {
            return leafValue;
        }

        public void setLeafValue(int leafValue) {
            this.leafValue = leafValue;
        }
    }

    @Entity
    private static class GenericEntity<T, U> {
        @Id
        protected T id;
        protected U test;
        protected UUID test2;

        public T getId() {
            return id;
        }

        public void setId(T id) {
            this.id = id;
        }

        public U getTest() {
            return test;
        }

        public void setTest(U test) {
            this.test = test;
        }

        public UUID getTest2() {
            return test2;
        }

        public void setTest2(UUID test2) {
            this.test2 = test2;
        }
    }

    @Entity
    private static class MoreSpecificEntity extends SpecificEntity<UUID, String> {
        private long number2;

        public long getNumber2() {
            return number2;
        }

        public void setNumber2(long number2) {
            this.number2 = number2;
        }
    }

    private static class Parent<T> extends Base<LocalDate> {
        private T t;
    }

    @Entity
    private static class SpecificEntity<ID, TEST> extends GenericEntity<ID, TEST> {
        private long number;

        public long getNumber() {
            return number;
        }

        public void setNumber(long number) {
            this.number = number;
        }
    }
}
