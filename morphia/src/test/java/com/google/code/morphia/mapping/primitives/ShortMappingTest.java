package com.google.code.morphia.mapping.primitives;


import java.util.ArrayList;
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
    final List<Short[]> shorts = new ArrayList<Short[]>();
    final List<short[]> shortPrimitives = new ArrayList<short[]>();
    short singlePrimitive;
    Short singleWrapper;
    short[] primitiveArray;
    Short[] wrapperArray;
  }


  @Test
  public void testMapping() throws Exception {
    morphia.map(Shorts.class);
    final Shorts ent = new Shorts();
    ent.shorts.add(new Short[] {1, 2});
    ent.shortPrimitives.add(new short[] {2, 3, 12});
    ent.singlePrimitive = 100;
    ent.singleWrapper = 47;
    ent.primitiveArray = new short[] {5, 93};
    ent.wrapperArray = new Short[] { 55, 16, 99 };
    ds.save(ent);
    final Shorts loaded = ds.get(ent);

    Assert.assertNotNull(loaded.id);

    Assert.assertArrayEquals(ent.shorts.get(0), loaded.shorts.get(0));
    Assert.assertArrayEquals(ent.shortPrimitives.get(0), loaded.shortPrimitives.get(0));

    Assert.assertEquals(ent.singlePrimitive, loaded.singlePrimitive, 0);
    Assert.assertEquals(ent.singleWrapper, loaded.singleWrapper, 0);

    Assert.assertArrayEquals(ent.primitiveArray, loaded.primitiveArray);
    Assert.assertArrayEquals(ent.wrapperArray, loaded.wrapperArray);
  }
}
