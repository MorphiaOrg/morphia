package dev.morphia.query.internal;


import com.mongodb.ServerAddress;
import com.mongodb.ServerCursor;
import com.mongodb.client.MongoCursor;
import com.mongodb.lang.NonNull;

import java.util.ArrayList;
import java.util.List;


/**
 * @param <T> the original type being iterated
 */
public class MorphiaCursor<T> implements MongoCursor<T> {
    private final MongoCursor<T> wrapped;

    /**
     * Creates a MorphiaCursor
     *
     * @param cursor the Iterator to use
     */
    public MorphiaCursor(MongoCursor<T> cursor) {
        wrapped = cursor;
        if (wrapped == null) {
            throw new IllegalArgumentException("The wrapped cursor can not be null");
        }
    }

    /**
     * Converts this cursor to a List.  Care should be taken on large datasets as OutOfMemoryErrors are a risk.
     *
     * @return the list of Entities
     */
    public List<T> toList() {
        final List<T> results = new ArrayList<>();
        try {
            while (wrapped.hasNext()) {
                results.add(next());
            }
        } finally {
            wrapped.close();
        }
        return results;
    }

    /**
     * Closes the underlying cursor.
     */
    public void close() {
        wrapped.close();
    }

    @Override
    public boolean hasNext() {
        return wrapped.hasNext();
    }

    @Override
    @NonNull
    public T next() {
        return wrapped.next();
    }

    @Override
    public T tryNext() {
        return wrapped.tryNext();
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

    @Override
    public void remove() {
        wrapped.remove();
    }

}
