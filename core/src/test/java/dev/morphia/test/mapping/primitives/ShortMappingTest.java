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


public class ShortMappingTest extends TestBase {
    @Test
    public void testMapping() {
        getMapper().map(Shorts.class);
        final Shorts ent = new Shorts();
        ent.listWrapperArray.add(new Short[]{1, 2});
        ent.listPrimitiveArray.add(new short[]{2, 3, 12});
        ent.listWrapper.addAll(Arrays.asList((short) 1, (short) 2));
        ent.singlePrimitive = 100;
        ent.singleWrapper = 47;
        ent.primitiveArray = new short[]{5, 93};
        ent.wrapperArray = new Short[]{55, 16, 99};
        ent.nestedPrimitiveArray = new short[][]{{5, 93}, {88}};
        ent.nestedWrapperArray = new Short[][]{{55, 16, 99}, {-47}};
        getDs().save(ent);
        final Shorts loaded = getDs().find(Shorts.class)
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
    private static class Shorts {
        private final List<Short[]> listWrapperArray = new ArrayList<>();
        private final List<short[]> listPrimitiveArray = new ArrayList<>();
        private final List<Short> listWrapper = new ArrayList<>();
        @Id
        private ObjectId id;
        private short singlePrimitive;
        private Short singleWrapper;
        private short[] primitiveArray;
        private Short[] wrapperArray;
        private short[][] nestedPrimitiveArray;
        private Short[][] nestedWrapperArray;
    }
}
