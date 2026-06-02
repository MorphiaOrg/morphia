package dev.morphia.test.mapping.primitives;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.test.TestBase;

import org.bson.types.ObjectId;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static dev.morphia.query.filters.Filters.eq;

public class ByteArrayMappingTest extends TestBase {
    @Test
    public void testCharMapping() {
        getMapper().map(ContainsByteArray.class);
        final ContainsByteArray entity = new ContainsByteArray();
        entity.ba = new Byte[] { 6, 9, 1, -122 };
        getDs().save(entity);
        final ContainsByteArray loaded = getDs().find(ContainsByteArray.class)
                .filter(eq("_id", entity.id))
                .first();

        for (int i = 0; i < entity.ba.length; i++) {
            Assertions.assertEquals(entity.ba[i], loaded.ba[i]);
        }
        Assertions.assertNotNull(loaded.id);
    }

    @Entity
    private static class ContainsByteArray {
        @Id
        private ObjectId id;
        private Byte[] ba;
    }

}
