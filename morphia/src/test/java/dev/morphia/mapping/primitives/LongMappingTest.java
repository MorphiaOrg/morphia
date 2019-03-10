package dev.morphia.mapping.primitives;


import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Test;
import dev.morphia.TestBase;
import dev.morphia.annotations.Id;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class LongMappingTest extends TestBase {
    @Test
    public void testMapping() throws Exception {
        getMorphia().map(Longs.class);
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
        final Longs loaded = getDs().get(ent);

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

    private static class Longs {
        private final List<Long[]> listWrapperArray = new ArrayList<Long[]>();
        private final List<long[]> listPrimitiveArray = new ArrayList<long[]>();
        private final List<Long> listWrapper = new ArrayList<Long>();
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
