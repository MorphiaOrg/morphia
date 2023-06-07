package dev.morphia.test.config;

import dev.morphia.Datastore;
import dev.morphia.Morphia;
import dev.morphia.mapping.MapperOptions;
import dev.morphia.test.TestBase;

import org.testng.annotations.Test;

public class TestConfig extends TestBase {
    @Test
    public void testConfig() {
        Datastore datastore = Morphia.createDatastore(getMongoClient());
    }

    @Test
    public void testConfigWithMapperOptions() {
        Datastore datastore = Morphia.createDatastore(getMongoClient(), "dummy", MapperOptions.DEFAULT);
    }

}
