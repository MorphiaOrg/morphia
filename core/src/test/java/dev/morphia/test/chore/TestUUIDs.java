package dev.morphia.test.chore;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.PreLoad;
import dev.morphia.annotations.Transient;
import dev.morphia.config.MorphiaConfig;
import dev.morphia.test.CustomMorphiaConfig;
import dev.morphia.test.JUnitMorphiaTestBase;
import dev.morphia.test.MorphiaConfigProvider;
import dev.morphia.test.models.FacebookUser;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static dev.morphia.query.filters.Filters.eq;
import static org.junit.jupiter.api.Assertions.*;

@CustomMorphiaConfig
public class TestUUIDs extends JUnitMorphiaTestBase implements MorphiaConfigProvider {

    @Override
    public MorphiaConfig provideMorphiaConfig() {
        return buildConfig(ContainsUUID.class, FacebookUser.class);
    }

    @Test
    public void testUUID() {
        final ContainsUUID uuid = new ContainsUUID();
        final UUID before = uuid.uuid;
        getDs().save(uuid);
        final ContainsUUID loaded = getDs().find(ContainsUUID.class)
                .iterator()
                .next();
        assertNotNull(loaded);
        assertNotNull(loaded.id);
        assertNotNull(loaded.uuid);
        assertEquals(before, loaded.uuid);
    }

    @Test
    public void testUuidId() {
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
        ContainsUuidId example = new ContainsUuidId();
        getDs().save(example);

        ContainsUuidId loaded = getDs().find(ContainsUuidId.class).first();
        assertNotNull(loaded);
        assertEquals(loaded.id, example.id);
        assertTrue(loaded.preload);
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
