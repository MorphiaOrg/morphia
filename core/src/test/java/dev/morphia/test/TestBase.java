package dev.morphia.test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.github.zafarkhaja.semver.Version;
import com.mongodb.ConnectionString;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.result.InsertManyResult;
import com.mongodb.lang.NonNull;

import dev.morphia.Datastore;
import dev.morphia.DatastoreImpl;
import dev.morphia.Morphia;
import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.MapperOptions;
import dev.morphia.mapping.codec.reader.DocumentReader;
import dev.morphia.mapping.codec.writer.DocumentWriter;
import dev.morphia.query.DefaultQueryFactory;
import dev.morphia.query.LegacyQueryFactory;
import dev.morphia.test.mapping.codec.ZonedDateTimeCodec;

import org.bson.Document;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecProvider;
import org.bson.codecs.configuration.CodecRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.utility.DockerImageName;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.DataProvider;

import static com.mongodb.MongoClientSettings.builder;
import static dev.morphia.internal.MorphiaInternals.proxyClassesPresent;
import static java.lang.String.format;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;

public abstract class TestBase {
    protected static final String TEST_DB_NAME = "morphia_test";
    private static final Logger LOG = LoggerFactory.getLogger(TestBase.class);
    private static MongoClient mongoClient;
    private static MongoDBContainer mongoDBContainer;
    private static String connectionString;

    private MapperOptions mapperOptions;
    private MongoDatabase database;
    private DatastoreImpl datastore;

    public TestBase() {
        mapperOptions = MapperOptions.builder()
                .codecProvider(new ZDTCodecProvider())
                .build();
    }

    public TestBase(MapperOptions mapperOptions) {
        this.mapperOptions = mapperOptions;
    }

    @BeforeSuite
    public void startContainer() {
        if (mongoDBContainer == null) {
            String mongodb = System.getProperty("mongodb");
            if (mongodb == null) {
                LOG.info("No mongodb property specified. Using already running server.");
                connectionString = "mongodb://localhost:27017/" + TEST_DB_NAME;
            } else {
                String imageName;
                try {
                    Versions match = Versions.bestMatch(Version.valueOf(mongodb));
                    imageName = match.dockerImage();
                } catch (IllegalArgumentException e) {
                    imageName = "mongo:" + mongodb;
                }

                LOG.info("mongodb property specified.  Running tests using " + imageName);
                mongoDBContainer = new MongoDBContainer(DockerImageName.parse(imageName)
                        .asCompatibleSubstituteFor("mongo"));
                mongoDBContainer.start();
                connectionString = mongoDBContainer.getReplicaSetUrl(TEST_DB_NAME);
            }
        }
    }

    @AfterSuite
    public void stopContainer() {
        if (mongoDBContainer != null) {
            mongoDBContainer.stop();
        }
    }

    @BeforeMethod
    public void beforeEach() {
        cleanup();
    }

    protected void cleanup() {
        MongoDatabase db = getDatabase();
        db.runCommand(new Document("profile", 0).append("slowms", 0));
        db.listCollectionNames().forEach(s -> {
            if (!s.equals("zipcodes") && !s.startsWith("system")) {
                db.getCollection(s).drop();
            }
        });
        database = null;
        datastore = null;
    }

    public MongoDatabase getDatabase() {
        if (database == null) {
            database = getDs().getDatabase();
        }
        return database;
    }

    public DatastoreImpl getDs() {
        if (datastore == null) {
            datastore = (DatastoreImpl) Morphia.createDatastore(getMongoClient(), TEST_DB_NAME, mapperOptions);
        }
        return datastore;
    }

    protected MongoClient getMongoClient() {
        if (mongoClient == null) {
            mongoClient = MongoClients.create(builder()
                    .uuidRepresentation(mapperOptions.getUuidRepresentation())
                    .applyConnectionString(new ConnectionString(connectionString))
                    .build());

        }
        return mongoClient;
    }

    public Mapper getMapper() {
        return getDs().getMapper();
    }

    public void installData() {
        File file = new File("zips.json");
        try {
            if (!file.exists()) {
                file = new File("target/zips.json");
                if (!file.exists()) {
                    download(new URL("https://media.mongodb.org/zips.json"), file);
                }
            }
            MongoCollection<Document> zips = getDatabase().getCollection("zipcodes");
            if (zips.countDocuments() == 0) {
                LOG.info("Count is 0.  (Re)installing sample data");
                MongoCollection<Document> zipcodes = getDatabase().getCollection("zipcodes");
                zipcodes.drop();
                Files.lines(file.toPath())
                        .forEach(l -> zipcodes.insertOne(Document.parse(l)));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        assumeTrue(file.exists(), "Failed to process media files");
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

    protected void assumeTrue(boolean condition, String message) {
        if (!condition) {
            throw new SkipException(message);
        }
    }

    @DataProvider(name = "mapperOptions")
    public Object[][] mapperOptions() {
        return new Object[][] {
                new Object[] { MapperOptions.DEFAULT },
                new Object[] { MapperOptions.legacy().build() }
        };
    }

    @DataProvider(name = "queryFactories")
    public Object[][] queryFactories() {
        return new Object[][] {
                new Object[] { new DefaultQueryFactory() },
                new Object[] { new LegacyQueryFactory() }
        };
    }

    protected void assertCapped(Class<?> type, Integer max, Integer size) {
        Document result = getOptions(type);
        Assert.assertTrue(result.getBoolean("capped"));
        assertEquals(result.get("max"), max);
        assertEquals(result.get("size"), size);
    }

    protected void assertDocumentEquals(Object actual, Object expected) {
        assertDocumentEquals("", actual, expected);
    }

    protected void assertLazy(Supplier<String> messageSupplier, Runnable assertion) {
        try {
            assertion.run();
        } catch (AssertionError error) {
            fail(messageSupplier.get(), error);
        }
    }

    protected void assertListEquals(Collection<?> actual, Collection<?> expected) {
        assertEquals(actual.size(), expected.size());
        expected.forEach(
                d -> assertTrueLazy(actual.contains(d), () -> format("Should have found <<%s>> in the actual list:%n%s", d, actual)));
    }

    public void assertTrueLazy(boolean condition, Supplier<String> messageSupplier) {
        if (!condition) {
            fail(messageSupplier.get());
        }
    }

    protected void checkForProxyTypes() {
        assumeTrue(proxyClassesPresent(), "Proxy classes are needed for this test");
    }

    protected void checkForReplicaSet() {
        assumeTrue(isReplicaSet(), "This test requires a replica set");
    }

    private boolean isReplicaSet() {
        return runIsMaster().get("setName") != null;
    }

    private Document runIsMaster() {
        return mongoClient.getDatabase("admin")
                .runCommand(new Document("ismaster", 1));
    }

    protected void checkMinDriverVersion(double version) {
        checkMinDriverVersion(Version.valueOf(version + ".0"));
    }

    protected void checkMinDriverVersion(Version version) {
        assumeTrue(driverIsAtLeastVersion(version),
                String.format("Server should be at least %s but found %s", version, getServerVersion()));
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

    protected Version getServerVersion() {
        String version = (String) getMongoClient()
                .getDatabase("admin")
                .runCommand(new Document("serverStatus", 1))
                .get("version");
        return Version.valueOf(version);
    }

    protected void checkMinServerVersion(double version) {
        checkMinServerVersion(Version.valueOf(version + ".0"));
    }

    protected void checkMinServerVersion(Version version) {
        assumeTrue(serverIsAtLeastVersion(version),
                String.format("Server should be at least %s but found %s", version, getServerVersion()));
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

    protected <T> T fromDocument(Class<T> type, Document document) {
        Class<T> aClass = type;
        Mapper mapper = getMapper();
        if (document.containsKey(mapper.getOptions().getDiscriminatorKey())) {
            aClass = mapper.getClass(document);
        }

        DocumentReader reader = new DocumentReader(document);

        return getDs().getCodecRegistry()
                .get(aClass)
                .decode(reader, DecoderContext.builder().build());
    }

    protected MongoCollection<Document> getDocumentCollection(Class<?> type) {
        return getDatabase().getCollection(getMapper().getEntityModel(type).getCollectionName());
    }

    protected List<Document> getIndexInfo(Class<?> clazz) {
        return getDs().getCollection(clazz).listIndexes().into(new ArrayList<>());
    }

    @NonNull
    protected Document getOptions(Class<?> type) {
        String collection = getMapper().getEntityModel(type).getCollectionName();
        Document result = getDatabase().runCommand(new Document("listCollections", 1.0)
                .append("filter",
                        new Document("name", collection)));

        Document cursor = (Document) result.get("cursor");
        return (Document) cursor.getList("firstBatch", Document.class)
                .get(0)
                .get("options");
    }

    protected void insert(String collectionName, List<Document> list) {
        MongoCollection<Document> collection = getDatabase().getCollection(collectionName);
        collection.deleteMany(new Document());
        if (!list.isEmpty()) {
            InsertManyResult insertManyResult = collection.insertMany(list);
            assertEquals(insertManyResult.getInsertedIds().size(), list.size());
        }
    }

    protected List<Document> removeIds(List<Document> documents) {
        return documents.stream()
                .peek(d -> d.remove("_id"))
                .collect(Collectors.toList());
    }

    /**
     * @param version the minimum version allowed
     * @return true if server is at least specified version
     */
    protected boolean serverIsAtLeastVersion(Version version) {
        return getServerVersion().greaterThanOrEqualTo(version);
    }

    protected Document toDocument(Object entity) {
        final Class<?> type = getMapper().getEntityModel(entity.getClass()).getType();

        DocumentWriter writer = new DocumentWriter(getMapper());
        ((Codec) getDs().getCodecRegistry().get(type)).encode(writer, entity, EncoderContext.builder().build());

        return writer.getDocument();
    }

    protected String toString(Document document) {
        return document.toJson(getDs().getCodecRegistry().get(Document.class));
    }

    protected void withClient(MongoClient client, Consumer<Datastore> block) {
        MapperOptions previousOptions = mapperOptions;
        try (client) {
            block.accept(Morphia.createDatastore(client, TEST_DB_NAME));
        }
    }

    protected void withOptions(MapperOptions options, Runnable block) {
        MapperOptions previousOptions = mapperOptions;
        try {
            mapperOptions = options;
            database = null;
            datastore = null;
            if (mongoClient != null) {
                mongoClient.close();
            }
            mongoClient = null;

            block.run();
        } finally {
            mapperOptions = previousOptions;
            database = null;
            datastore = null;
            if (mongoClient != null) {
                mongoClient.close();
            }
            mongoClient = null;
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private void assertDocumentEquals(String path, Object actual, Object expected) {
        assertSameNullity(path, expected, actual);
        if (expected == null) {
            return;
        }
        assertSameType(path, actual, expected);

        if (expected instanceof Document) {
            for (Entry<String, Object> entry : ((Document) expected).entrySet()) {
                final String key = entry.getKey();
                Object expectedValue = entry.getValue();
                Object actualValue = ((Document) actual).get(key);
                assertDocumentEquals(path + "." + key, actualValue, expectedValue);
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
                        assertDocumentEquals(newPath, other.next(), o);
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

    private void assertSameType(String path, Object actual, Object expected) {
        if (expected instanceof List && actual instanceof List) {
            return;
        }
        if (!expected.getClass().equals(actual.getClass())) {
            assertEquals(actual, expected, format("mismatch found at %s:%n%s vs %s", path, expected, actual));
        }
    }

    private static class ZDTCodecProvider implements CodecProvider {
        @Override
        public <T> Codec<T> get(Class<T> clazz, CodecRegistry registry) {
            if (clazz.equals(ZonedDateTime.class)) {
                return (Codec<T>) new ZonedDateTimeCodec();
            }
            return null;
        }
    }
}
