package com.google.code.morphia.mapping;


import java.util.ArrayList;

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
    final ArrayList<String[]> listOfStrings = new ArrayList<String[]>();
  }

  @Test
  public void testMapping() throws Exception {
    morphia.map(ContainsListStringArray.class);
    final ContainsListStringArray ent = new ContainsListStringArray();
    ent.listOfStrings.add(new String[] { "a", "b" });
    ds.save(ent);
    final ContainsListStringArray loaded = ds.get(ent);
    Assert.assertNotNull(loaded.id);
    Assert.assertArrayEquals(ent.listOfStrings.get(0), loaded.listOfStrings.get(0));
  }
}