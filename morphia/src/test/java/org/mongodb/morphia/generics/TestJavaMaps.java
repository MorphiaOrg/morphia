package org.mongodb.morphia.generics;

import com.mongodb.BasicDBObject;
import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Test;
import org.mongodb.morphia.TestBase;
import org.mongodb.morphia.annotations.Embedded;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.mapping.cache.DefaultEntityCache;
import org.mongodb.morphia.testutil.TestEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class TestJavaMaps extends TestBase {
    @Test
    public void mapperTest() {
        getMorphia().map(Employee.class);

        final BasicDBObject dbObject = new BasicDBObject("byteMap", new BasicDBObject("b", 1));
        Employee loaded = getMorphia().getMapper().fromDBObject(getDs(), Employee.class, dbObject, new DefaultEntityCache());

        assertEquals(Byte.class, (((Map) loaded.getByteMap()).get("b").getClass()));
    }

    @Test
    public void testKeyOrdering() {
        getMorphia().map(LinkedHashMapTestEntity.class);
        final LinkedHashMapTestEntity expectedEntity = new LinkedHashMapTestEntity();
        for (int i = 100; i >= 0; i--) {
            expectedEntity.getLinkedHashMap().put(i, "a" + i);
        }
        getDs().save(expectedEntity);
        LinkedHashMapTestEntity storedEntity = getDs().find(LinkedHashMapTestEntity.class).get();
        Assert.assertNotNull(storedEntity);
        Assert.assertEquals(
            new ArrayList<Integer>(expectedEntity.getLinkedHashMap().keySet()),
            new ArrayList<Integer>(storedEntity.getLinkedHashMap().keySet()));
    }

}

@Entity
class LinkedHashMapTestEntity extends TestEntity {
    @Embedded(concreteClass = java.util.LinkedHashMap.class)
    private final Map<Integer, String> linkedHashMap = new LinkedHashMap<Integer, String>();

    public Map<Integer, String> getLinkedHashMap() {
        return linkedHashMap;
    }
}

@Entity("employees")
class Employee {
    @Id
    private ObjectId id;

    private Map<String, Float> floatMap = new HashMap<String, Float>();
    private Map<String, Byte> byteMap = new HashMap<String, Byte>();

    public Map<String, Byte> getByteMap() {
        return byteMap;
    }

    public Map<String, Float> getFloatMap() {
        return floatMap;
    }

    public ObjectId getId() {
        return id;
    }
}
