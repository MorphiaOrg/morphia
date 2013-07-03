package com.google.code.morphia.mapping.primitives;


import java.util.ArrayList;
import java.util.Arrays;
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
    final List<Long[]> listWrapperArray = new ArrayList<Long[]>();
    final List<long[]> listPrimitiveArray = new ArrayList<long[]>();
    final List<Long> listWrapper = new ArrayList<Long>();
    long singlePrimitive;
    Long singleWrapper;
    long[] primitiveArray;
    Long[] wrapperArray;
    long[][] nestedPrimitiveArray;
    Long[][] nestedWrapperArray;
  }


  @Test
  public void testMapping() throws Exception {
    morphia.map(Longs.class);
    final Longs ent = new Longs();
    ent.listWrapperArray.add(new Long[] {1L, 2L});
    ent.listPrimitiveArray.add(new long[] {2, 3, 12});
    ent.listWrapper.addAll(Arrays.asList(1L, 2L));
    ent.singlePrimitive = 100;
    ent.singleWrapper = 47L;
    ent.primitiveArray = new long[] {5, 93};
    ent.wrapperArray = new Long[] {55L, 16L, 99L};
    ent.nestedPrimitiveArray = new long[][] {{0}, {5, 93}};
    ent.nestedWrapperArray = new Long[][] {{99L, 1L}, {55L, 16L, 99L}};
    ds.save(ent);
    final Longs loaded = ds.get(ent);

    Assert.assertNotNull(loaded.id);

    Assert.assertArrayEquals(ent.listWrapperArray.get(0), loaded.listWrapperArray.get(0));
    Assert.assertArrayEquals(ent.listPrimitiveArray.get(0), loaded.listPrimitiveArray.get(0));

    Assert.assertEquals(ent.singlePrimitive, loaded.singlePrimitive, 0);
    Assert.assertEquals(ent.singleWrapper, loaded.singleWrapper, 0);

    Assert.assertArrayEquals(ent.primitiveArray, loaded.primitiveArray);
    Assert.assertArrayEquals(ent.wrapperArray, loaded.wrapperArray);
    Assert.assertArrayEquals(ent.nestedPrimitiveArray, loaded.nestedPrimitiveArray);
    Assert.assertArrayEquals(ent.nestedWrapperArray, loaded.nestedWrapperArray);
  }
}
