package org.mongodb.morphia.query;

import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.mapping.Mapper;
import org.mongodb.morphia.mapping.cache.EntityCache;

import java.util.Iterator;

/**
 * An iterator factory so that we can use iterator interfaces and users can control iterator creation without having
 * to override a bunch of internal stuff.
 *
 * @author Ross M. Lodge
 */
public interface IteratorFactory {

    public <T, V> MorphiaIterator<T, V> createIterator(final Datastore datastore, final Iterator<DBObject> it, final Mapper mapper, final Class<T> clazz,
        final String collection, final EntityCache cache);

    public <T> MorphiaKeyIterator<T> createKeyIterator(final Datastore datastore, final DBCursor cursor, final Mapper mapper,
        final Class<T> clazz, final String collection);

}
