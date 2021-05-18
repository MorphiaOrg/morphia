package dev.morphia;

import com.antwerkz.bottlerocket.clusters.MongoCluster;
import com.antwerkz.bottlerocket.clusters.ReplicaSet;
import com.antwerkz.bottlerocket.clusters.SingleNode;
import com.github.zafarkhaja.semver.Version;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoClientSettings.Builder;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.MapperOptions;
import dev.morphia.mapping.codec.pojo.EntityModel;
import org.apache.commons.io.FileUtils;
import org.bson.Document;
import org.junit.After;
import org.junit.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assume.assumeTrue;

@SuppressWarnings("WeakerAccess")
public abstract class TestBase {
    protected static final String TEST_DB_NAME = "morphia_test";
    private static final Logger LOG = LoggerFactory.getLogger(TestBase.class);
    private static MapperOptions mapperOptions = MapperOptions.DEFAULT;
    private static MongoClient mongoClient;
    private MongoDatabase database;
    private Datastore datastore;

    protected TestBase() {
        mapperOptions = MapperOptions.DEFAULT;
    }

    protected TestBase(MapperOptions option) {
        mapperOptions = option;
    }

    public MongoDatabase getDatabase() {
        if (database == null) {
            database = getDs().getDatabase();
        }
        return database;
    }

    public Datastore getDs() {
        if (datastore == null) {
            datastore = Morphia.createDatastore(getMongoClient(), TEST_DB_NAME, mapperOptions);
        }
        return datastore;
    }

    public Mapper getMapper() {
        return getDs().getMapper();
    }

    public MongoClient getMongoClient() {
        if (mongoClient == null) {
            startMongo();
        }
        return mongoClient;
    }

    @Before
    public void setUp() {
        cleanup();
        installSampleData();
    }

    @After
    public void tearDown() {
        cleanup();
    }

    protected void cleanup() {
        MongoDatabase db = getDatabase();
        db.listCollectionNames().forEach(s -> {
            if (!s.equals("zipcodes") && !s.startsWith("system.")) {
                db.getCollection(s).drop();
            }
        });
    }

    protected MongoCollection<Document> getDocumentCollection(Class<?> type) {
        return getDatabase().getCollection(getEntityModel(type).getCollectionName());
    }

    protected EntityModel getEntityModel(Class<?> aClass) {
        Mapper mapper = getMapper();
        mapper.map(aClass);

        return mapper.getEntityModel(aClass);
    }

    protected List<Document> getIndexInfo(Class<?> clazz) {
        return getMapper().getCollection(clazz).listIndexes().into(new ArrayList<>());
    }

    protected void insert(String collectionName, List<Document> list) {
        MongoCollection<Document> collection = getDatabase().getCollection(collectionName);
        collection.deleteMany(new Document());
        collection.insertMany(list);
    }

    protected String toString(Document document) {
        return document.toJson(getMapper().getCodecRegistry().get(Document.class));
    }

    private void download(URL url, File file) throws IOException {
        LOG.info("Downloading zip data set to " + file);
        try (InputStream inputStream = url.openStream(); FileOutputStream outputStream = new FileOutputStream(file)) {
            byte[] read = new byte[49152];
            int count;
            while ((count = inputStream.read(read)) != -1) {
                outputStream.write(read, 0, count);
            }
        }
    }

    private void installSampleData() {
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
                LOG.info("Installing sample data");
                MongoCollection<Document> zipcodes = getDatabase().getCollection("zipcodes");
                Files.lines(file.toPath())
                     .forEach(l -> zipcodes.insertOne(Document.parse(l)));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        assumeTrue("Failed to process media files", file.exists());
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
            File mongodbRoot = new File("target/mongo");
            try {
                FileUtils.deleteDirectory(mongodbRoot);
            } catch (IOException e) {
                throw new RuntimeException(e.getMessage(), e);
            }
            Version version = Version.valueOf(mongodb);
            final MongoCluster cluster = version.lessThan(Version.valueOf("4.0.0"))
                                         ? new SingleNode(mongodbRoot, "morphia_test", version)
                                         : new ReplicaSet(mongodbRoot, "morphia_test", version);

            cluster.start();
            mongoClient = cluster.getClient(builder);
        } else {
            mongoClient = MongoClients.create(builder.build());
        }
    }
}
