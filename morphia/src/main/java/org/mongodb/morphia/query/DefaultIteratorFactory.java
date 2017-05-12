package org.mongodb.morphia.query;

import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.mapping.Mapper;
import org.mongodb.morphia.mapping.cache.EntityCache;

import java.util.Iterator;

/**
 * Default implementation of Iterator factory.
 *
 * @author Ross M. Lodge
 */
public class DefaultIteratorFactory implements IteratorFactory {
    @Override
    public <T, V> MorphiaIterator<T, V> createIterator(
        final Datastore datastore,
        final Iterator<DBObject> it,
        final Mapper mapper,
        final Class<T> clazz,
        final String collection,
        final EntityCache cache
    ) {
        return new MorphiaIteratorImpl<T, V>(datastore, it, mapper, clazz, collection, cache);
    }

    @Override
    public <T> MorphiaKeyIterator<T> createKeyIterator(
        final Datastore datastore,
        final DBCursor cursor,
        final Mapper mapper,
        final Class<T> clazz,
        final String collection
    ) {
        return new MorphiaKeyIteratorImpl<T>(datastore, cursor, mapper, clazz, collection);
    }
}
