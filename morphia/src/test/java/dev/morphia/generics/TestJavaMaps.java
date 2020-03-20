package dev.morphia.generics;

import dev.morphia.Datastore;
import dev.morphia.DeleteOptions;
import dev.morphia.Morphia;
import dev.morphia.TestBase;
import dev.morphia.annotations.Embedded;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Property;
import dev.morphia.mapping.MapperOptions;
import dev.morphia.query.FindOptions;
import dev.morphia.testutil.TestEntity;
import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static dev.morphia.query.experimental.filters.Filters.eq;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class TestJavaMaps extends TestBase {
    @Test
    public void mapperTest() {
        getMapper().map(Employee.class);

        for (boolean nulls : new boolean[]{true, false}) {
            for (boolean empties : new boolean[]{true, false}) {
                MapperOptions options = MapperOptions.builder(getMapper().getOptions())
                                                     .storeNulls(nulls)
                                                     .storeEmpties(empties)
                                                     .build();
                empties(Morphia.createDatastore(getMongoClient(), getDatabase().getName(), options));
            }
        }
    }


    @Test
    public void emptyModel() {
        MapperOptions options = MapperOptions.builder(getMapper().getOptions())
                                             .storeEmpties(true)
                                             .storeNulls(false)
                                             .build();
        final Datastore datastore = Morphia.createDatastore(getMongoClient(), getDatabase().getName(), options);

        TestEmptyModel model = new TestEmptyModel();
        model.text = "text";
        model.wrapped = new TestEmptyModel.Wrapped();
        model.wrapped.text = "textWrapper";
        datastore.save(model);
        TestEmptyModel model2 = getDs().find(TestEmptyModel.class)
                                       .filter(eq("id", model.id)).iterator(new FindOptions().limit(1))
                                       .next();
        Assert.assertNull(model.wrapped.others);
        Assert.assertNull(model2.wrapped.others);
    }

    @Test
    public void testKeyOrdering() {
        getMapper().map(LinkedHashMapTestEntity.class);
        final LinkedHashMapTestEntity expectedEntity = new LinkedHashMapTestEntity();
        for (int i = 100; i >= 0; i--) {
            expectedEntity.getLinkedHashMap().put(i, "a" + i);
        }
        getDs().save(expectedEntity);
        LinkedHashMapTestEntity storedEntity = getDs().find(LinkedHashMapTestEntity.class).iterator(new FindOptions().limit(1))
                                                      .next();
        Assert.assertNotNull(storedEntity);
        Assert.assertEquals(expectedEntity.getLinkedHashMap(), storedEntity.getLinkedHashMap());
    }

    private void empties(final Datastore datastore) {
        datastore.find(Employee.class).remove(new DeleteOptions().multi(true));
        Employee employee = new Employee();
        HashMap<String, Byte> byteMap = new HashMap<String, Byte>();
        byteMap.put("b", (byte) 1);
        employee.byteMap = byteMap;
        datastore.save(employee);

        Employee loaded = datastore.find(Employee.class).iterator(new FindOptions().limit(1))
                                   .next();

        assertEquals(Byte.valueOf((byte) 1), loaded.byteMap.get("b"));
        assertNull(loaded.floatMap);
    }

    @Entity
    static class TestEmptyModel {
        @Id
        private ObjectId id;
        private String text;
        private Wrapped wrapped;

        @Embedded
        private static class Wrapped {
            private Map<String, Wrapped> others;
            private String text;
        }
    }

    @Entity("employees")
    static class Employee {
        @Id
        private ObjectId id;

        private Map<String, Float> floatMap;
        private Map<String, Byte> byteMap;
    }

    @Entity
    static class LinkedHashMapTestEntity extends TestEntity {

        @Property(concreteClass = java.util.LinkedHashMap.class)
        private final Map<Integer, String> linkedHashMap = new LinkedHashMap<Integer, String>();
        private Map<Integer, String> getLinkedHashMap() {
            return linkedHashMap;
        }

    }

}
