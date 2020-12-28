package dev.morphia.test;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.PreLoad;
import dev.morphia.annotations.Transient;
import dev.morphia.query.FindOptions;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.List;
import java.util.UUID;

import static dev.morphia.query.experimental.filters.Filters.eq;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

public class TestUUIDs extends TestBase {

    @Test
    public void testUUID() {
        getMapper().map(ContainsUUID.class);
        final ContainsUUID uuid = new ContainsUUID();
        final UUID before = uuid.uuid;
        getDs().save(uuid);
        final ContainsUUID loaded = getDs().find(ContainsUUID.class)
                                           .iterator(new FindOptions().limit(1))
                                           .next();
        assertNotNull(loaded);
        assertNotNull(loaded.id);
        assertNotNull(loaded.uuid);
        assertEquals(before, loaded.uuid);
    }

    @Test
    public void testUuidId() {
        getMapper().map(List.of(ContainsUuidId.class));
        final ContainsUuidId uuidId = new ContainsUuidId();
        final UUID before = uuidId.id;
        getDs().save(uuidId);
        final ContainsUuidId loaded = getDs().find(ContainsUuidId.class).filter(eq("_id", before)).first();
        assertNotNull(loaded);
        assertNotNull(loaded.id);
        assertEquals(before, loaded.id);
    }

    @Test
    void checkLifecycleForUuid() {
        getDs().getMapper().map(ContainsUuidId.class);

        ContainsUuidId example = new ContainsUuidId();
        getDs().save(example);

        ContainsUuidId loaded = getDs().find(ContainsUuidId.class).first();
        Assert.assertEquals(loaded.id, example.id);
        Assert.assertTrue(loaded.preload);
    }

    @Entity(useDiscriminator = false)
    private static class ContainsUUID {
        private final UUID uuid = UUID.randomUUID();
        @Id
        private ObjectId id;
    }

    @Entity(useDiscriminator = false)
    private static class ContainsUuidId {
        @Id
        private final UUID id = UUID.randomUUID();
        @Transient
        boolean preload = false;

        @PreLoad
        void preLoad(Document doc) {
            preload = true;
        }
    }
}
