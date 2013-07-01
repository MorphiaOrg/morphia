package com.google.code.morphia.mapping;


import java.util.ArrayList;
import java.util.List;

import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Test;
import com.google.code.morphia.TestBase;
import com.google.code.morphia.annotations.Id;


/**
 * @author scotthernandez
 */
public class ListOfStringArrayMappingTest extends TestBase {
  private static class ContainsListStringArray {
    @Id ObjectId id;
    final List<String[]> listOfStrings = new ArrayList<String[]>();
    String[] arrayOfStrings;
    String string;
  }

  @Test
  public void testMapping() throws Exception {
    morphia.map(ContainsListStringArray.class);
    final ContainsListStringArray ent = new ContainsListStringArray();
    ent.listOfStrings.add(new String[] { "a", "b" });
    ent.arrayOfStrings =  new String[] { "only", "the", "lonely" };
    ent.string = "raw string";

    ds.save(ent);
    final ContainsListStringArray loaded = ds.get(ent);
    Assert.assertNotNull(loaded.id);
    Assert.assertArrayEquals(ent.listOfStrings.get(0), loaded.listOfStrings.get(0));
    Assert.assertArrayEquals(ent.arrayOfStrings, loaded.arrayOfStrings);
    Assert.assertEquals(ent.string, loaded.string);
  }
}