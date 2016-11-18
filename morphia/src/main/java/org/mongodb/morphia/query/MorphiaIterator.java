package org.mongodb.morphia.query;

import com.mongodb.DBCursor;
import org.mongodb.morphia.mapping.Mapper;

import java.util.Iterator;

/**
 * @author Ross M. Lodge
 */
public interface MorphiaIterator<T, V> extends Iterable<V>,Iterator<V> {
    void close();

    Class<T> getClazz();

    String getCollection();

    DBCursor getCursor();

    long getDriverTime();

    Mapper getMapper();

    long getMapperTime();

    @Override
    boolean hasNext();

    @Override
    V next();

    @Override
    void remove();

    @Override
    Iterator<V> iterator();
}
