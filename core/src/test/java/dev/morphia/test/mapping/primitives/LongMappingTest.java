package dev.morphia.test.mapping.primitives;


import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.test.TestBase;
import org.bson.types.ObjectId;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static dev.morphia.query.experimental.filters.Filters.eq;


public class LongMappingTest extends TestBase {
    @Test
    public void testMapping() {
        getMapper().map(Longs.class);
        final Longs ent = new Longs();
        ent.listWrapperArray.add(new Long[]{1L, 2L});
        ent.listPrimitiveArray.add(new long[]{2, 3, 12});
        ent.listWrapper.addAll(Arrays.asList(1L, 2L));
        ent.singlePrimitive = 100;
        ent.singleWrapper = 47L;
        ent.primitiveArray = new long[]{5, 93};
        ent.wrapperArray = new Long[]{55L, 16L, 99L};
        ent.nestedPrimitiveArray = new long[][]{{0}, {5, 93}};
        ent.nestedWrapperArray = new Long[][]{{99L, 1L}, {55L, 16L, 99L}};
        getDs().save(ent);
        final Longs loaded = getDs().find(Longs.class)
                                    .filter(eq("_id", ent.id))
                                    .first();

        Assert.assertNotNull(loaded.id);

        Assert.assertEquals(loaded.listWrapperArray.get(0), ent.listWrapperArray.get(0));
        Assert.assertEquals(loaded.listPrimitiveArray.get(0), ent.listPrimitiveArray.get(0));

        Assert.assertEquals(loaded.singlePrimitive, ent.singlePrimitive, 0);
        Assert.assertEquals(loaded.singleWrapper, ent.singleWrapper, 0);

        Assert.assertEquals(loaded.primitiveArray, ent.primitiveArray);
        Assert.assertEquals(loaded.wrapperArray, ent.wrapperArray);
        Assert.assertEquals(loaded.nestedPrimitiveArray, ent.nestedPrimitiveArray);
        Assert.assertEquals(loaded.nestedWrapperArray, ent.nestedWrapperArray);
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
