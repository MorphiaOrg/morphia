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
    private final Mapper m;
    private final Class<T> clazz;
    private final String kind;
    private final EntityCache cache;
    private long driverTime;
    private long mapperTime;

    public MorphiaIterator(final Iterator<DBObject> it, final Mapper m, final Class<T> clazz, final String kind, final EntityCache cache) {
        wrapped = it;
        this.m = m;
        this.clazz = clazz;
        this.kind = kind;
        this.cache = cache;
    }

    public Class<T> getClazz() {
        return clazz;
    }

    public String getKind() {
        return kind;
    }

    public Iterator<V> iterator() {
        return this;
    }

    public boolean hasNext() {
        if (wrapped == null) {
            return false;
        }
        final long start = System.currentTimeMillis();
        final boolean ret = wrapped.hasNext();
        driverTime += System.currentTimeMillis() - start;
        return ret;
    }

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

    protected V convertItem(final DBObject dbObj) {
        return (V) m.fromDBObject(clazz, dbObj, cache);
    }

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