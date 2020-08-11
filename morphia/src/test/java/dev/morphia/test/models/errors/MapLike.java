package dev.morphia.test.models.errors;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import org.bson.types.ObjectId;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Entity(useDiscriminator = false)
public class MapLike implements Map<String, String> {
    private final Map<String, String> realMap = new HashMap<>();
    @Id
    private ObjectId id;

    @Override
    public int size() {
        return realMap.size();
    }

    @Override
    public boolean isEmpty() {
        return realMap.isEmpty();
    }

    @Override
    public boolean containsKey(final Object key) {
        return realMap.containsKey(key);
    }

    @Override
    public boolean containsValue(final Object value) {
        return realMap.containsValue(value);
    }

    @Override
    public String get(final Object key) {
        return realMap.get(key);
    }

    @Override
    public String put(final String key, final String value) {
        return realMap.put(key, value);
    }

    @Override
    public String remove(final Object key) {
        return realMap.remove(key);
    }

    @Override
    public void putAll(final Map<? extends String, ? extends String> m) {
        realMap.putAll(m);
    }

    @Override
    public void clear() {
        realMap.clear();
    }

    @Override
    public Set<String> keySet() {
        return realMap.keySet();
    }

    @Override
    public Collection<String> values() {
        return realMap.values();
    }

    @Override
    public Set<Entry<String, String>> entrySet() {
        return realMap.entrySet();
    }
}
