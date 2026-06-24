package dev.morphia.mapping.codec;

import java.util.HashMap;
import java.util.Map;

import com.mongodb.lang.Nullable;

import dev.morphia.annotations.internal.MorphiaInternal;

/**
 * Per-cursor-decode cache that maps (collection, id) → entity instance.
 * Activated via {@link DecodeSession#activate()} at cursor creation and cleared via
 * {@link DecodeSession#deactivate()} when the cursor is closed, so all documents in
 * one query iteration share the same cache.
 *
 * @hidden
 * @morphia.internal
 */
@MorphiaInternal
public class DecodeSession {
    private static final ThreadLocal<DecodeSession> CURRENT = new ThreadLocal<>();

    private final Map<String, Map<Object, Object>> cache = new HashMap<>();

    private DecodeSession() {
    }

    /**
     * Activates a session on the current thread. If a session is already active it is
     * reused, so nested activations (e.g. fetching a @Reference while decoding an outer
     * document) share one cache. Returns {@code true} if this call created the root session
     * and therefore owns the responsibility of calling {@link #deactivate()}.
     *
     * @return {@code true} if a new root session was created; {@code false} if an existing session was reused
     */
    public static boolean activate() {
        if (CURRENT.get() != null) {
            return false;
        }
        CURRENT.set(new DecodeSession());
        return true;
    }

    /**
     * Returns the session active on the current thread, or {@code null} if none.
     */
    @Nullable
    public static DecodeSession current() {
        return CURRENT.get();
    }

    /**
     * Removes the session from the current thread.
     */
    public static void deactivate() {
        CURRENT.remove();
    }

    /**
     * Stores a decoded entity in the cache.
     *
     * @param collection the MongoDB collection name
     * @param id         the entity's {@code _id} value
     * @param entity     the decoded entity instance
     */
    public void register(String collection, Object id, Object entity) {
        cache.computeIfAbsent(collection, k -> new HashMap<>()).put(id, entity);
    }

    /**
     * Returns a previously cached entity, or {@code null} if not present.
     *
     * @param collection the MongoDB collection name
     * @param id         the entity's {@code _id} value
     */
    @Nullable
    public Object lookup(String collection, Object id) {
        Map<Object, Object> col = cache.get(collection);
        return col != null ? col.get(id) : null;
    }
}
