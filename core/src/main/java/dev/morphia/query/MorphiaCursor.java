package dev.morphia.query;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import com.mongodb.ServerAddress;
import com.mongodb.ServerCursor;
import com.mongodb.client.MongoCursor;
import com.mongodb.lang.NonNull;

import dev.morphia.annotations.internal.MorphiaInternal;
import dev.morphia.mapping.codec.DecodeSession;

/**
 * @param <T> the original type being iterated
 * @since 2.2
 */
public class MorphiaCursor<T> implements AutoCloseable, MongoCursor<T> {
    private final MongoCursor<T> wrapped;
    private final DecodeSession session;

    /**
     * Creates a MorphiaCursor
     *
     * @param cursor the Iterator to use
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public MorphiaCursor(MongoCursor<T> cursor) {
        session = new DecodeSession();
        wrapped = cursor;
    }

    /**
     * Creates a MorphiaCursor, opening the underlying cursor within this cursor's decode session. The
     * driver decodes a whole batch of documents when it fetches one -- including the first batch, which it
     * fetches while the cursor is being opened -- so the cursor has to be opened inside the session for
     * those documents to share it.
     *
     * @param cursor supplies the Iterator to use
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public MorphiaCursor(Supplier<MongoCursor<T>> cursor) {
        this(new DecodeSession(), cursor);
    }

    /**
     * Creates a MorphiaCursor using the given decode session, opening the underlying cursor within it.
     *
     * @param session the decode session to use for this cursor's decodes
     * @param cursor  supplies the Iterator to use
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public MorphiaCursor(DecodeSession session, Supplier<MongoCursor<T>> cursor) {
        this.session = session;
        wrapped = session.decoding(cursor);
    }

    /**
     * Closes the underlying cursor.
     */
    public void close() {
        wrapped.close();
    }

    @Override
    public boolean hasNext() {
        // the driver decodes an entire batch of documents when it fetches one, and it fetches from here
        return session.decoding(wrapped::hasNext);
    }

    @Override
    @NonNull
    public T next() {
        return session.decoding(wrapped::next);
    }

    @Override
    public int available() {
        return wrapped.available();
    }

    @Override
    public T tryNext() {
        return session.decoding(wrapped::tryNext);
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
        try (this) {
            while (hasNext()) {
                results.add(next());
            }
        }
        return results;
    }

}
