package com.google.code.morphia.mapping;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Test;
import com.google.code.morphia.TestBase;
import com.google.code.morphia.annotations.Id;


public class IntegerMappingTest extends TestBase {
  private static class Integers {
    @Id
    ObjectId id;
    final List<Integer[]> ints = new ArrayList<Integer[]>();
    final List<int[]> intPrimitives = new ArrayList<int[]>();
    final List<Integer> list = new ArrayList<Integer>();
    int singlePrimitive;
    Integer singleWrapper;
    int[] primitiveArray;
    Integer[] wrapperArray;
  }


  @Test
  public void testMapping() throws Exception {
    morphia.map(Integers.class);
    final Integers ent = new Integers();
    ent.ints.add(new Integer[] {1, 2});
    ent.list.addAll(Arrays.asList(1, 2));

    ent.intPrimitives.add(new int[] {2, 3, 12});
    ent.singlePrimitive = 100;
    ent.singleWrapper = 47;
    ent.primitiveArray = new int[] {5, 93};
    ent.wrapperArray = new Integer[] { 55, 16, 99 };
    ds.save(ent);
    final Integers loaded = ds.get(ent);

    Assert.assertNotNull(loaded.id);

    Assert.assertArrayEquals(ent.ints.get(0), loaded.ints.get(0));
    Assert.assertArrayEquals(ent.list.toArray(new Integer[0]), loaded.list.toArray(new Integer[0]));
    Assert.assertArrayEquals(ent.intPrimitives.get(0), loaded.intPrimitives.get(0));

    Assert.assertEquals(ent.singlePrimitive, loaded.singlePrimitive, 0);
    Assert.assertEquals(ent.singleWrapper, loaded.singleWrapper, 0);

    Assert.assertArrayEquals(ent.primitiveArray, loaded.primitiveArray);
    Assert.assertArrayEquals(ent.wrapperArray, loaded.wrapperArray);
  }
}
