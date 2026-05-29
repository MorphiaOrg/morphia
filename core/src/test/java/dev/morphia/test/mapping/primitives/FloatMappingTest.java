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

public class FloatMappingTest extends TestBase {
    @Test
    public void testMapping() {
        getMapper().map(Floats.class);
        final Floats ent = new Floats();
        ent.listWrapperArray.add(new Float[] { 1.1f, 2.2f });
        ent.listPrimitiveArray.add(new float[] { 2.0f, 3.6f, 12.4f });
        ent.listWrapper.addAll(Arrays.asList(1.1f, 2.2f));
        ent.singlePrimitive = 100.0f;
        ent.singleWrapper = 40.7f;
        ent.primitiveArray = new float[] { 5.0f, 93.5f };
        ent.wrapperArray = new Float[] { 55.7f, 16.2f, 99.9999f };
        ent.nestedPrimitiveArray = new float[][] { {}, { 5.0f, 93.5f } };
        ent.nestedWrapperArray = new Float[][] { { 55.7f, 16.2f, 99.9999f }, {} };
        getDs().save(ent);
        final Floats loaded = getDs().find(Floats.class)
                .filter(eq("_id", ent.id))
                .first();

        Assertions.assertNotNull(loaded.id);

        Assertions.assertArrayEquals(ent.listWrapperArray.get(0), loaded.listWrapperArray.get(0));
        Assertions.assertEquals(ent.listWrapper, loaded.listWrapper);
        Assertions.assertArrayEquals(ent.listPrimitiveArray.get(0), loaded.listPrimitiveArray.get(0), 0.0f);

        Assertions.assertEquals(ent.singlePrimitive, loaded.singlePrimitive, 0);
        Assertions.assertEquals(ent.singleWrapper, loaded.singleWrapper, 0);

        Assertions.assertArrayEquals(ent.primitiveArray, loaded.primitiveArray, 0.0f);
        Assertions.assertArrayEquals(ent.wrapperArray, loaded.wrapperArray);

        Assertions.assertArrayEquals(ent.nestedPrimitiveArray, loaded.nestedPrimitiveArray);
        Assertions.assertArrayEquals(ent.nestedWrapperArray, loaded.nestedWrapperArray);
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
