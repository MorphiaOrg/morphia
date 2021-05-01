package dev.morphia.query.internal;

import com.mongodb.client.MongoCursor;

/**
 * @param <T> the original type being iterated
 * @morphia.internal
 * @deprecated use {@link dev.morphia.query.MorphiaCursor} instead
 */
@Deprecated(forRemoval = true)
public class MorphiaCursor<T> extends dev.morphia.query.MorphiaCursor<T> {

    /**
     * Creates a MorphiaCursor
     *
     * @param cursor the Iterator to use
     */
    public MorphiaCursor(MongoCursor<T> cursor) {
        super(cursor);
    }
}
