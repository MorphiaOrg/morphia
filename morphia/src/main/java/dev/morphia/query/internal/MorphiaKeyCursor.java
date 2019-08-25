package dev.morphia.query.internal;

import com.mongodb.ServerAddress;
import com.mongodb.ServerCursor;
import com.mongodb.client.MongoCursor;
import dev.morphia.Key;
import dev.morphia.mapping.Mapper;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Defines an Iterator across the Key values for a given type.
 *
 * @param <T> the entity type
 */
public class MorphiaKeyCursor<T> implements MongoCursor<Key<T>> {
    private final MongoCursor<Document> wrapped;
    private final Mapper mapper;
    private final Class<T> clazz;
    private final String collection;


    /**
     * Create
     * @param cursor     the cursor to use
     * @param mapper     the Mapper to use
     * @param clazz      the original type being iterated
     * @param collection the mongodb collection
     */
    public MorphiaKeyCursor(final MongoCursor<Document> cursor, final Mapper mapper, final Class<T> clazz, final String collection) {
        this.wrapped = cursor;
        if(wrapped == null) {
            throw new IllegalArgumentException("The wrapped cursor can not be null");
        }
        this.mapper = mapper;
        this.clazz = clazz;
        this.collection = collection;
    }

    /**
     * Closes the underlying cursor.
     */
    public void close() {
        if (wrapped != null) {
            wrapped.close();
        }
    }

    @Override
    public boolean hasNext() {
        if (wrapped == null) {
            return false;
        }
        return wrapped.hasNext();
    }

    @Override
    public Key<T> next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        return convertItem(wrapped.next());
    }

    @Override
    public Key<T> tryNext() {
        if (hasNext()) {
            return next();
        } else {
            return null;
        }
    }

    /**
     * Converts this cursor to a List.  Care should be taken on large datasets as OutOfMemoryErrors are a risk.
     * @return the list of Entities
     */
    public List<Key<T>> toList() {
        final List<Key<T>> results = new ArrayList<>();
        try {
            while (wrapped.hasNext()) {
                results.add(next());
            }
        } finally {
            wrapped.close();
        }
        return results;
    }

    @Override
    public ServerCursor getServerCursor() {
        return wrapped.getServerCursor();
    }

    @Override
    public ServerAddress getServerAddress() {
        return wrapped.getServerAddress();
    }

    @Override
    public void remove() {
        wrapped.remove();
    }

    private Key<T> convertItem(final Document document) {
        Object id = document.get("_id");
        if (id instanceof Document) {
            id = mapper.fromDocument(clazz, (Document) id);
        }
        return new Key<>(clazz, collection, id);
    }
}
