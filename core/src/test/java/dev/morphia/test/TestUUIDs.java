package dev.morphia.test;

import java.util.UUID;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.PreLoad;
import dev.morphia.annotations.Transient;
import dev.morphia.test.models.FacebookUser;

import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static dev.morphia.query.filters.Filters.eq;

public class TestUUIDs extends TestBase {

    public TestUUIDs() {
        super(buildConfig(ContainsUUID.class, FacebookUser.class));
    }

    @Test
    public void testUUID() {
        final ContainsUUID uuid = new ContainsUUID();
        final UUID before = uuid.uuid;
        getDs().save(uuid);
        final ContainsUUID loaded = getDs().find(ContainsUUID.class)
                .iterator()
                .next();
        Assertions.assertNotNull(loaded);
        Assertions.assertNotNull(loaded.id);
        Assertions.assertNotNull(loaded.uuid);
        Assertions.assertEquals(loaded.uuid, before);
    }

    @Test
    public void testUuidId() {
        final ContainsUuidId uuidId = new ContainsUuidId();
        final UUID before = uuidId.id;
        getDs().save(uuidId);
        final ContainsUuidId loaded = getDs().find(ContainsUuidId.class).filter(eq("_id", before)).first();
        Assertions.assertNotNull(loaded);
        Assertions.assertNotNull(loaded.id);
        Assertions.assertEquals(loaded.id, before);
    }

    @Test
    void checkLifecycleForUuid() {
        ContainsUuidId example = new ContainsUuidId();
        getDs().save(example);

        ContainsUuidId loaded = getDs().find(ContainsUuidId.class).first();
        Assertions.assertEquals(example.id, loaded.id);
        Assertions.assertTrue(loaded.preload);
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
