package dev.morphia.test.config;

import dev.morphia.Datastore;
import dev.morphia.Morphia;
import dev.morphia.test.TestBase;

import org.testng.annotations.Test;

public class TestConfig extends TestBase {
    @Test
    public void testConfig() {
        Datastore datastore = Morphia.createDatastore(getMongoClient());
    }

}
