package dev.morphia.mapping;


import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Test;
import dev.morphia.TestBase;
import dev.morphia.annotations.Id;

import java.util.ArrayList;
import java.util.List;


/**
 * @author scotthernandez
 */
public class ListOfStringArrayMappingTest extends TestBase {
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

    private static class ContainsListStringArray {
        private final List<String[]> listOfStrings = new ArrayList<String[]>();
        @Id
        private ObjectId id;
        private String[] arrayOfStrings;
        private String string;
    }
}
