package dev.morphia.test;

import com.mongodb.client.MongoClient;
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
import dev.morphia.test.config.ManualMorphiaTestConfig;
import dev.morphia.test.config.MorphiaTestConfig;
import dev.morphia.test.mapping.codec.ZonedDateTimeCodec;
import org.bson.Document;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecProvider;
import org.bson.codecs.configuration.CodecRegistry;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.function.Supplier;

import static dev.morphia.internal.MorphiaInternals.proxyClassesPresent;
import static java.lang.String.format;
import static java.nio.file.Files.lines;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unified JUnit 5 base class for Morphia tests
 * Combines functionality from TestBase and MorphiaTestSetup using JUnit 5 Extension
 */
@ExtendWith(MorphiaJUnitExtension.class)
public abstract class JUnitMorphiaTestBase {
    private static final Logger LOG = LoggerFactory.getLogger(JUnitMorphiaTestBase.class);
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

    // Access methods for extension-managed resources
    protected MongoClient getMongoClient() {
        return getMongoHolder().getMongoClient();
    }

    protected MongoHolder getMongoHolder() {
        return MorphiaJUnitExtension.getCurrentMongoHolder();
    }

    protected MorphiaContainer getMorphiaContainer() {
        // Check if we're in a withConfig scope
        ConfigScope currentScope = ConfigScope.getCurrentScope();
        if (currentScope != null) {
            return currentScope.getContainer();
        }
        return MorphiaJUnitExtension.getCurrentMorphiaContainer();
    }

    protected MorphiaConfig getMorphiaConfig() {
        // Check if we're in a withConfig scope
        ConfigScope currentScope = ConfigScope.getCurrentScope();
        if (currentScope != null) {
            return currentScope.getConfig();
        }
        return MorphiaJUnitExtension.getCurrentMorphiaConfig();
    }

    /**
     * Executes the given body with a temporary MorphiaContainer using custom config.
     * This allows tests to use different configurations within individual test methods.
     * The temporary container is automatically cleaned up after the body execution completes.
     *
     * @param config the custom MorphiaConfig to use
     * @param body   the code to execute with the custom configuration
     */
    protected void withTestConfig(MorphiaConfig config, List<Class<?>> types, Runnable body) {
        withConfig(new ManualMorphiaTestConfig(config).classes(types), body);
    }

    protected void withConfig(MorphiaConfig config, Runnable body) {
        // Execute the body with the custom config
        if (config instanceof MorphiaTestConfig testConfig) {
            List<Class<?>> classes = testConfig.classes();
            if (classes != null) {
                getMorphiaContainer().getDs().getMapper().map(classes);
            }
            if (config.applyIndexes()) {
                getMorphiaContainer().getDs().applyIndexes();
            }
        }
        // Create temporary container with the custom config
        MorphiaContainer tempContainer = new MorphiaContainer(getMongoClient(), config);
        ConfigScope scope = new ConfigScope(tempContainer);
        // Store the current scope in ThreadLocal for access during body execution
        ConfigScope.setCurrentScope(scope);
        try {
            body.run();
        } finally {
            // Clean up the temporary config scope
            ConfigScope.clearCurrentScope();
            // Optionally clean up the container if needed
            // tempContainer could have cleanup logic here if required
        }
    }

    // Core test utility methods
    public MorphiaDatastore getDs() {
        // Check if we're in a withConfig scope
        ConfigScope currentScope = ConfigScope.getCurrentScope();
        if (currentScope != null) {
            return currentScope.getDs();
        }
        return getMorphiaContainer().getDs();
    }

    public MongoDatabase getDatabase() {
        // Check if we're in a withConfig scope
        ConfigScope currentScope = ConfigScope.getCurrentScope();
        if (currentScope != null) {
            return currentScope.getDatabase();
        }
        return getMorphiaContainer().getDatabase();
    }

    public MongoDatabase getDatabase(String databaseName) {
        return getMongoHolder().getMongoClient().getDatabase(databaseName);
    }

    public Mapper getMapper() {
        return getDs().getMapper();
    }

    // Data setup utilities
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

    // Assertion utilities
    protected void assertCapped(Class<?> type, Integer max) {
        Document result = getOptions(type);
        assertTrue(result.getBoolean("capped"));
        assertEquals(max, result.get("max"));
        assertEquals(1048576, result.get("size"));
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
        assertEquals(expected.size(), actual.size());
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

    // Test condition checks
    protected void checkForProxyTypes() {
        assumeTrue(proxyClassesPresent(), "Proxy classes are needed for this test");
    }

    protected void checkForReplicaSet() {
        assumeTrue(isReplicaSet(), "This test requires a replica set");
    }

    protected void assumeTrue(boolean condition, String message) {
        Assumptions.assumeTrue(condition, message);
    }

    // Version checking methods
    protected void checkMinDriverVersion(String version) {
        MorphiaJUnitExtension.checkMinDriverVersion(version);
    }

    protected void checkMinServerVersion(String version) {
        ExtensionContext context = MorphiaJUnitExtension.getCurrentContext();
        MorphiaJUnitExtension.checkMinServerVersion(context, version);
    }

    protected void checkMaxServerVersion(String version) {
        ExtensionContext context = MorphiaJUnitExtension.getCurrentContext();
        MorphiaJUnitExtension.checkMaxServerVersion(context, version);
    }

    // Utility methods
    public static <T> T walk(Map map, List<String> steps) {
        Object value = map;
        for (String step : steps) {
            if (value instanceof Map) {
                value = ((Map<?, ?>) value).get(step);
            }
        }
        return (T) value;
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
            assertEquals(list.size(), insertManyResult.getInsertedIds().size());
        }
    }

    protected boolean isReplicaSet() {
        return getMorphiaContainer().runIsMaster().get("setName") != null;
    }

    protected static MorphiaConfig buildConfig(Class<?>... types) {
        return MorphiaJUnitExtension.buildConfig(types);
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

    protected void toFile(String name, List<Document> pipeline) {
        try (var writer = new PrintWriter("target/%s.json".formatted(name))) {
            writer.println(toJson(pipeline));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected String toJson(Document document) {
        return document.toJson(TemplatedTestBase.JSON_WRITER_SETTINGS, getDatabase().getCodecRegistry().get(Document.class));
    }

    protected String toJson(List<Document> pipeline) {
        return pipeline.stream()
                .map(d -> d.toJson(TemplatedTestBase.JSON_WRITER_SETTINGS, getDatabase().getCodecRegistry().get(Document.class)))
                .collect(joining("\n, ", "[\n", "\n]"));
    }

    protected String toString(Document document) {
        return document.toJson(getDs().getCodecRegistry().get(Document.class));
    }

    protected Object coerceToLong(Object value) {
        if (value instanceof Integer) {
            return ((Integer) value).longValue();
        }
        return value;
    }

    // Document comparison (simplified implementation)
    @SuppressWarnings({"rawtypes", "unchecked"})
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
            assertEquals(coerceToLong(expected), coerceToLong(actual), format("mismatch found at %s:%n%s vs %s", path, expected, actual));
        }
    }

    @NotNull
    private static String append(String path, String key) {
        return path.isEmpty() ? key : path + "." + key;
    }

    private void assertSameNullity(String path, Object expected, Object actual) {
        if (expected == null && actual != null
                || actual == null && expected != null) {
            assertEquals(expected, actual, format("mismatch found at %s:%n%s vs %s", path, expected, actual));
        }
    }

    // ZDT Codec Provider (same as original TestBase)
    public static class ZDTCodecProvider implements CodecProvider {
        @Override
        @SuppressWarnings("unchecked")
        public <T> Codec<T> get(Class<T> clazz, CodecRegistry registry) {
            if (clazz.equals(ZonedDateTime.class)) {
                return (Codec<T>) new ZonedDateTimeCodec();
            }
            return null;
        }
    }
}