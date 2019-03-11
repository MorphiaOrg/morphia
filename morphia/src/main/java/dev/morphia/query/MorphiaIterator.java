package dev.morphia.query;


import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.client.MongoCursor;
import dev.morphia.Datastore;
import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.cache.EntityCache;

import java.util.Iterator;
import java.util.NoSuchElementException;


/**
 * @param <T> the original type being iterated
 * @param <V> the type of the values returned
 * @author Scott Hernandez
 * @see MorphiaKeyIterator
 * @deprecated this is being replaced by {@link MongoCursor}
 */
@Deprecated
public class MorphiaIterator<T, V> implements Iterable<V>, Iterator<V> {
    private final Iterator<DBObject> wrapped;
    private final Mapper mapper;
    private final Class<T> clazz;
    private final String collection;
    private final EntityCache cache;
    private long driverTime;
    private long mapperTime;
    private Datastore datastore;

    /**
     * Creates a MorphiaIterator
     * @param datastore  the Datastore to use when fetching this reference
     * @param it         the Iterator to use
     * @param mapper     the Mapper to use
     * @param clazz      the original type being iterated
     * @param collection the mongodb collection
     * @param cache      the EntityCache
     */
    public MorphiaIterator(final Datastore datastore, final Iterator<DBObject> it, final Mapper mapper, final Class<T> clazz,
                           final String collection, final EntityCache cache) {
        wrapped = it;
        this.mapper = mapper;
        this.clazz = clazz;
        this.collection = collection;
        this.cache = cache;
        this.datastore = datastore;
    }

    /**
     * Closes the underlying cursor.
     */
    public void close() {
        if (wrapped != null && wrapped instanceof DBCursor) {
            ((DBCursor) wrapped).close();
        }
    }

    /**
     * @return the original class type.
     */
    public Class<T> getClazz() {
        return clazz;
    }

    /**
     * @return the mongodb collection
     */
    public String getCollection() {
        return collection;
    }

    /**
     * @return the underlying DBCursor
     */
    public DBCursor getCursor() {
        return (DBCursor) wrapped;
    }

    /**
     * @return the time spent calling the driver in ms
     */
    public long getDriverTime() {
        return driverTime;
    }

    /**
     * @return the Mapper being used
     */
    public Mapper getMapper() {
        return mapper;
    }

    /**
     * @return the time spent calling the mapper in ms
     */
    public long getMapperTime() {
        return mapperTime;
    }

    @Override
    public boolean hasNext() {
        if (wrapped == null) {
            return false;
        }
        final long start = System.currentTimeMillis();
        final boolean ret = wrapped.hasNext();
        driverTime += System.currentTimeMillis() - start;
        return ret;
    }

    @Override
    public V next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        final DBObject dbObj = getNext();
        return processItem(dbObj);
    }

    @Override
    public void remove() {
        final long start = System.currentTimeMillis();
        wrapped.remove();
        driverTime += System.currentTimeMillis() - start;
    }

    @Override
    public Iterator<V> iterator() {
        return this;
    }

    @SuppressWarnings("unchecked")
    protected V convertItem(final DBObject dbObj) {
        return (V) mapper.fromDBObject(datastore, clazz, dbObj, cache);
    }

    protected DBObject getNext() {
        final long start = System.currentTimeMillis();
        final DBObject dbObj = wrapped.next();
        driverTime += System.currentTimeMillis() - start;
        return dbObj;
    }

    private V processItem(final DBObject dbObj) {
        final long start = System.currentTimeMillis();
        final V item = convertItem(dbObj);
        mapperTime += System.currentTimeMillis() - start;
        return item;
    }

    Datastore getDatastore() {
        return datastore;
    }
}
