package dev.morphia.test.mapping.codec.pojo;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.mapping.codec.pojo.EntityModel;
import dev.morphia.mapping.codec.pojo.EntityModelBuilder;
import dev.morphia.mapping.codec.pojo.PropertyModel;
import dev.morphia.test.TestBase;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.testng.annotations.Test;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import static java.util.stream.Collectors.joining;
import static org.testng.Assert.assertEquals;

public class EntityModelTest extends TestBase {
    @Test
    public void testFindParameterization() {
        EntityModel model = new EntityModelBuilder(getDs(), Child.class).build();
        assertEquals(model.getProperty("someField").getType(), LocalDate.class);
    }

    @Test
    public void testGenericFields() {
        EntityModel model = getDs().getMapper().map(Base.class).get(0);
        assertEquals(model.getProperties().size(), 3, model.getProperties().stream()
                                                           .map(PropertyModel::getName)
                                                           .collect(joining(", ")));

        model = getDs().getMapper().map(Parent.class).get(0);
        assertEquals(model.getProperties().size(), 4, model.getProperties().stream()
                                                           .map(PropertyModel::getName)
                                                           .collect(joining(", ")));

        model = getDs().getMapper().map(Child.class).get(0);
        assertEquals(model.getProperties().size(), 5, model.getProperties().stream()
                                                           .map(PropertyModel::getName)
                                                           .collect(joining(", ")));

        assertEquals(model.getProperty("t").getType(), String.class);
        assertEquals(model.getProperty("someField").getType(), LocalDate.class);
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

        assertEquals(model.getProperty("id").getType(), UUID.class);
        assertEquals(model.getProperty("test").getType(), String.class);
        assertEquals(model.getProperty("test2").getType(), UUID.class);

        getDs().getDatabase()
               .getCollection("specificEntity")
               .deleteMany(new Document());
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
