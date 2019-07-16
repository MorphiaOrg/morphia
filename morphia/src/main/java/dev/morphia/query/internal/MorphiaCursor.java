package dev.morphia.query.internal;


import com.mongodb.ServerAddress;
import com.mongodb.ServerCursor;
import com.mongodb.client.MongoCursor;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;


/**
 * @param <T> the original type being iterated
 */
public class MorphiaCursor<T> implements MongoCursor<T> {
    private final MongoCursor<T> wrapped;

    /**
     * Creates a MorphiaCursor
     *
     * @param cursor     the Iterator to use
     */
    public MorphiaCursor(final MongoCursor<T> cursor) {
        wrapped = cursor;
        if(wrapped == null) {
            throw new IllegalArgumentException("The wrapped cursor can not be null");
        }
    }

    /**
     * Converts this cursor to a List.  Care should be taken on large datasets as OutOfMemoryErrors are a risk.
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
    public T next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        return wrapped.next();
    }

    @Override
    public T tryNext() {
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
    public ServerAddress getServerAddress() {
        return wrapped.getServerAddress();
    }

    @Override
    public void remove() {
        wrapped.remove();
    }

}
