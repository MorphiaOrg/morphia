package com.google.code.morphia.mapping;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Test;
import com.google.code.morphia.TestBase;
import com.google.code.morphia.annotations.Id;


/**
 * @author scotthernandez
 */
public class DoubleMappingTest extends TestBase {
  private static class Doubles {
    @Id
    ObjectId id;
    final List<Double[]> doubles = new ArrayList<Double[]>();
    final List<double[]> doublePrimitives = new ArrayList<double[]>();
    final List<Double> list = new ArrayList<Double>();
    double singlePrimitive;
    Double singleWrapper;
    double[] primitiveArray;
    Double[] wrapperArray;
  }


  @Test
  public void testMapping() throws Exception {
    morphia.map(Doubles.class);
    final Doubles ent = new Doubles();
    ent.doubles.add(new Double[] {1.1, 2.2});
    ent.list.addAll(Arrays.asList(1.1, 2.2));
    ent.doublePrimitives.add(new double[] {2.0, 3.6, 12.4});
    ent.singlePrimitive = 100.0;
    ent.singleWrapper = 40.7;
    ent.primitiveArray = new double[] {5.0, 93.5};
    ent.wrapperArray = new Double[] { 55.7, 16.2, 99.9999 };
    ds.save(ent);
    final Doubles loaded = ds.get(ent);

    Assert.assertNotNull(loaded.id);

    compare("doubles", ent.doubles.get(0), loaded.doubles.get(0));
    compare("list", ent.list.toArray(new Double[0]), loaded.list.toArray(new Double[0]));
    Assert.assertArrayEquals(ent.doublePrimitives.get(0), loaded.doublePrimitives.get(0), 0.0);

    Assert.assertEquals(ent.singlePrimitive, loaded.singlePrimitive, 0);
    Assert.assertEquals(ent.singleWrapper, loaded.singleWrapper, 0);

    Assert.assertArrayEquals(ent.primitiveArray, loaded.primitiveArray, 0.0);
    compare("wrapperArray", ent.wrapperArray, loaded.wrapperArray);
  }

  private void compare(final String property, final Double[] expected, final Double[] received) {
    Assert.assertEquals(String.format("%s lengths should match", property), expected.length, received.length);
    for (int i = 0; i < expected.length; i++) {
      Assert.assertEquals(String.format("%s[%s] should match", property, i), expected[i], received[i], 0);
    }
  }
}
