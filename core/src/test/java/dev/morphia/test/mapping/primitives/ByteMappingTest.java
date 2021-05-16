package dev.morphia.test.mapping.primitives;


import dev.morphia.Datastore;
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


public class ByteMappingTest extends TestBase {

    @Test
    public void testMapping() {
        getMapper().map(Bytes.class);
        final Bytes ent = new Bytes();
        ent.listWrapperArray.add(new Byte[]{1, 2});
        ent.listPrimitiveArray.add(new byte[]{2, 3, 12});
        ent.listWrapper.addAll(Arrays.asList((byte) 148, (byte) 6, (byte) 255));
        ent.singlePrimitive = 100;
        ent.singleWrapper = 47;
        ent.primitiveArray = new byte[]{5, 93};
        ent.wrapperArray = new Byte[]{55, 16, 99};
        ent.nestedPrimitiveArray = new byte[][]{{1, 2}, {3, 4}};
        ent.nestedWrapperArray = new Byte[][]{{1, 2}, {3, 4}};
        getDs().save(ent);
        final Datastore datastore = getDs();

        final Bytes loaded = datastore.find(Bytes.class).filter(eq("_id", ent.id)).first();

        Assert.assertNotNull(loaded.id);

        Assert.assertEquals(loaded.listWrapperArray.get(0), ent.listWrapperArray.get(0));
        Assert.assertEquals(loaded.listPrimitiveArray.get(0), ent.listPrimitiveArray.get(0));
        Assert.assertEquals(loaded.listWrapper, ent.listWrapper);

        Assert.assertEquals(loaded.singlePrimitive, ent.singlePrimitive, 0);
        Assert.assertEquals(loaded.singleWrapper, ent.singleWrapper, 0);

        Assert.assertEquals(loaded.primitiveArray, ent.primitiveArray);
        Assert.assertEquals(loaded.wrapperArray, ent.wrapperArray);
        Assert.assertEquals(loaded.nestedPrimitiveArray, ent.nestedPrimitiveArray);
        Assert.assertEquals(loaded.nestedWrapperArray, ent.nestedWrapperArray);
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
