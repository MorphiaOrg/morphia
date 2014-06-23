package org.mongodb.morphia.utils;


import org.bson.BSONObject;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;


/**
 * Helper to allow for optimizations for different types of Map/Collections
 *
 * @param <T> The key type of the map
 * @param <V> The value type of the map/collection
 * @author Scott Hernandez
 */
public final class IterHelper<T, V> {
    @SuppressWarnings("unchecked")
    public void loopMap(final Object x, final MapIterCallback<T, V> iter) {
        if (x == null) {
            return;
        }

        if (x instanceof Collection) {
            throw new IllegalArgumentException("call loop instead");
        }

        if (x instanceof HashMap<?, ?>) {
            if (((HashMap) x).isEmpty()) {
                return;
            }

            final HashMap<?, ?> hm = (HashMap<?, ?>) x;
            for (final Entry<?, ?> e : hm.entrySet()) {
                iter.eval((T) e.getKey(), (V) e.getValue());
            }
            return;
        }
        if (x instanceof Map) {
            final Map<T, V> m = (Map<T, V>) x;
            for (final Entry<T, V> entry : m.entrySet()) {
                iter.eval(entry.getKey(), entry.getValue());
            }
            return;
        }
        if (x instanceof BSONObject) {
            final BSONObject m = (BSONObject) x;
            for (final String k : m.keySet()) {
                iter.eval((T) k, (V) m.get(k));
            }
        }

    }

    /**
     * Calls eval for each entry found, or just once if the "x" isn't iterable/collection/list/etc. with "x"
     */
    @SuppressWarnings({"unchecked"})
    public void loopOrSingle(final Object x, final IterCallback<V> iter) {
        if (x == null) {
            return;
        }

        //A collection
        if (x instanceof Collection<?>) {
            final Collection<?> l = (Collection<?>) x;
            for (final Object o : l) {
                iter.eval((V) o);
            }
            return;
        }

        //An array of Object[]
        if (x.getClass().isArray()) {
            for (final Object o : (Object[]) x) {
                iter.eval((V) o);
            }
            return;
        }

        iter.eval((V) x);
    }

    public abstract static class MapIterCallback<T, V> {
        public abstract void eval(T t, V v);
    }

    public abstract static class IterCallback<V> {
        public abstract void eval(V v);
    }
}
