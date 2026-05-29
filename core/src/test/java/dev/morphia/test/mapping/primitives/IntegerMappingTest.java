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

public class IntegerMappingTest extends TestBase {
    @Test
    public void testMapping() {
        getMapper().map(Integers.class);
        final Integers ent = new Integers();
        ent.listWrapperArray.add(new Integer[] { 1, 2 });
        ent.listPrimitiveArray.add(new int[] { 2, 3, 12 });
        ent.listWrapper.addAll(Arrays.asList(1, 2));

        ent.singlePrimitive = 100;
        ent.singleWrapper = 47;
        ent.primitiveArray = new int[] { 5, 93 };
        ent.wrapperArray = new Integer[] { 55, 16, 99 };
        ent.nestedPrimitiveArray = new int[][] { {}, { 5, 93 } };
        ent.nestedWrapperArray = new Integer[][] { { 55, 16, 99 }, { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 }, { 0 } };
        getDs().save(ent);

        final Integers loaded = getDs().find(Integers.class)
                .filter(eq("_id", ent.id))
                .first();

        Assertions.assertNotNull(loaded.id);

        Assertions.assertArrayEquals(ent.listWrapperArray.get(0), loaded.listWrapperArray.get(0));
        Assertions.assertArrayEquals(ent.listWrapper.toArray(new Integer[0]), loaded.listWrapper.toArray(new Integer[0]));
        Assertions.assertArrayEquals(ent.listPrimitiveArray.get(0), loaded.listPrimitiveArray.get(0));

        Assertions.assertEquals(ent.singlePrimitive, loaded.singlePrimitive, 0);
        Assertions.assertEquals(ent.singleWrapper, loaded.singleWrapper, 0);

        Assertions.assertArrayEquals(ent.primitiveArray, loaded.primitiveArray);
        Assertions.assertArrayEquals(ent.wrapperArray, loaded.wrapperArray);
        Assertions.assertArrayEquals(ent.nestedPrimitiveArray, loaded.nestedPrimitiveArray);
        Assertions.assertArrayEquals(ent.nestedWrapperArray, loaded.nestedWrapperArray);
    }

    @Entity
    private static class Integers {
        private final List<Integer[]> listWrapperArray = new ArrayList<>();
        private final List<int[]> listPrimitiveArray = new ArrayList<>();
        private final List<Integer> listWrapper = new ArrayList<>();
        @Id
        private ObjectId id;
        private int singlePrimitive;
        private Integer singleWrapper;
        private int[] primitiveArray;
        private Integer[] wrapperArray;
        private int[][] nestedPrimitiveArray;
        private Integer[][] nestedWrapperArray;
    }
}
