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

public class LongMappingTest extends TestBase {
    @Test
    public void testMapping() {
        getMapper().map(Longs.class);
        final Longs ent = new Longs();
        ent.listWrapperArray.add(new Long[] { 1L, 2L });
        ent.listPrimitiveArray.add(new long[] { 2, 3, 12 });
        ent.listWrapper.addAll(Arrays.asList(1L, 2L));
        ent.singlePrimitive = 100;
        ent.singleWrapper = 47L;
        ent.primitiveArray = new long[] { 5, 93 };
        ent.wrapperArray = new Long[] { 55L, 16L, 99L };
        ent.nestedPrimitiveArray = new long[][] { { 0 }, { 5, 93 } };
        ent.nestedWrapperArray = new Long[][] { { 99L, 1L }, { 55L, 16L, 99L } };
        getDs().save(ent);
        final Longs loaded = getDs().find(Longs.class)
                .filter(eq("_id", ent.id))
                .first();

        Assertions.assertNotNull(loaded.id);

        Assertions.assertArrayEquals(ent.listWrapperArray.get(0), loaded.listWrapperArray.get(0));
        Assertions.assertArrayEquals(ent.listPrimitiveArray.get(0), loaded.listPrimitiveArray.get(0));

        Assertions.assertEquals(ent.singlePrimitive, loaded.singlePrimitive, 0);
        Assertions.assertEquals(ent.singleWrapper, loaded.singleWrapper, 0);

        Assertions.assertArrayEquals(ent.primitiveArray, loaded.primitiveArray);
        Assertions.assertArrayEquals(ent.wrapperArray, loaded.wrapperArray);
        Assertions.assertArrayEquals(ent.nestedPrimitiveArray, loaded.nestedPrimitiveArray);
        Assertions.assertArrayEquals(ent.nestedWrapperArray, loaded.nestedWrapperArray);
    }

    @Entity
    private static class Longs {
        private final List<Long[]> listWrapperArray = new ArrayList<>();
        private final List<long[]> listPrimitiveArray = new ArrayList<>();
        private final List<Long> listWrapper = new ArrayList<>();
        @Id
        private ObjectId id;
        private long singlePrimitive;
        private Long singleWrapper;
        private long[] primitiveArray;
        private Long[] wrapperArray;
        private long[][] nestedPrimitiveArray;
        private Long[][] nestedWrapperArray;
    }
}
