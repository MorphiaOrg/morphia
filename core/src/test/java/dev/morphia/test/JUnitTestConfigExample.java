package dev.morphia.test;

import dev.morphia.Datastore;
import dev.morphia.Morphia;
import dev.morphia.MorphiaDatastore;
import dev.morphia.config.MorphiaConfig;
import dev.morphia.mapping.MappingException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JUnit 5 migration example of a configuration test
 * <p>
 * This demonstrates migrating a test that extends TestBase using the unified base class
 */
public class JUnitTestConfigExample extends JUnitMorphiaTestBase {

    @Test
    public void testConfig() {
        Datastore datastore = Morphia.createDatastore(getMongoClient());
    }

    @Test
    public void loadSpecificConfigFiles() {
        var config = MorphiaConfig.load("META-INF/morphia-config-packageless.properties");

        assertEquals("morphia", config.database());
        assertEquals(List.of(".*"), config.packages());

        // this will fail because some of the test entities are intentionally invalid to test validations.  But that it fails at all
        // means that the mapper is scanning *all* the classes as expected when no packages are specified.
        assertThrows(MappingException.class, () -> {
            MorphiaDatastore datastore = (MorphiaDatastore) Morphia.createDatastore(getMongoClient(), config);
            assertFalse(datastore.getMapper().getMappedEntities().isEmpty(), "Should find packages to map by default");
        });
    }
}