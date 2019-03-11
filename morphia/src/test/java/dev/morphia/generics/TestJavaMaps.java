package dev.morphia.generics;

import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Test;
import dev.morphia.Datastore;
import dev.morphia.TestBase;
import dev.morphia.annotations.Embedded;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.query.FindOptions;
import dev.morphia.testutil.TestEntity;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class TestJavaMaps extends TestBase {
    @Test
    public void mapperTest() {
        getMorphia().map(Employee.class);

        for (boolean nulls : new boolean[]{true, false}) {
            for (boolean empties : new boolean[]{true, false}) {
                getMorphia().getMapper().getOptions().setStoreNulls(nulls);
                getMorphia().getMapper().getOptions().setStoreEmpties(empties);
                empties();
            }
        }
    }

    private void empties() {
        Datastore ds = getDs();
        ds.delete(ds.find(Employee.class));
        Employee employee = new Employee();
        HashMap<String, Byte> byteMap = new HashMap<String, Byte>();
        byteMap.put("b", (byte) 1);
        employee.byteMap = byteMap;
        ds.save(employee);

        Employee loaded = ds.find(Employee.class)
                            .find(new FindOptions().limit(1))
                            .next();

        assertEquals(Byte.valueOf((byte) 1), loaded.byteMap.get("b"));
        assertNull(loaded.floatMap);
    }

    @Test
    public void emptyModel() {
        getMorphia().getMapper().getOptions().setStoreEmpties(true);
        getMorphia().getMapper().getOptions().setStoreNulls(false);

        TestEmptyModel model = new TestEmptyModel();
        model.text = "text";
        model.wrapped = new TestEmptyModel.Wrapped();
        model.wrapped.text = "textWrapper";
        getDs().save(model);
        TestEmptyModel model2 = getDs().find(TestEmptyModel.class).filter("id", model.id)
                                       .find(new FindOptions().limit(1))
                                       .next();
        Assert.assertNull(model.wrapped.others);
        Assert.assertNull(model2.wrapped.others);
    }

    @Test
    public void testKeyOrdering() {
        getMorphia().map(LinkedHashMapTestEntity.class);
        final LinkedHashMapTestEntity expectedEntity = new LinkedHashMapTestEntity();
        for (int i = 100; i >= 0; i--) {
            expectedEntity.getLinkedHashMap().put(i, "a" + i);
        }
        getDs().save(expectedEntity);
        LinkedHashMapTestEntity storedEntity = getDs().find(LinkedHashMapTestEntity.class)
                                                      .find(new FindOptions().limit(1))
                                                      .next();
        Assert.assertNotNull(storedEntity);
        Assert.assertEquals(expectedEntity.getLinkedHashMap(), storedEntity.getLinkedHashMap());
    }

    @Entity
    static class TestEmptyModel{
        @Id
        private ObjectId id;
        private String text;
        private Wrapped wrapped;

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

        @Embedded(concreteClass = java.util.LinkedHashMap.class)
        private final Map<Integer, String> linkedHashMap = new LinkedHashMap<Integer, String>();
        private Map<Integer, String> getLinkedHashMap() {
            return linkedHashMap;
        }

    }

}
