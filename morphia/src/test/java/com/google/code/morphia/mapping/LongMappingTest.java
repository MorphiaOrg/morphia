package com.google.code.morphia.mapping;


import java.util.ArrayList;
import java.util.List;

import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Test;
import com.google.code.morphia.TestBase;
import com.google.code.morphia.annotations.Id;


public class LongMappingTest extends TestBase {
  private static class Longs {
    @Id
    ObjectId id;
    final List<Long[]> longs = new ArrayList<Long[]>();
    final List<long[]> longPrimitives = new ArrayList<long[]>();
    long singlePrimitive;
    Long singleWrapper;
    long[] primitiveArray;
    Long[] wrapperArray;
  }


  @Test
  public void testMapping() throws Exception {
    morphia.map(Longs.class);
    final Longs ent = new Longs();
    ent.longs.add(new Long[] {1L, 2L});
    ent.longPrimitives.add(new long[] {2, 3, 12});
    ent.singlePrimitive = 100;
    ent.singleWrapper = 47L;
    ent.primitiveArray = new long[] {5, 93};
    ent.wrapperArray = new Long[] { 55L, 16L, 99L };
    ds.save(ent);
    final Longs loaded = ds.get(ent);

    Assert.assertNotNull(loaded.id);

    Assert.assertArrayEquals(ent.longs.get(0), loaded.longs.get(0));
    Assert.assertArrayEquals(ent.longPrimitives.get(0), loaded.longPrimitives.get(0));

    Assert.assertEquals(ent.singlePrimitive, loaded.singlePrimitive, 0);
    Assert.assertEquals(ent.singleWrapper, loaded.singleWrapper, 0);

    Assert.assertArrayEquals(ent.primitiveArray, loaded.primitiveArray);
    Assert.assertArrayEquals(ent.wrapperArray, loaded.wrapperArray);
  }
}
