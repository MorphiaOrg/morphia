package dev.morphia.test.mapping.codec.pojo;

import com.mongodb.BasicDBObject;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.mapping.codec.pojo.EntityModel;
import dev.morphia.mapping.codec.pojo.EntityModelBuilder;
import dev.morphia.mapping.codec.pojo.FieldModelBuilder;
import dev.morphia.query.experimental.filters.Filters;
import dev.morphia.test.TestBase;
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
        EntityModel model = new EntityModelBuilder(getDatastore(), Child.class).build();
        assertEquals(model.getField("someField").getType(), LocalDate.class);
    }

    @Test
    public void testGenericFields() {
        EntityModelBuilder builder = new EntityModelBuilder(getDatastore(), Base.class);
        assertEquals(builder.fieldModels().size(), 3, builder.fieldModels().stream()
                                                             .map(FieldModelBuilder::name)
                                                             .collect(joining(", ")));

        builder = new EntityModelBuilder(getDatastore(), Parent.class);
        assertEquals(builder.fieldModels().size(), 4, builder.fieldModels().stream()
                                                             .map(FieldModelBuilder::name)
                                                             .collect(joining(", ")));

        builder = new EntityModelBuilder(getDatastore(), Child.class);
        assertEquals(builder.fieldModels().size(), 5, builder.fieldModels().stream()
                                                             .map(FieldModelBuilder::name)
                                                             .collect(joining(", ")));

        EntityModel model = builder.build();
        assertEquals(model.getField("t").getType(), String.class);
        assertEquals(model.getField("someField").getType(), LocalDate.class);
    }

    @Test
    public void testInheritedTypes() {
        EntityModel model = getDatastore().getMapper().map(MoreSpecificEntity.class).get(0);
        MoreSpecificEntity beforeDB = new MoreSpecificEntity();
        beforeDB.setId(UUID.randomUUID());
        beforeDB.setTest(UUID.randomUUID());
        beforeDB.setTest2(UUID.randomUUID());
        beforeDB.setNumber(13);
        beforeDB.setNumber2(14);
        getDatastore().save(beforeDB);

        MoreSpecificEntity fromDB = getDatastore().find(MoreSpecificEntity.class)
                                                  .filter(Filters.eq("_id", beforeDB.getId()))
                                                  .first();

        assertEquals(model.getField("id").getType(), UUID.class);
        assertEquals(model.getField("test").getType(), UUID.class);
        assertEquals(model.getField("test2").getType(), UUID.class);


        getDatastore().getDatabase()
                      .getCollection("specificEntity")
                      .deleteMany(new BasicDBObject());
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
    private static class GenericEntity<T> {
        @Id
        protected T id;
        protected T test;
        protected UUID test2;

        public T getId() {
            return id;
        }

        public void setId(T id) {
            this.id = id;
        }

        public T getTest() {
            return test;
        }

        public void setTest(T test) {
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
    private static class MoreSpecificEntity extends SpecificEntity<UUID> {
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
    private static class SpecificEntity<ID> extends GenericEntity<ID> {
        private long number;

        public long getNumber() {
            return number;
        }

        public void setNumber(long number) {
            this.number = number;
        }
    }
}