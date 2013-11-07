package org.mongodb.morphia.mapping;


import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Test;
import org.mongodb.morphia.TestBase;
import org.mongodb.morphia.annotations.Embedded;
import org.mongodb.morphia.annotations.Id;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * @author scotthernandez
 */
public class NestedMapOrCollTest extends TestBase {


    private static class HashMapOfMap {
        @Id
        private ObjectId id;

        @Embedded
        private final Map<String, Map<String, String>> mom = new HashMap<String, Map<String, String>>();
    }

    private static class HashMapOfList {
        @Id
        private ObjectId id;

        @Embedded
        private final Map<String, List<String>> mol = new HashMap<String, List<String>>();
    }

    private static class HashMapOfListOfMapMap {
        @Id
        private ObjectId id;

        @Embedded
        private final Map<String, List<HashMapOfMap>> mol = new HashMap<String, List<HashMapOfMap>>();
    }

    @Test
    public void testMapOfMap() throws Exception {
        HashMapOfMap mapOfMap = new HashMapOfMap();
        final Map<String, String> map = new HashMap<String, String>();
        mapOfMap.mom.put("root", map);
        map.put("deep", "values");
        map.put("peer", "lame");

        getDs().save(mapOfMap);
        mapOfMap = getDs().find(HashMapOfMap.class).get();
        Assert.assertNotNull(mapOfMap.mom);
        Assert.assertNotNull(mapOfMap.mom.get("root"));
        Assert.assertNotNull(mapOfMap.mom.get("root").get("deep"));
        Assert.assertEquals("values", mapOfMap.mom.get("root").get("deep"));
        Assert.assertNotNull("lame", mapOfMap.mom.get("root").get("peer"));
    }

    @Test
    public void testMapOfList() throws Exception {
        HashMapOfList map = new HashMapOfList();
        map.mol.put("entry1", Collections.singletonList("val1"));
        map.mol.put("entry2", Collections.singletonList("val2"));

        getDs().save(map);
        map = getDs().find(HashMapOfList.class).get();
        Assert.assertNotNull(map.mol);
        Assert.assertNotNull(map.mol.get("entry1"));
        Assert.assertNotNull(map.mol.get("entry1").get(0));
        Assert.assertEquals("val1", map.mol.get("entry1").get(0));
        Assert.assertNotNull("val2", map.mol.get("entry2").get(0));
    }

    @Test
    public void testMapOfListOfMapMap() throws Exception {
        final HashMapOfMap mapOfMap = new HashMapOfMap();
        final Map<String, String> map = new HashMap<String, String>();
        mapOfMap.mom.put("root", map);
        map.put("deep", "values");
        map.put("peer", "lame");


        HashMapOfListOfMapMap mapMap = new HashMapOfListOfMapMap();
        mapMap.mol.put("r1", Collections.singletonList(mapOfMap));
        mapMap.mol.put("r2", Collections.singletonList(mapOfMap));

        getDs().save(mapMap);
        mapMap = getDs().find(HashMapOfListOfMapMap.class).get();
        Assert.assertNotNull(mapMap.mol);
        Assert.assertNotNull(mapMap.mol.get("r1"));
        Assert.assertNotNull(mapMap.mol.get("r1").get(0));
        Assert.assertNotNull(mapMap.mol.get("r1").get(0).mom);
        Assert.assertEquals("values", mapMap.mol.get("r1").get(0).mom.get("root").get("deep"));
        Assert.assertEquals("lame", mapMap.mol.get("r1").get(0).mom.get("root").get("peer"));
        Assert.assertEquals("values", mapMap.mol.get("r2").get(0).mom.get("root").get("deep"));
        Assert.assertEquals("lame", mapMap.mol.get("r2").get(0).mom.get("root").get("peer"));
    }
}
