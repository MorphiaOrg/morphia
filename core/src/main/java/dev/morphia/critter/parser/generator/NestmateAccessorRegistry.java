package dev.morphia.critter.parser.generator;

import java.util.concurrent.ConcurrentHashMap;

import org.bson.codecs.pojo.PropertyAccessor;

/**
 * Static registry mapping stable accessor names to pre-instantiated {@link PropertyAccessor} instances
 * for hidden nestmate accessor classes (which cannot be looked up by name via {@link Class#forName}).
 */
public final class NestmateAccessorRegistry {
    private static final ConcurrentHashMap<String, PropertyAccessor<?>> INSTANCES = new ConcurrentHashMap<>();

    private NestmateAccessorRegistry() {
    }

    public static void register(String key, PropertyAccessor<?> accessor) {
        INSTANCES.put(key, accessor);
    }

    public static PropertyAccessor<?> get(String key) {
        return INSTANCES.get(key);
    }
}
