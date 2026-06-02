package dev.morphia.test.mapping.primitives;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import dev.morphia.Datastore;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.test.TestBase;

import org.bson.types.ObjectId;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static dev.morphia.query.filters.Filters.eq;

public class ByteMappingTest extends TestBase {

    @Test
    public void testMapping() {
        getMapper().map(Bytes.class);
        final Bytes ent = new Bytes();
        ent.listWrapperArray.add(new Byte[] { 1, 2 });
        ent.listPrimitiveArray.add(new byte[] { 2, 3, 12 });
        ent.listWrapper.addAll(Arrays.asList((byte) 148, (byte) 6, (byte) 255));
        ent.singlePrimitive = 100;
        ent.singleWrapper = 47;
        ent.primitiveArray = new byte[] { 5, 93 };
        ent.wrapperArray = new Byte[] { 55, 16, 99 };
        ent.nestedPrimitiveArray = new byte[][] { { 1, 2 }, { 3, 4 } };
        ent.nestedWrapperArray = new Byte[][] { { 1, 2 }, { 3, 4 } };
        getDs().save(ent);
        final Datastore datastore = getDs();

        final Bytes loaded = datastore.find(Bytes.class).filter(eq("_id", ent.id)).first();

        Assertions.assertNotNull(loaded.id);

        Assertions.assertArrayEquals(ent.listWrapperArray.get(0), loaded.listWrapperArray.get(0));
        Assertions.assertArrayEquals(ent.listPrimitiveArray.get(0), loaded.listPrimitiveArray.get(0));
        Assertions.assertEquals(ent.listWrapper, loaded.listWrapper);

        Assertions.assertEquals(ent.singlePrimitive, loaded.singlePrimitive, 0);
        Assertions.assertEquals(ent.singleWrapper, loaded.singleWrapper, 0);

        Assertions.assertArrayEquals(ent.primitiveArray, loaded.primitiveArray);
        Assertions.assertArrayEquals(ent.wrapperArray, loaded.wrapperArray);
        Assertions.assertArrayEquals(ent.nestedPrimitiveArray, loaded.nestedPrimitiveArray);
        Assertions.assertArrayEquals(ent.nestedWrapperArray, loaded.nestedWrapperArray);
    }

    @Entity
    private static class Bytes {
        private final List<Byte[]> listWrapperArray = new ArrayList<>();
        private final List<byte[]> listPrimitiveArray = new ArrayList<>();
        private final List<Byte> listWrapper = new ArrayList<>();
        @Id
        private ObjectId id;
        private byte singlePrimitive;
        private Byte singleWrapper;
        private byte[] primitiveArray;
        private Byte[] wrapperArray;
        private byte[][] nestedPrimitiveArray;
        private Byte[][] nestedWrapperArray;
    }
}
