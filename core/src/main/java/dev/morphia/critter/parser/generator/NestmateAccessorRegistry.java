package dev.morphia.critter.parser.generator;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

import org.bson.codecs.pojo.PropertyAccessor;

/**
 * Hands pre-instantiated {@link PropertyAccessor} instances for hidden nestmate accessor classes (which cannot be looked
 * up by name via {@link Class#forName}) to the generated property models that use them.
 * <p>
 * Accessors are registered per {@link dev.morphia.critter.CritterClassLoader}. The same entity can be generated into
 * several loaders with different mapping configurations (e.g. field and method property discovery), and a model must
 * always get the accessors generated alongside it, however many other generations have run since. Loaders are held
 * weakly, so a discarded loader's accessors are released with it.
 */
public final class NestmateAccessorRegistry {
    private static final Map<ClassLoader, Map<String, PropertyAccessor<?>>> INSTANCES = Collections.synchronizedMap(
            new WeakHashMap<>());

    private NestmateAccessorRegistry() {
    }

    /**
     * @param loader   the loader that defines the generated models using this accessor
     * @param key      the accessor's key
     * @param accessor the accessor
     */
    public static void register(ClassLoader loader, String key, PropertyAccessor<?> accessor) {
        INSTANCES.computeIfAbsent(loader, l -> new ConcurrentHashMap<>()).put(key, accessor);
    }

    /**
     * @param loader the loader that defines the generated model asking for the accessor
     * @param key    the accessor's key
     * @return the accessor, or null if none was registered for that loader
     */
    public static PropertyAccessor<?> get(ClassLoader loader, String key) {
        Map<String, PropertyAccessor<?>> accessors = INSTANCES.get(loader);
        return accessors != null ? accessors.get(key) : null;
    }
}
