package dev.morphia.query.internal;

import com.mongodb.ServerAddress;
import com.mongodb.ServerCursor;
import com.mongodb.client.MongoCursor;
import com.mongodb.lang.NonNull;
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
@Deprecated(since = "2.0", forRemoval = true)
public class MorphiaKeyCursor<T> implements MongoCursor<Key<T>> {
    private final MongoCursor<Document> wrapped;
    private final Mapper mapper;
    private final Class<T> clazz;
    private final String collection;


    /**
     * Create
     *
     * @param cursor     the cursor to use
     * @param mapper     the Mapper to use
     * @param clazz      the original type being iterated
     * @param collection the mongodb collection
     */
    public MorphiaKeyCursor(MongoCursor<Document> cursor, Mapper mapper, Class<T> clazz, String collection) {
        this.wrapped = cursor;
        if (wrapped == null) {
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
    @NonNull
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

    @Override
    public ServerCursor getServerCursor() {
        return wrapped.getServerCursor();
    }

    @Override
    @NonNull
    public ServerAddress getServerAddress() {
        return wrapped.getServerAddress();
    }

    private Key<T> convertItem(Document document) {
        Object id = document.get("_id");
        if (id instanceof Document) {
            id = mapper.fromDocument(clazz, (Document) id);
        }
        return new Key<>(clazz, collection, id);
    }

    /**
     * Converts this cursor to a List.  Care should be taken on large datasets as OutOfMemoryErrors are a risk.
     *
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
    public void remove() {
        wrapped.remove();
    }
}
