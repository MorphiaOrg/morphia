package dev.morphia.test;

import java.util.concurrent.atomic.AtomicInteger;

import com.github.zafarkhaja.semver.Version;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoDatabase;

import dev.morphia.DatastoreImpl;
import dev.morphia.mapping.MapperOptions;

import org.testng.SkipException;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;

import static java.lang.String.format;

@SuppressWarnings("removal")
public class MorphiaTestSetup {
    protected static MorphiaContainer morphiaContainer;
    private static Object lock = new Object();
    private static AtomicInteger count = new AtomicInteger(0);

    @BeforeSuite
    public void startContainer() {
        synchronized (lock) {
            if (morphiaContainer == null) {
                morphiaContainer = new MorphiaContainer(false)
                        .start();
            }
        }
    }

    @AfterSuite
    public void stopContainer() {
        if (morphiaContainer != null) {
            morphiaContainer.close();
            morphiaContainer = null;
        }
    }

    protected void assumeTrue(boolean condition, String message) {
        if (!condition) {
            throw new SkipException(message);
        }
    }

    protected void checkMinDriverVersion(double version) {
        checkMinDriverVersion(Version.valueOf(version + ".0"));
    }

    protected void checkMinDriverVersion(Version version) {
        assumeTrue(driverIsAtLeastVersion(version),
                format("Server should be at least %s but found %s", version, getServerVersion()));
    }

    protected void checkMinServerVersion(double version) {
        checkMinServerVersion(Version.valueOf(version + ".0"));
    }

    protected void checkMinServerVersion(Version version) {
        assumeTrue(serverIsAtLeastVersion(version),
                format("Server should be at least %s but found %s", version, getServerVersion()));
    }

    protected MongoClient getMongoClient() {
        return morphiaContainer.getMongoClient();
    }

    protected Version getServerVersion() {
        return morphiaContainer.getServerVersion();
    }

    protected boolean isReplicaSet() {
        return morphiaContainer.runIsMaster().get("setName") != null;
    }

    /**
     * @param version the minimum version allowed
     * @return true if server is at least specified version
     */
    protected boolean serverIsAtLeastVersion(Version version) {
        return getServerVersion().greaterThanOrEqualTo(version);
    }

    protected void with(MorphiaContainer newContainer, Runnable block) {
        MorphiaContainer container = morphiaContainer;
        try (newContainer) {
            morphiaContainer = newContainer;
            block.run();
        } finally {
            morphiaContainer.close();
            morphiaContainer = container;
        }
    }

    protected void withOptions(MapperOptions options, Runnable block) {
        MapperOptions oldOptions = morphiaContainer.mapperOptions;
        DatastoreImpl datastore = morphiaContainer.datastore;
        MongoDatabase database = morphiaContainer.database;
        try {
            morphiaContainer.mapperOptions(options);
            block.run();
        } finally {
            morphiaContainer.mapperOptions(oldOptions);
            morphiaContainer.datastore = datastore;
            morphiaContainer.database = database;

        }
    }

    /**
     * @param version the minimum version allowed
     * @return true if server is at least specified version
     */
    private boolean driverIsAtLeastVersion(Version version) {
        String property = System.getProperty("driver.version");
        Version driverVersion = property != null ? Version.valueOf(property) : null;
        return driverVersion == null || driverVersion.greaterThanOrEqualTo(version);
    }
}
