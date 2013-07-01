package com.google.code.morphia.mapping;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Test;
import com.google.code.morphia.TestBase;
import com.google.code.morphia.annotations.Id;


public class FloatMappingTest extends TestBase {
  private static class Floats {
    @Id
    ObjectId id;
    final List<Float[]> floats = new ArrayList<Float[]>();
    final List<float[]> floatPrimitives = new ArrayList<float[]>();
    final List<Float> list = new ArrayList<Float>();
    float singlePrimitive;
    Float singleWrapper;
    float[] primitiveArray;
    Float[] wrapperArray;
  }


  @Test
  public void testMapping() throws Exception {
    morphia.map(Floats.class);
    final Floats ent = new Floats();
    ent.floats.add(new Float[] {1.1f, 2.2f});
    ent.floatPrimitives.add(new float[] {2.0f, 3.6f, 12.4f});
    ent.list.addAll(Arrays.asList(1.1f, 2.2f));
    ent.singlePrimitive = 100.0f;
    ent.singleWrapper = 40.7f;
    ent.primitiveArray = new float[] {5.0f, 93.5f};
    ent.wrapperArray = new Float[] { 55.7f, 16.2f, 99.9999f };
    ds.save(ent);
    final Floats loaded = ds.get(ent);

    Assert.assertNotNull(loaded.id);

    compare("floats", ent.floats.get(0), loaded.floats.get(0));
    compare("list", ent.list.toArray(new Float[0]), loaded.list.toArray(new Float[0]));
    Assert.assertArrayEquals(ent.floatPrimitives.get(0), loaded.floatPrimitives.get(0), 0.0f);

    Assert.assertEquals(ent.singlePrimitive, loaded.singlePrimitive, 0);
    Assert.assertEquals(ent.singleWrapper, loaded.singleWrapper, 0);

    Assert.assertArrayEquals(ent.primitiveArray, loaded.primitiveArray, 0.0f);
    compare("wrapperArray", ent.wrapperArray, loaded.wrapperArray);
  }

  private void compare(final String property, final Float[] expected, final Float[] received) {
    Assert.assertEquals(String.format("%s lengths should match", property), expected.length, received.length);
    for (int i = 0; i < expected.length; i++) {
      Assert.assertEquals(String.format("%s[%s] should match", property, i), expected[i], received[i], 0);
    }
  }
}
