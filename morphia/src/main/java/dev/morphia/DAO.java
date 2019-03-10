package dev.morphia;


import com.mongodb.MongoClient;
import dev.morphia.dao.BasicDAO;


/**
 * Provides a basic DAO for use in applications
 *
 * @param <T> the entity type
 * @param <K> the key type
 * @deprecated use dev.morphia.dao.BasicDAO
 */
@Deprecated
public class DAO<T, K> extends BasicDAO<T, K> {
    /**
     * @param entityClass the type to use with this DAO
     * @param mongoClient the client to use to talk to the database
     * @param morphia     the morphia instance to use
     * @param dbName      the database to connect to
     */
    public DAO(final Class<T> entityClass, final MongoClient mongoClient, final Morphia morphia, final String dbName) {
        super(entityClass, mongoClient, morphia, dbName);
    }

    /**
     * @param entityClass the type to use with this DAO
     * @param ds          the datastore to use
     */
    public DAO(final Class<T> entityClass, final Datastore ds) {
        super(entityClass, ds);
    }
}
