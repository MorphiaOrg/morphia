package dev.morphia.test.config;

import dev.morphia.Datastore;
import dev.morphia.Morphia;
import dev.morphia.config.MorphiaConfigHelper;
import dev.morphia.mapping.MapperOptions;
import dev.morphia.test.TestBase;

import org.testng.annotations.Test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class TestConfig extends TestBase {
    @Test
    public void testConfig() {
        Datastore datastore = Morphia.createDatastore(getMongoClient());
    }

    @Test
    public void testConfigWithMapperOptions() throws IOException {
        var root = findProjectRoot();

        var docsTarget = new File(root, "docs/modules/ROOT/pages");

        try (var writer = new FileWriter(new File(docsTarget, "complete-morphia-config.properties"))) {
            var configContents = MorphiaConfigHelper.dumpConfigurationFile(MapperOptions.DEFAULT, "dummy", true);
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

    @Test
    public void testLegacyConfigDump() {
        MapperOptions mapperOptions = MapperOptions.legacy().build();
        MorphiaConfigHelper.dumpConfigurationFile(mapperOptions, "test-database");
    }

}
