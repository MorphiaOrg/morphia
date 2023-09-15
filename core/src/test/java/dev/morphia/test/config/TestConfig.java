package dev.morphia.test.config;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import dev.morphia.Datastore;
import dev.morphia.Morphia;
import dev.morphia.config.ManualMorphiaConfig;
import dev.morphia.config.MorphiaConfig;
import dev.morphia.config.MorphiaConfigHelper;
import dev.morphia.mapping.MapperOptions;
import dev.morphia.mapping.MappingException;
import dev.morphia.test.TestBase;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertThrows;

public class TestConfig extends TestBase {
    @Test
    public void testConfig() {
        Datastore datastore = Morphia.createDatastore(getMongoClient());
    }

    @Test
    public void loadSpecificConfigFiles() {
        var config = MorphiaConfig.load("META-INF/morphia-config-packageless.properties");

        assertEquals(config.database(), "morphia");
        assertEquals(config.packages(), List.of(".*"));

        // this will fail because some of the test entities are intentionally invalid to test validations.  But that it fails at all
        // means that the mapper is scanning *all* the classes as expected when no packages are specified.
        assertThrows(MappingException.class, () -> {
            Datastore datastore = Morphia.createDatastore(getMongoClient(), config);
            assertFalse(datastore.getMapper().getMappedEntities().isEmpty(), "Should find packages to map by default");
        });
    }

    @Test
    public void generateExampleConfigFiles() throws IOException {
        var root = findProjectRoot();

        var docsTarget = new File(root, "docs/modules/ROOT/examples");

        try (var writer = new FileWriter(new File(docsTarget, "complete-morphia-config.properties"))) {
            var configContents = MorphiaConfigHelper.dumpConfigurationFile(new ManualMorphiaConfig(), true);
            writer.write(configContents);
            writer.flush();
        }

        try (var writer = new FileWriter(new File(docsTarget, "minimal-morphia-config.properties"))) {
            var configContents = MorphiaConfigHelper.dumpConfigurationFile(new ManualMorphiaConfig(), false);
            writer.write(configContents);
            writer.flush();
        }

        try (var writer = new FileWriter(new File(docsTarget, "legacy-morphia-config.properties"))) {
            var configContents = MorphiaConfigHelper.dumpConfigurationFile(new ManualMorphiaConfig()
                    .legacy(), false);
            writer.write(configContents);
            writer.flush();
        }
    }

    private File findProjectRoot() {
        var root = new File(".").getAbsoluteFile();
        while (root != null && !new File(root, ".git").exists()) {
            root = root.getParentFile();
        }

        if (root == null) {
            throw new IllegalStateException("Could not find .git directory");
        }
        return root;
    }
}
