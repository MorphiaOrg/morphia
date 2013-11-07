package org.mongodb.morphia.mapping;


import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Test;
import org.mongodb.morphia.TestBase;
import org.mongodb.morphia.annotations.Id;

import java.util.ArrayList;
import java.util.List;


/**
 * @author scotthernandez
 */
public class ListOfStringArrayMappingTest extends TestBase {
    private static class ContainsListStringArray {
        @Id
        private ObjectId id;
        private final List<String[]> listOfStrings = new ArrayList<String[]>();
        private String[] arrayOfStrings;
        private String string;
    }

    @Test
    public void testMapping() throws Exception {
        getMorphia().map(ContainsListStringArray.class);
        final ContainsListStringArray ent = new ContainsListStringArray();
        ent.listOfStrings.add(new String[]{"a", "b"});
        ent.arrayOfStrings = new String[]{"only", "the", "lonely"};
        ent.string = "raw string";

        getDs().save(ent);
        final ContainsListStringArray loaded = getDs().get(ent);
        Assert.assertNotNull(loaded.id);
        Assert.assertArrayEquals(ent.listOfStrings.get(0), loaded.listOfStrings.get(0));
        Assert.assertArrayEquals(ent.arrayOfStrings, loaded.arrayOfStrings);
        Assert.assertEquals(ent.string, loaded.string);
    }
}