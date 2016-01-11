package org.mongodb.morphia.generics;

import com.mongodb.BasicDBObject;
import org.bson.types.ObjectId;
import org.junit.Test;
import org.mongodb.morphia.TestBase;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.mapping.cache.DefaultEntityCache;

import java.util.HashMap;
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
