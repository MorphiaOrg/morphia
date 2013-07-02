package com.google.code.morphia.mapping.primitives;


import java.util.ArrayList;
import java.util.List;

import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Test;
import com.google.code.morphia.TestBase;
import com.google.code.morphia.annotations.Id;


public class ByteMappingTest extends TestBase {
  private static class Bytes {
    @Id
    ObjectId id;
    final List<Byte[]> bytes = new ArrayList<Byte[]>();
    final List<byte[]> bytePrimitives = new ArrayList<byte[]>();
    byte singlePrimitive;
    Byte singleWrapper;
    byte[] primitiveArray;
    Byte[] wrapperArray;
  }


  @Test
  public void testMapping() throws Exception {
    morphia.map(Bytes.class);
    final Bytes ent = new Bytes();
    ent.bytes.add(new Byte[] {1, 2});
    ent.bytePrimitives.add(new byte[] {2, 3, 12});
    ent.singlePrimitive = 100;
    ent.singleWrapper = 47;
    ent.primitiveArray = new byte[] {5, 93};
    ent.wrapperArray = new Byte[] { 55, 16, 99 };
    ds.save(ent);
    final Bytes loaded = ds.get(ent);

    Assert.assertNotNull(loaded.id);

    Assert.assertArrayEquals(ent.bytes.get(0), loaded.bytes.get(0));
    Assert.assertArrayEquals(ent.bytePrimitives.get(0), loaded.bytePrimitives.get(0));

    Assert.assertEquals(ent.singlePrimitive, loaded.singlePrimitive, 0);
    Assert.assertEquals(ent.singleWrapper, loaded.singleWrapper, 0);

    Assert.assertArrayEquals(ent.primitiveArray, loaded.primitiveArray);
    Assert.assertArrayEquals(ent.wrapperArray, loaded.wrapperArray);
  }
}
