package dev.morphia.mapping.codec;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

import com.mongodb.lang.Nullable;

import dev.morphia.annotations.internal.MorphiaInternal;
import dev.morphia.config.MorphiaConfig;

/**
 * A cache of {@code (collection, id) -> entity instance} used while decoding. It serves two purposes:
 *
 * <ol>
 * <li>cycle safety: an entity is published to the session <em>before</em> its properties are decoded, so a
 * reference cycle A&rarr;B&rarr;A resolves to the in-progress instance instead of recursing forever</li>
 * <li>deduplication: two {@code @Reference} fields pointing at the same document decode to the same
 * Java instance</li>
 * </ol>
 *
 * <p>
 * A session is <em>installed</em> on the current thread only for the duration of a decode, via
 * {@link #decoding(Supplier)}. It is never left behind on the thread: an owner that is never closed
 * (an unclosed cursor, say) can leak at most the session object itself, which is unreachable garbage,
 * rather than poisoning later decodes on that thread with stale entities.
 *
 * <p>
 * Entities being decoded live in an unbounded in-flight map that is emptied as each decode finishes.
 * Completed entities move to a bounded LRU cache so that a cursor over a very large result set does not
 * retain every document it has seen; eviction costs deduplication but never cycle safety. The bound
 * comes from {@link dev.morphia.config.MorphiaConfig#decodeSessionCacheSize()}, where {@code 0}
 * disables cross-document caching entirely while leaving cycle safety intact.
 *
 * @hidden
 * @morphia.internal
 */
@MorphiaInternal
public class DecodeSession {
    /**
     * The bound used when no configuration is at hand; also the default for
     * {@link MorphiaConfig#decodeSessionCacheSize()}.
     */
    public static final int DEFAULT_CACHE_SIZE = 1_000;

    private static final ThreadLocal<DecodeSession> CURRENT = new ThreadLocal<>();

    private final int maxCached;

    @Nullable
    private Map<Key, Object> inFlight;

    @Nullable
    private Map<Key, Object> cache;

    /**
     * Creates a session bounded by {@link #DEFAULT_CACHE_SIZE}. Prefer {@link #forConfig(MorphiaConfig)}
     * wherever the configuration is reachable.
     */
    public DecodeSession() {
        this(DEFAULT_CACHE_SIZE);
    }

    /**
     * Creates a session that will cache at most {@code maxCached} completed entities.
     *
     * @param maxCached the cache bound; {@code 0} disables caching of completed entities
     */
    public DecodeSession(int maxCached) {
        this.maxCached = Math.max(0, maxCached);
    }

    /**
     * Creates a session bounded by {@link MorphiaConfig#decodeSessionCacheSize()}.
     *
     * @param config the configuration to read the bound from
     * @return the new session
     */
    public static DecodeSession forConfig(MorphiaConfig config) {
        return new DecodeSession(config.decodeSessionCacheSize());
    }

    /**
     * @return the session installed on the current thread, or {@code null} if none
     */
    @Nullable
    public static DecodeSession current() {
        return CURRENT.get();
    }

    /**
     * Runs {@code action} with this session installed on the current thread, removing it again when the
     * action completes. If another session is already installed -- a reference being fetched part way
     * through an outer decode, for instance -- that one stays in place and this session is not used, so
     * the whole object graph shares a single cache.
     *
     * @param action the decode to run
     * @param <T>    the decoded type
     * @return whatever {@code action} returns
     */
    public <T> T decoding(Supplier<T> action) {
        if (CURRENT.get() != null) {
            return action.get();
        }
        CURRENT.set(this);
        try {
            return action.get();
        } finally {
            CURRENT.remove();
        }
    }

    /**
     * Publishes an entity whose properties have not been decoded yet. It is visible to {@link #lookup} so
     * that cycles terminate, and is never evicted; {@link #complete} or {@link #discard} must follow.
     *
     * @param collection the MongoDB collection name
     * @param id         the entity's {@code _id} value
     * @param entity     the entity instance being decoded
     */
    public void registerInFlight(String collection, Object id, Object entity) {
        if (inFlight == null) {
            inFlight = new LinkedHashMap<>();
        }
        inFlight.put(new Key(collection, id), entity);
    }

    /**
     * Marks an in-flight entity as fully decoded, moving it to the bounded cache.
     *
     * @param collection the MongoDB collection name
     * @param id         the entity's {@code _id} value
     */
    public void complete(String collection, Object id) {
        if (inFlight != null) {
            Object entity = inFlight.remove(new Key(collection, id));
            if (entity != null) {
                register(collection, id, entity);
            }
        }
    }

    /**
     * Drops an in-flight entity without caching it, for a decode that failed part way through.
     *
     * @param collection the MongoDB collection name
     * @param id         the entity's {@code _id} value
     */
    public void discard(String collection, Object id) {
        if (inFlight != null) {
            inFlight.remove(new Key(collection, id));
        }
    }

    /**
     * Caches a fully decoded entity.
     *
     * @param collection the MongoDB collection name
     * @param id         the entity's {@code _id} value
     * @param entity     the decoded entity instance
     */
    public void register(String collection, Object id, Object entity) {
        if (maxCached == 0) {
            return;
        }
        if (cache == null) {
            cache = new LinkedHashMap<>(16, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<Key, Object> eldest) {
                    return size() > maxCached;
                }
            };
        }
        cache.put(new Key(collection, id), entity);
    }

    /**
     * Returns a known entity, preferring one that is still being decoded so that cycles resolve.
     *
     * @param collection the MongoDB collection name
     * @param id         the entity's {@code _id} value
     * @return the entity, or {@code null} if this session has not seen it
     */
    @Nullable
    public Object lookup(String collection, Object id) {
        Key key = new Key(collection, id);
        Object entity = inFlight != null ? inFlight.get(key) : null;
        return entity != null ? entity : (cache != null ? cache.get(key) : null);
    }

    private static final class Key {
        private final String collection;
        private final Object id;

        Key(String collection, Object id) {
            this.collection = collection;
            this.id = id;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof Key)) {
                return false;
            }
            Key other = (Key) o;
            return collection.equals(other.collection) && Objects.equals(id, other.id);
        }

        @Override
        public int hashCode() {
            return 31 * collection.hashCode() + Objects.hashCode(id);
        }
    }
}
