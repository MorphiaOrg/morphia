package org.mongodb.morphia.query;


import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import org.mongodb.morphia.mapping.Mapper;
import org.mongodb.morphia.mapping.cache.EntityCache;

import java.util.Iterator;
import java.util.NoSuchElementException;


/**
 * @author Scott Hernandez
 */
public class MorphiaIterator<T, V> implements Iterable<V>, Iterator<V> {
    private final Iterator<DBObject> wrapped;
    private final Mapper mapper;
    private final Class<T> clazz;
    private final String collection;
    private final EntityCache cache;
    private long driverTime;
    private long mapperTime;

    public MorphiaIterator(final Iterator<DBObject> it, final Mapper mapper, final Class<T> clazz, final String collection,
                           final EntityCache cache) {
        wrapped = it;
        this.mapper = mapper;
        this.clazz = clazz;
        this.collection = collection;
        this.cache = cache;
    }

    public Class<T> getClazz() {
        return clazz;
    }

    public String getCollection() {
        return collection;
    }

    public Mapper getMapper() {
        return mapper;
    }

    @Override
    public Iterator<V> iterator() {
        return this;
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

    protected V processItem(final DBObject dbObj) {
        final long start = System.currentTimeMillis();
        final V item = convertItem(dbObj);
        mapperTime += System.currentTimeMillis() - start;
        return item;
    }

    protected DBObject getNext() {
        final long start = System.currentTimeMillis();
        final DBObject dbObj = wrapped.next();
        driverTime += System.currentTimeMillis() - start;
        return dbObj;
    }

    @SuppressWarnings("unchecked")
    protected V convertItem(final DBObject dbObj) {
        return (V) mapper.fromDBObject(clazz, dbObj, cache);
    }

    @Override
    public void remove() {
        final long start = System.currentTimeMillis();
        wrapped.remove();
        driverTime += System.currentTimeMillis() - start;
    }

    /**
     * Returns the time spent calling the driver in ms
     */
    public long getDriverTime() {
        return driverTime;
    }

    /**
     * Returns the time spent calling the mapper in ms
     */
    public long getMapperTime() {
        return mapperTime;
    }

    public DBCursor getCursor() {
        return (DBCursor) wrapped;
    }

    public void close() {
        if (wrapped != null && wrapped instanceof DBCursor) {
            ((DBCursor) wrapped).close();
        }
    }
}