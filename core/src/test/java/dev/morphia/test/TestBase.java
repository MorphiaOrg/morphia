package dev.morphia.test;

import com.antwerkz.bottlerocket.clusters.MongoCluster;
import com.antwerkz.bottlerocket.clusters.ReplicaSet;
import com.antwerkz.bottlerocket.clusters.SingleNode;
import com.antwerkz.bottlerocket.configuration.types.Verbosity;
import com.github.zafarkhaja.semver.Version;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoClientSettings.Builder;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import dev.morphia.Datastore;
import dev.morphia.Morphia;
import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.MapperOptions;
import dev.morphia.query.DefaultQueryFactory;
import dev.morphia.query.LegacyQueryFactory;
import org.bson.Document;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.function.Supplier;

import static java.lang.String.format;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;

public abstract class TestBase {
    protected static final String TEST_DB_NAME = "morphia_test";
    private static final Logger LOG = LoggerFactory.getLogger(TestBase.class);
    private static MongoClient mongoClient;
    private final MapperOptions mapperOptions = MapperOptions.DEFAULT;

    private MongoDatabase database;
    private Datastore datastore;

    public void assertTrueLazy(boolean condition, Supplier<String> messageSupplier) {
        if (!condition) {
            fail(messageSupplier.get());
        }
    }

    @BeforeMethod
    public void beforeEach() {
        cleanup();
    }

    public MongoDatabase getDatabase() {
        if (database == null) {
            database = getDs().getDatabase();
        }
        return database;
    }

    public Datastore getDs() {
        if (datastore == null) {
            datastore = Morphia.createDatastore(getMongoClient(), TEST_DB_NAME);
        }
        return datastore;
    }

    public Mapper getMapper() {
        return getDs().getMapper();
    }

    public boolean isReplicaSet() {
        return runIsMaster().get("setName") != null;
    }

    public void lazyAssert(Supplier<String> messageSupplier, Runnable assertion) {
        try {
            assertion.run();
        } catch (AssertionError error) {
            fail(messageSupplier.get(), error);
        }
    }

    @DataProvider(name = "queryFactories")
    public Object[] queryFactories() {
        return new Object[]{
            new DefaultQueryFactory(),
            new LegacyQueryFactory()
        };
    }

    protected void assertCapped(Class<?> type, Integer max, Integer size) {
        Document result = getOptions(type);
        Assert.assertTrue(result.getBoolean("capped"));
        assertEquals(result.get("max"), max);
        assertEquals(result.get("size"), size);
    }

    protected void assertDocumentEquals(Object actual, Object expected) {
        assertDocumentEquals("", expected, actual);
    }

    protected void assertListEquals(Collection<?> actual, Collection<?> expected) {
        assertEquals(actual.size(), expected.size());
        expected.forEach(
            d -> assertTrueLazy(actual.contains(d), () -> format("Should have found <<%s>> in the actual list:%n%s", d, actual)));
    }

    protected void assumeTrue(boolean condition, String message) {
        if (!condition) {
            throw new SkipException(message);
        }
    }

    protected void checkMinServerVersion(double version) {
        assumeTrue(serverIsAtLeastVersion(version), "Server should be at least " + version + " but found " + getServerVersion());
    }

    protected int count(MongoCursor<?> cursor) {
        int count = 0;
        while (cursor.hasNext()) {
            cursor.next();
            count++;
        }
        return count;
    }

    protected int count(Iterator<?> iterator) {
        int count = 0;
        while (iterator.hasNext()) {
            count++;
            iterator.next();
        }
        return count;
    }

    protected void download(URL url, File file) throws IOException {
        LOG.info("Downloading zip data set to " + file);
        try (InputStream inputStream = url.openStream(); FileOutputStream outputStream = new FileOutputStream(file)) {
            byte[] read = new byte[49152];
            int count;
            while ((count = inputStream.read(read)) != -1) {
                outputStream.write(read, 0, count);
            }
        }
    }

    protected MongoCollection<Document> getDocumentCollection(Class<?> type) {
        return getDatabase().getCollection(getMapper().getEntityModel(type).getCollectionName());
    }

    protected List<Document> getIndexInfo(Class<?> clazz) {
        return getMapper().getCollection(clazz).listIndexes().into(new ArrayList<>());
    }

    protected MongoClient getMongoClient() {
        if (mongoClient == null) {
            startMongo();
        }
        return mongoClient;
    }

    @NotNull
    protected Document getOptions(Class<?> type) {
        MongoCollection<?> collection = getMapper().getCollection(type);
        Document result = getDatabase().runCommand(new Document("listCollections", 1.0)
                                                       .append("filter",
                                                           new Document("name", collection.getNamespace().getCollectionName())));

        Document cursor = (Document) result.get("cursor");
        return (Document) cursor.getList("firstBatch", Document.class)
                                .get(0)
                                .get("options");
    }

    protected double getServerVersion() {
        String version = (String) getMongoClient()
                                      .getDatabase("admin")
                                      .runCommand(new Document("serverStatus", 1))
                                      .get("version");
        return Double.parseDouble(version.substring(0, 3));
    }

    protected void insert(String collectionName, List<Document> list) {
        MongoCollection<Document> collection = getDatabase().getCollection(collectionName);
        collection.deleteMany(new Document());
        collection.insertMany(list);
    }

    /**
     * @param version must be a major version, e.g. 1.8, 2,0, 2.2
     * @return true if server is at least specified version
     */
    protected boolean serverIsAtLeastVersion(double version) {
        return getServerVersion() >= version;
    }

    protected String toString(Document document) {
        return document.toJson(getMapper().getCodecRegistry().get(Document.class));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void assertDocumentEquals(String path, Object expected, Object actual) {
        assertSameNullity(path, expected, actual);
        if (expected == null) {
            return;
        }
        assertSameType(path, expected, actual);

        if (expected instanceof Document) {
            for (Entry<String, Object> entry : ((Document) expected).entrySet()) {
                final String key = entry.getKey();
                Object expectedValue = entry.getValue();
                Object actualValue = ((Document) actual).get(key);
                assertDocumentEquals(path + "." + key, expectedValue, actualValue);
            }
        } else if (expected instanceof List) {
            List list = (List) expected;
            List copy = new ArrayList<>((List) actual);

            Object o;
            for (int i = 0; i < list.size(); i++) {
                o = list.get(i);
                boolean found = false;
                final Iterator other = copy.iterator();
                while (!found && other.hasNext()) {
                    try {
                        String newPath = format("%s[%d]", path, i);
                        assertDocumentEquals(newPath, o, other.next());
                        other.remove();
                        found = true;
                    } catch (AssertionError ignore) {
                    }
                }
                if (!found) {
                    fail(format("mismatch found at %s", path));
                }
            }
        } else {
            assertEquals(actual, expected, format("mismatch found at %s:%n%s vs %s", path, expected, actual));
        }
    }

    private void assertSameNullity(String path, Object expected, Object actual) {
        if (expected == null && actual != null
            || actual == null && expected != null) {
            assertEquals(actual, expected, format("mismatch found at %s:%n%s vs %s", path, expected, actual));
        }
    }

    private void assertSameType(String path, Object expected, Object actual) {
        if (expected instanceof List && actual instanceof List) {
            return;
        }
        if (!expected.getClass().equals(actual.getClass())) {
            assertEquals(actual, expected, format("mismatch found at %s:%n%s vs %s", path, expected, actual));
        }
    }

    private void cleanup() {
        MongoDatabase db = getDatabase();
        db.listCollectionNames().forEach(s -> {
            if (!s.equals("zipcodes") && !s.startsWith("system")) {
                db.getCollection(s).drop();
            }
        });
    }

    private Document runIsMaster() {
        return mongoClient.getDatabase("admin")
                          .runCommand(new Document("ismaster", 1));
    }

    private void startMongo() {
        String mongodb = System.getenv("MONGODB");
        Builder builder = MongoClientSettings.builder();

        try {
            builder.uuidRepresentation(mapperOptions.getUuidRepresentation());
        } catch (Exception ignored) {
            // not a 4.0 driver
        }

        if (mongodb != null) {
            Version version = Version.valueOf(mongodb);
            final MongoCluster cluster = version.lessThan(Version.valueOf("4.0.0"))
                                         ? new SingleNode(new File("target/mongo/"), "morphia_test", version)
                                         : new ReplicaSet(new File("target/mongo/"), "morphia_test", version);

            cluster.configure(c -> {
                c.systemLog(s -> {
                    s.setTraceAllExceptions(true);
                    s.setVerbosity(Verbosity.FIVE);
                    return null;
                });
                return null;
            });
            cluster.clean();
            cluster.start();
            mongoClient = cluster.getClient(builder);
        } else {
            mongoClient = MongoClients.create(builder.build());
        }
    }
}
