package com.google.code.morphia.mapping.primitives;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Test;
import com.google.code.morphia.TestBase;
import com.google.code.morphia.annotations.Id;


public class ShortMappingTest extends TestBase {
  private static class Shorts {
    @Id
    ObjectId id;
    final List<Short[]> listWrapperArray = new ArrayList<Short[]>();
    final List<short[]> listPrimitiveArray = new ArrayList<short[]>();
    final List<Short> listWrapper = new ArrayList<Short>();
    short singlePrimitive;
    Short singleWrapper;
    short[] primitiveArray;
    Short[] wrapperArray;
    short[][] nestedPrimitiveArray;
    Short[][] nestedWrapperArray;
  }


  @Test
  public void testMapping() throws Exception {
    morphia.map(Shorts.class);
    final Shorts ent = new Shorts();
    ent.listWrapperArray.add(new Short[] {1, 2});
    ent.listPrimitiveArray.add(new short[] {2, 3, 12});
    ent.listWrapper.addAll(Arrays.asList((short)1, (short)2));
    ent.singlePrimitive = 100;
    ent.singleWrapper = 47;
    ent.primitiveArray = new short[] {5, 93};
    ent.wrapperArray = new Short[] { 55, 16, 99 };
    ent.nestedPrimitiveArray = new short[][] {{5, 93}, {88}};
    ent.nestedWrapperArray = new Short[][] {{ 55, 16, 99 }, {-47}};
    ds.save(ent);
    final Shorts loaded = ds.get(ent);

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
