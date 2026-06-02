package dev.morphia.test.mapping.primitives;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.test.TestBase;

import org.bson.types.ObjectId;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static dev.morphia.query.filters.Filters.eq;

public class DoubleMappingTest extends TestBase {
    @Test
    public void testMapping() {
        getMapper().map(Doubles.class);
        final Doubles ent = new Doubles();
        ent.listWrapperArray.add(new Double[] { 1.1, 2.2 });
        ent.listPrimitiveArray.add(new double[] { 2.0, 3.6, 12.4 });
        ent.listWrapper.addAll(Arrays.asList(1.1, 2.2));
        ent.singlePrimitive = 100.0;
        ent.singleWrapper = 40.7;
        ent.primitiveArray = new double[] { 5.0, 93.5 };
        ent.wrapperArray = new Double[] { 55.7, 16.2, 99.9999 };
        ent.nestedPrimitiveArray = new double[][] { { 42.0, 49152.0 }, { 5.0, 93.5 } };
        ent.nestedWrapperArray = new Double[][] { { 42.0, 49152.0 }, { 5.0, 93.5 } };
        getDs().save(ent);

        final Doubles loaded = getDs().find(Doubles.class)
                .filter(eq("_id", ent.id))
                .first();
        Assertions.assertNotNull(loaded.id);

        Assertions.assertArrayEquals(ent.listWrapperArray.get(0), loaded.listWrapperArray.get(0));
        Assertions.assertEquals(ent.listWrapper, loaded.listWrapper);
        Assertions.assertArrayEquals(ent.listPrimitiveArray.get(0), loaded.listPrimitiveArray.get(0), 0.0);

        Assertions.assertEquals(ent.singlePrimitive, loaded.singlePrimitive, 0);
        Assertions.assertEquals(ent.singleWrapper, loaded.singleWrapper, 0);

        Assertions.assertArrayEquals(ent.primitiveArray, loaded.primitiveArray, 0.0);
        Assertions.assertArrayEquals(ent.wrapperArray, loaded.wrapperArray);
        Assertions.assertArrayEquals(ent.nestedPrimitiveArray, loaded.nestedPrimitiveArray);
        Assertions.assertArrayEquals(ent.nestedWrapperArray, loaded.nestedWrapperArray);
    }

    @Entity
    private static class Doubles {
        private final List<Double[]> listWrapperArray = new ArrayList<>();
        private final List<double[]> listPrimitiveArray = new ArrayList<>();
        private final List<Double> listWrapper = new ArrayList<>();
        @Id
        private ObjectId id;
        private double singlePrimitive;
        private Double singleWrapper;
        private double[] primitiveArray;
        private Double[] wrapperArray;
        private double[][] nestedPrimitiveArray;
        private Double[][] nestedWrapperArray;
    }
}
