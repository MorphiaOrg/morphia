package dev.morphia.query;

import java.util.ArrayList;
import java.util.List;

import com.mongodb.ServerAddress;
import com.mongodb.ServerCursor;
import com.mongodb.client.MongoCursor;
import com.mongodb.lang.NonNull;

import dev.morphia.annotations.internal.MorphiaInternal;

/**
 * @param <T> the original type being iterated
 * @since 2.2
 */
public class MorphiaCursor<T> implements AutoCloseable, MongoCursor<T> {
    private final MongoCursor<T> wrapped;

    /**
     * Creates a MorphiaCursor
     *
     * @param cursor the Iterator to use
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public MorphiaCursor(MongoCursor<T> cursor) {
        wrapped = cursor;
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
    public int available() {
        return wrapped.available();
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

    /**
     * Converts this cursor to a List. Care should be taken on large datasets as OutOfMemoryErrors are a risk.
     *
     * @return the list of Entities
     */
    public List<T> toList() {
        final List<T> results = new ArrayList<>();
        try (wrapped) {
            while (wrapped.hasNext()) {
                results.add(next());
            }
        }
        return results;
    }

}
