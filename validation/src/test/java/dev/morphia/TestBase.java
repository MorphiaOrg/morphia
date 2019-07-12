package dev.morphia;


import com.mongodb.DB;
import com.mongodb.MongoClient;
import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.MappingException;
import org.bson.Document;
import org.junit.After;
import org.junit.Before;


public abstract class TestBase {
    private MongoClient mongoClient;
    private DB db;
    private Datastore ds;
    private Mapper mapper;

    protected TestBase() {
        try {
            this.mongoClient = new MongoClient();
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Converts an entity to a Document.  This method is primarily an internal method. Reliance on this method may break your application
     * in
     * future releases.
     *
     * @param entity the entity to convert
     * @return the Document
     */
    public Document toDocument(final Object entity) {
        try {
            return mapper.toDocument(entity);
        } catch (Exception e) {
            throw new MappingException("Could not map entity to Document", e);
        }
    }

    /**
     * Creates an entity and populates its state based on the document given.  This method is primarily an internal method.  Reliance on
     * this method may break your application in future releases.
     *
     * @param <T>         type of the entity
     * @param datastore   the Datastore to use when fetching this reference
     * @param entityClass type to create
     * @param document    the object state to use
     * @return the newly created and populated entity
     */
    public <T> T fromDocument(final Datastore datastore, final Class<T> entityClass, final Document document) {
        if (!entityClass.isInterface() && !datastore.getMapper().isMapped(entityClass)) {
            throw new MappingException("Trying to map to an unmapped class: " + entityClass.getName());
        }
        try {
            return mapper.fromDocument(, document);
        } catch (Exception e) {
            throw new MappingException("Could not map entity from Document", e);
        }
    }

    @Before
    public void setUp() {
        this.mongoClient.dropDatabase("morphia_test");
        this.db = this.mongoClient.getDB("morphia_test");
        this.ds = Morphia.createDatastore(this.mongoClient, this.db.getName());
        mapper = ds.getMapper();
    }

    @After
    public void tearDown() {
        // new ScopedFirstLevelCacheProvider().release();
    }

    public DB getDb() {
        return db;
    }

    public Datastore getDs() {
        return ds;
    }
}
