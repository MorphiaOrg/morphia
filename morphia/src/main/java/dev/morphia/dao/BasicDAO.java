package dev.morphia.dao;

import com.mongodb.DBCollection;
import com.mongodb.MongoClient;
import com.mongodb.WriteConcern;
import com.mongodb.WriteResult;
import dev.morphia.Datastore;
import dev.morphia.InsertOptions;
import dev.morphia.Key;
import dev.morphia.Morphia;
import dev.morphia.UpdateOptions;
import dev.morphia.query.FindOptions;
import dev.morphia.query.Query;
import dev.morphia.query.UpdateOperations;
import dev.morphia.query.UpdateResults;

import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @param <T> the type of the entity
 * @param <K> the type of the key
 * @author Olafur Gauti Gudmundsson
 * @author Scott Hernandez
 * @deprecated This interface poorly tracks Datastore's API.  Use Datastore directly or wrap in an application specific DAO
 */
@Deprecated
@SuppressWarnings({"WeakerAccess", "deprecation", "unused"})
public class BasicDAO<T, K> implements DAO<T, K> {
    //CHECKSTYLE:OFF
    /**
     * @deprecated use {@link #getEntityClass()}
     */
    @Deprecated
    protected Class<T> entityClazz;
    /**
     * @deprecated use {@link #getDatastore()}
     */
    @Deprecated
    protected dev.morphia.DatastoreImpl ds;
    //CHECKSTYLE:ON

    /**
     * Create a new BasicDAO
     *
     * @param entityClass the class of the POJO you want to persist using this DAO
     * @param mongoClient the representations of the connection to a MongoDB instance
     * @param morphia     a Morphia instance
     * @param dbName      the name of the database
     */
    public BasicDAO(final Class<T> entityClass, final MongoClient mongoClient, final Morphia morphia, final String dbName) {
        initDS(mongoClient, morphia, dbName);
        initType(entityClass);
    }

    /**
     * Create a new BasicDAO
     *
     * @param entityClass the class of the POJO you want to persist using this DAO
     * @param ds          the Datastore which gives access to the MongoDB instance for this DAO
     */
    public BasicDAO(final Class<T> entityClass, final Datastore ds) {
        this.ds = (dev.morphia.DatastoreImpl) ds;
        initType(entityClass);
    }

    /**
     * Only calls this from your derived class when you explicitly declare the generic types with concrete classes
     * <p/>
     * {@code class MyDao extends DAO<MyEntity, String>}
     *
     * @param mongoClient the representations of the connection to a MongoDB instance
     * @param morphia     a Morphia instance
     * @param dbName      the name of the database
     */
    @SuppressWarnings("unchecked")
    protected BasicDAO(final MongoClient mongoClient, final Morphia morphia, final String dbName) {
        initDS(mongoClient, morphia, dbName);
        initType(((Class<T>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0]));
    }

    @SuppressWarnings("unchecked")
    protected BasicDAO(final Datastore ds) {
        this.ds = (dev.morphia.DatastoreImpl) ds;
        initType(((Class<T>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0]));
    }

    @Override
    public long count() {
        return ds.getCount(entityClazz);
    }

    @Override
    public long count(final String key, final Object value) {
        return count(ds.find(entityClazz).filter(key, value));
    }

    @Override
    public long count(final Query<T> query) {
        return ds.getCount(query);
    }

    @Override
    public Query<T> createQuery() {
        return ds.find(entityClazz);
    }

    @Override
    public UpdateOperations<T> createUpdateOperations() {
        return ds.createUpdateOperations(entityClazz);
    }

    @Override
    public WriteResult delete(final T entity) {
        return ds.delete(entity);
    }

    @Override
    public WriteResult delete(final T entity, final WriteConcern wc) {
        return ds.delete(entity, wc);
    }

    @Override
    public WriteResult deleteById(final K id) {
        return ds.delete(entityClazz, id);
    }

    @Override
    public WriteResult deleteByQuery(final Query<T> query) {
        return ds.delete(query);
    }

    @Override
    public void ensureIndexes() {
        ds.ensureIndexes(entityClazz);
    }

    @Override
    public boolean exists(final String key, final Object value) {
        return exists(ds.find(entityClazz).filter(key, value));
    }

    @Override
    public boolean exists(final Query<T> query) {
        return query.get(new FindOptions().limit(1)) != null;
    }

    @Override
    public Query<T> find() {
        return createQuery();
    }

    @Override
    public Query<T> find(final Query<T> query) {
        return query;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<K> findIds() {
        return (List<K>) keysToIds(ds.find(entityClazz).asKeyList());
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<K> findIds(final String key, final Object value) {
        return (List<K>) keysToIds(ds.find(entityClazz).filter(key, value).asKeyList());
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<K> findIds(final Query<T> query) {
        return (List<K>) keysToIds(query.asKeyList());
    }

    @Override
    public T findOne(final String key, final Object value) {
        return ds.find(entityClazz).filter(key, value).get();
    }

    /* (non-Javadoc)
     * @see dev.morphia.DAO#findOne(dev.morphia.query.Query)
     */
    @Override
    public T findOne(final Query<T> query) {
        return query.get();
    }

    @Override
    public Key<T> findOneId() {
        return findOneId(ds.find(entityClazz));
    }

    @Override
    public Key<T> findOneId(final String key, final Object value) {
        return findOneId(ds.find(entityClazz).filter(key, value));
    }

    @Override
    public Key<T> findOneId(final Query<T> query) {
        Iterator<Key<T>> keys = query.fetchKeys().iterator();
        return keys.hasNext() ? keys.next() : null;
    }

    @Override
    public T get(final K id) {
        return ds.get(entityClazz, id);
    }

    @Override
    public DBCollection getCollection() {
        return ds.getCollection(entityClazz);
    }

    /* (non-Javadoc)
     * @see dev.morphia.DAO#getDatastore()
     */
    @Override
    public Datastore getDatastore() {
        return ds;
    }

    @Override
    public Class<T> getEntityClass() {
        return entityClazz;
    }

    @Override
    public Key<T> save(final T entity) {
        return ds.save(entity);
    }

    @Override
    public Key<T> save(final T entity, final WriteConcern wc) {
        return ds.save(entity, new InsertOptions().writeConcern(wc));
    }

    @Override
    public UpdateResults update(final Query<T> query, final UpdateOperations<T> ops) {
        return ds.update(query, ops);
    }

    @Override
    public UpdateResults updateFirst(final Query<T> query, final UpdateOperations<T> ops) {
        return ds.update(query, ops, new UpdateOptions());
    }

    /**
     * @return the Datastore used by this DAO
     * @deprecated use {@link #getDatastore()}
     */
    @Deprecated
    public dev.morphia.DatastoreImpl getDs() {
        return ds;
    }

    /**
     * @return the entity class
     * @deprecated use {@link #getEntityClass()} instead
     */
    @Deprecated
    public Class<T> getEntityClazz() {
        return entityClazz;
    }

    protected void initDS(final MongoClient mongoClient, final Morphia mor, final String db) {
        ds = (dev.morphia.DatastoreImpl) mor.createDatastore(mongoClient, db);
    }

    protected void initType(final Class<T> type) {
        entityClazz = type;
        ds.getMapper().addMappedClass(type);
    }

    /**
     * Converts from a List<Key> to their id values
     */
    protected List<?> keysToIds(final List<Key<T>> keys) {
        final List<Object> ids = new ArrayList<Object>(keys.size() * 2);
        for (final Key<T> key : keys) {
            ids.add(key.getId());
        }
        return ids;
    }

}
