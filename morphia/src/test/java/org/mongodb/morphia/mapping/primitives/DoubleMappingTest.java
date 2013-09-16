package org.mongodb.morphia.mapping.primitives;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Test;
import org.mongodb.morphia.TestBase;
import org.mongodb.morphia.annotations.Id;

public class DoubleMappingTest extends TestBase {
  private static class Doubles {
    @Id
    ObjectId id;
    final List<Double[]> listWrapperArray = new ArrayList<Double[]>();
    final List<double[]> listPrimitiveArray = new ArrayList<double[]>();
    final List<Double> listWrapper = new ArrayList<Double>();
    double singlePrimitive;
    Double singleWrapper;
    double[] primitiveArray;
    Double[] wrapperArray;
    double[][] nestedPrimitiveArray;
    Double[][] nestedWrapperArray;
  }


  @Test
  public void testMapping() throws Exception {
    morphia.map(Doubles.class);
    final Doubles ent = new Doubles();
    ent.listWrapperArray.add(new Double[] {1.1, 2.2});
    ent.listPrimitiveArray.add(new double[] {2.0, 3.6, 12.4});
    ent.listWrapper.addAll(Arrays.asList(1.1, 2.2));
    ent.singlePrimitive = 100.0;
    ent.singleWrapper = 40.7;
    ent.primitiveArray = new double[] {5.0, 93.5};
    ent.wrapperArray = new Double[] { 55.7, 16.2, 99.9999 };
    ent.nestedPrimitiveArray = new double[][] {{42.0, 49152.0}, {5.0, 93.5}};
    ent.nestedWrapperArray = new Double[][] {{42.0, 49152.0}, {5.0, 93.5}};
    ds.save(ent);

    final Doubles loaded = ds.get(ent);
    Assert.assertNotNull(loaded.id);

    Assert.assertArrayEquals(ent.listWrapperArray.get(0), loaded.listWrapperArray.get(0));
    Assert.assertEquals(ent.listWrapper, loaded.listWrapper);
    Assert.assertArrayEquals(ent.listPrimitiveArray.get(0), loaded.listPrimitiveArray.get(0), 0.0);

    Assert.assertEquals(ent.singlePrimitive, loaded.singlePrimitive, 0);
    Assert.assertEquals(ent.singleWrapper, loaded.singleWrapper, 0);

    Assert.assertArrayEquals(ent.primitiveArray, loaded.primitiveArray, 0.0);
    Assert.assertArrayEquals(ent.wrapperArray, loaded.wrapperArray);
    Assert.assertArrayEquals(ent.nestedPrimitiveArray, loaded.nestedPrimitiveArray);
    Assert.assertArrayEquals(ent.nestedWrapperArray, loaded.nestedWrapperArray);
  }
}
