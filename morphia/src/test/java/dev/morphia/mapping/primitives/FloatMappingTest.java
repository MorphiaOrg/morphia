package dev.morphia.mapping.primitives;


import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Test;
import dev.morphia.TestBase;
import dev.morphia.annotations.Id;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FloatMappingTest extends TestBase {
    @Test
    public void testMapping() throws Exception {
        getMorphia().map(Floats.class);
        final Floats ent = new Floats();
        ent.listWrapperArray.add(new Float[]{1.1f, 2.2f});
        ent.listPrimitiveArray.add(new float[]{2.0f, 3.6f, 12.4f});
        ent.listWrapper.addAll(Arrays.asList(1.1f, 2.2f));
        ent.singlePrimitive = 100.0f;
        ent.singleWrapper = 40.7f;
        ent.primitiveArray = new float[]{5.0f, 93.5f};
        ent.wrapperArray = new Float[]{55.7f, 16.2f, 99.9999f};
        ent.nestedPrimitiveArray = new float[][]{{}, {5.0f, 93.5f}};
        ent.nestedWrapperArray = new Float[][]{{55.7f, 16.2f, 99.9999f}, {}};
        getDs().save(ent);
        final Floats loaded = getDs().get(ent);

        Assert.assertNotNull(loaded.id);

        Assert.assertArrayEquals(ent.listWrapperArray.get(0), loaded.listWrapperArray.get(0));
        Assert.assertEquals(ent.listWrapper, loaded.listWrapper);
        Assert.assertArrayEquals(ent.listPrimitiveArray.get(0), loaded.listPrimitiveArray.get(0), 0.0f);

        Assert.assertEquals(ent.singlePrimitive, loaded.singlePrimitive, 0);
        Assert.assertEquals(ent.singleWrapper, loaded.singleWrapper, 0);

        Assert.assertArrayEquals(ent.primitiveArray, loaded.primitiveArray, 0.0f);
        Assert.assertArrayEquals(ent.wrapperArray, loaded.wrapperArray);

        Assert.assertArrayEquals(ent.nestedPrimitiveArray, loaded.nestedPrimitiveArray);
        Assert.assertArrayEquals(ent.nestedWrapperArray, loaded.nestedWrapperArray);
    }

    private static class Floats {
        private final List<Float[]> listWrapperArray = new ArrayList<Float[]>();
        private final List<float[]> listPrimitiveArray = new ArrayList<float[]>();
        private final List<Float> listWrapper = new ArrayList<Float>();
        @Id
        private ObjectId id;
        private float singlePrimitive;
        private Float singleWrapper;
        private float[] primitiveArray;
        private Float[] wrapperArray;
        private float[][] nestedPrimitiveArray;
        private Float[][] nestedWrapperArray;
    }
}
