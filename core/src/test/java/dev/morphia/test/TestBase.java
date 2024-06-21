package dev.morphia.test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.result.InsertManyResult;
import com.mongodb.lang.NonNull;

import dev.morphia.MorphiaDatastore;
import dev.morphia.config.MorphiaConfig;
import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.codec.reader.DocumentReader;
import dev.morphia.mapping.codec.writer.DocumentWriter;
import dev.morphia.test.mapping.codec.ZonedDateTimeCodec;

import org.bson.Document;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecProvider;
import org.bson.codecs.configuration.CodecRegistry;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;

import static dev.morphia.internal.MorphiaInternals.proxyClassesPresent;
import static java.lang.String.format;
import static java.nio.file.Files.lines;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;

public abstract class TestBase extends MorphiaTestSetup {
    private static final Logger LOG = LoggerFactory.getLogger(TestBase.class);
    protected static final String TEST_DB_NAME = "morphia_test";

    public static File GIT_ROOT = new File(".").getAbsoluteFile();
    protected static File CORE_ROOT;

    static {
        while (!new File(GIT_ROOT, ".git").exists()) {
            GIT_ROOT = GIT_ROOT.getParentFile();
        }
        try {
            CORE_ROOT = new File(GIT_ROOT, "core").getCanonicalFile();
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    protected DriverVersion minDriver = DriverVersion.v41;

    public TestBase() {
    }

    public TestBase(MorphiaConfig config) {
        super(config);
    }

    @BeforeMethod
    public void beforeEach() {
        cleanup();
    }

    protected void cleanup() {
        MongoDatabase db = getMongoClient().getDatabase(getMorphiaContainer().getMorphiaConfig().database());
        db.runCommand(new Document("profile", 0).append("slowms", 0));
        db.drop();
        getMorphiaContainer().reset();
    }

    public MorphiaDatastore getDs() {
        return getMorphiaContainer().getDs();
    }

    public MongoDatabase getDatabase() {
        return getMorphiaContainer().getDatabase();
    }

    public Mapper getMapper() {
        return getDs().getMapper();
    }

    protected void download(URL url, File file) throws IOException {
        LOG.info("Downloading zip data set to " + file);
        try (var inputStream = url.openStream(); var outputStream = new FileOutputStream(file)) {
            byte[] read = new byte[49152];
            int count;
            while ((count = inputStream.read(read)) != -1) {
                outputStream.write(read, 0, count);
            }
        }
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
                zipcodes.insertMany(
                        lines(file.toPath())
                                .map(Document::parse)
                                .collect(toList()));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        assumeTrue(file.exists(), "Failed to process media files");
    }

    public static <T> T walk(Map map, List<String> steps) {
        Object value = map;
        for (String step : steps) {
            if (value instanceof Map) {
                value = ((Map<?, ?>) value).get(step);
            }
        }
        return (T) value;
    }

    protected void assertCapped(Class<?> type, Integer max) {
        Document result = getOptions(type);
        Assert.assertTrue(result.getBoolean("capped"));
        assertEquals(result.get("max"), max);
        assertEquals(result.get("size"), 1048576);
    }

    protected void assertDocumentEquals(Object actual, Object expected) {
        assertDocumentEquals("", actual, expected);
    }

    protected void assertDocumentEquals(Object actual, Object expected, String message) {
        try {
            assertDocumentEquals("", actual, expected);
        } catch (AssertionError error) {
            fail(message);
        }
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
                d -> assertTrueLazy(actual.contains(coerceToLong(d)), () -> {
                    String actualString = actual.stream()
                            .map(c -> c.toString())
                            .collect(joining("\n\t", "actual list:\n\t", ""));
                    String expectedString = expected.stream()
                            .map(c -> c.toString())
                            .collect(joining("\n\t", "expected list:\n\t", ""));
                    return format("Lists do not match:\n%s \n%s", actualString, expectedString);
                }));
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
        if (document.containsKey(mapper.getConfig().discriminatorKey())) {
            aClass = mapper.getClass(document);
        }

        DocumentReader reader = new DocumentReader(document);

        return getDs().getCodecRegistry()
                .get(aClass)
                .decode(reader, DecoderContext.builder().build());
    }

    protected MongoCollection<Document> getDocumentCollection(Class<?> type) {
        return getDatabase().getCollection(getMapper().getEntityModel(type).collectionName());
    }

    protected List<Document> getIndexInfo(Class<?> clazz) {
        return getDs().getCollection(clazz).listIndexes().into(new ArrayList<>());
    }

    @NonNull
    protected Document getOptions(Class<?> type) {
        String collection = getMapper().getEntityModel(type).collectionName();
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
                .peek(d -> removeId(d))
                .collect(toList());
    }

    private static Object removeId(Document d) {
        Object doc = d.remove("_id");
        d.values().forEach(v -> {
            if (v instanceof Document document) {
                removeId(document);
            } else if (v instanceof List<?> list) {
                list.forEach(e -> {
                    if (e instanceof Document document) {
                        removeId(document);
                    }
                });
            }
        });
        return doc;
    }

    protected Document toDocument(Object entity) {
        final Class<?> type = getMapper().getEntityModel(entity.getClass()).getType();

        DocumentWriter writer = new DocumentWriter(getMapper().getConfig());
        ((Codec) getDs().getCodecRegistry().get(type)).encode(writer, entity, EncoderContext.builder().build());

        return writer.getDocument();
    }

    protected String toString(Document document) {
        return document.toJson(getDs().getCodecRegistry().get(Document.class));
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private void assertDocumentEquals(String path, Object actual, Object expected) {
        assertSameNullity(path, expected, actual);
        if (expected == null) {
            return;
        }

        if (expected instanceof Document document) {
            document.entrySet()
                    .forEach(entry -> {
                        final String key = entry.getKey();
                        Object actualValue = ((Document) actual).get(key);
                        Object expectedValue = entry.getValue();
                        assertDocumentEquals(append(path, key), actualValue, expectedValue);
                    });
        } else if (expected instanceof List list) {
            List copy = new ArrayList<>((List) actual);

            Object o;
            for (int i = 0; i < list.size(); i++) {
                o = list.get(i);
                boolean found = false;
                final Iterator other = copy.iterator();
                String newPath = null;
                while (!found && other.hasNext()) {
                    try {
                        newPath = format("%s[%d]", path, i);
                        assertDocumentEquals(newPath, other.next(), o);
                        other.remove();
                        found = true;
                    } catch (AssertionError ignore) {
                    }
                }
                if (!found) {
                    fail("mismatch found at %s.\n\tactual = %s,\n\texpected = %s".formatted(newPath, actual, expected));
                }
            }

        } else {
            assertEquals(coerceToLong(actual), coerceToLong(expected), format("mismatch found at %s:%n%s vs %s", path, expected, actual));
        }
    }

    @NotNull
    private static String append(String path, String key) {
        return path.isEmpty() ? key : path + "." + key;
    }

    public static Object coerceToLong(Object object) {
        return object instanceof Integer ? ((Integer) object).longValue() : object;
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

    @BeforeMethod
    private void setDriverMinimum() {
        minDriver = DriverVersion.v43;
    }

    public static class ZDTCodecProvider implements CodecProvider {
        @Override
        public <T> Codec<T> get(Class<T> clazz, CodecRegistry registry) {
            if (clazz.equals(ZonedDateTime.class)) {
                return (Codec<T>) new ZonedDateTimeCodec();
            }
            return null;
        }
    }
}
