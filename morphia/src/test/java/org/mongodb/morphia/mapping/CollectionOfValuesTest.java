package org.mongodb.morphia.mapping;


import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.mongodb.morphia.Key;
import org.mongodb.morphia.TestBase;
import org.mongodb.morphia.annotations.Embedded;
import org.mongodb.morphia.annotations.Id;

import java.util.ArrayList;
import java.util.List;


/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 */
public class CollectionOfValuesTest extends TestBase {

    public static class ContainsListOfList {
        @Id
        private ObjectId id;
        @Embedded
        private List<List<String>> listOfList;
    }

    public static class ContainsTwoDimensionalArray {
        @Id
        private String id;
        @Embedded
        private byte[][] twoDimArray;
    }

    @Test
    @Ignore("Not yet implemented")
    public void testTwoDimensionalArrayMapping() throws Exception {
        getMorphia().map(ContainsTwoDimensionalArray.class);
        final ContainsTwoDimensionalArray entity = new ContainsTwoDimensionalArray();
        final byte[][] test2DimBa = new byte[][]{"Joseph".getBytes(), "uwe".getBytes()};
        entity.twoDimArray = test2DimBa;
        final Key<ContainsTwoDimensionalArray> savedKey = getDs().save(entity);
        final ContainsTwoDimensionalArray loaded = getDs().get(ContainsTwoDimensionalArray.class, savedKey.getId());
        Assert.assertNotNull(loaded.twoDimArray);
        Assert.assertEquals(test2DimBa, loaded.twoDimArray);
        Assert.assertNotNull(loaded.id);
    }

    @Test
    public void testListOfListMapping() throws Exception {
        getMorphia().map(ContainsListOfList.class);
        getDs().delete(getDs().find(ContainsListOfList.class));
        final ContainsListOfList entity = new ContainsListOfList();
        final List<List<String>> testList = new ArrayList<List<String>>();
        final List<String> element1 = new ArrayList<String>();
        element1.add("element1");
        testList.add(element1);

        final List<String> element2 = new ArrayList<String>();
        element2.add("element2");
        testList.add(element2);

        entity.listOfList = testList;
        getDs().save(entity);
        final ContainsListOfList loaded = getDs().get(entity);

        Assert.assertNotNull(loaded.listOfList);

        Assert.assertEquals(testList, loaded.listOfList);
        final List<String> loadedElement1 = loaded.listOfList.get(0);
        Assert.assertEquals(element1, loadedElement1);
        Assert.assertEquals(element1.get(0), loadedElement1.get(0));
        Assert.assertNotNull(loaded.id);
    }

}
