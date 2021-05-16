package dev.morphia.test.mapping.primitives;


import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.test.TestBase;
import org.bson.types.ObjectId;
import org.testng.Assert;
import org.testng.annotations.Test;

import static dev.morphia.query.experimental.filters.Filters.eq;

public class ByteArrayMappingTest extends TestBase {
    @Test
    public void testCharMapping() {
        getMapper().map(ContainsByteArray.class);
        final ContainsByteArray entity = new ContainsByteArray();
        entity.ba = new Byte[]{6, 9, 1, -122};
        getDs().save(entity);
        final ContainsByteArray loaded = getDs().find(ContainsByteArray.class)
                                                .filter(eq("_id", entity.id))
                                                .first();

        for (int i = 0; i < entity.ba.length; i++) {
            Assert.assertEquals(loaded.ba[i], entity.ba[i]);
        }
        Assert.assertNotNull(loaded.id);
    }

    @Entity
    private static class ContainsByteArray {
        @Id
        private ObjectId id;
        private Byte[] ba;
    }

}
