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

public class FloatMappingTest extends TestBase {
    @Test
    public void testMapping() {
        getMapper().map(Floats.class);
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
        final Floats loaded = getDs().find(Floats.class)
                                     .filter(eq("_id", ent.id))
                                     .first();

        Assert.assertNotNull(loaded.id);

        Assert.assertEquals(loaded.listWrapperArray.get(0), ent.listWrapperArray.get(0));
        Assert.assertEquals(loaded.listWrapper, ent.listWrapper);
        Assert.assertEquals(loaded.listPrimitiveArray.get(0), ent.listPrimitiveArray.get(0), 0.0f);

        Assert.assertEquals(loaded.singlePrimitive, ent.singlePrimitive, 0);
        Assert.assertEquals(loaded.singleWrapper, ent.singleWrapper, 0);

        Assert.assertEquals(loaded.primitiveArray, ent.primitiveArray, 0.0f);
        Assert.assertEquals(loaded.wrapperArray, ent.wrapperArray);

        Assert.assertEquals(loaded.nestedPrimitiveArray, ent.nestedPrimitiveArray);
        Assert.assertEquals(loaded.nestedWrapperArray, ent.nestedWrapperArray);
    }

    @Entity
    private static class Floats {
        private final List<Float[]> listWrapperArray = new ArrayList<>();
        private final List<float[]> listPrimitiveArray = new ArrayList<>();
        private final List<Float> listWrapper = new ArrayList<>();
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
