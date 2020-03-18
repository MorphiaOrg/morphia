package dev.morphia.mapping;


import dev.morphia.Datastore;
import dev.morphia.TestBase;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static dev.morphia.query.experimental.filters.Filters.eq;


/**
 * @author scotthernandez
 */
public class ListOfStringArrayMappingTest extends TestBase {
    @Test
    public void testMapping() {
        getMapper().map(ContainsListStringArray.class);
        final ContainsListStringArray ent = new ContainsListStringArray();
        ent.listOfStrings.add(new String[]{"a", "b"});
        ent.arrayOfStrings = new String[]{"only", "the", "lonely"};
        ent.string = "raw string";

        getDs().save(ent);
        final Datastore datastore = getDs();
        final ContainsListStringArray loaded = datastore.find(ContainsListStringArray.class)
                                                        .filter(eq("_id", ent.id))
                                                        .first();
        Assert.assertNotNull(loaded.id);
        Assert.assertArrayEquals(ent.listOfStrings.get(0), loaded.listOfStrings.get(0));
        Assert.assertArrayEquals(ent.arrayOfStrings, loaded.arrayOfStrings);
        Assert.assertEquals(ent.string, loaded.string);
    }

    @Entity
    private static class ContainsListStringArray {
        private final List<String[]> listOfStrings = new ArrayList<>();
        @Id
        private ObjectId id;
        private String[] arrayOfStrings;
        private String string;
    }
}
