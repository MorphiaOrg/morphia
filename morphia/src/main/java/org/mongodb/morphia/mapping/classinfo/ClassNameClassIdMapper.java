package org.mongodb.morphia.mapping.classinfo;

import org.mongodb.morphia.logging.Logger;
import org.mongodb.morphia.logging.MorphiaLoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A {@link ClassIdMapper} which simply uses the full class name as a class id and can optionally cache the classes
 * after load. This is designed for backward compatibility with older versions of Morphia.
 */
public class ClassNameClassIdMapper implements ClassIdMapper {

    private static final Logger LOG = MorphiaLoggerFactory.get(ClassNameClassIdMapper.class);

    private final Map<String, Class<?>> classNameCache = new ConcurrentHashMap<String, Class<?>>();
    private boolean cacheClassLookUps;

    @Override
    public String getId(final Object value) {
        return value.getClass().getName();
    }

    @Override
    public <T> Class<T> getClass(final String id) {
        @SuppressWarnings("unchecked")
        Class<T> clazz = (Class<T>) (cacheClassLookUps ? getClassCached(id) : doGetClass(id));
        return clazz;
    }

    private Class<?> doGetClass(final String className) {
        try {
            return Class.forName(
                    className,
                    true,
                    Thread.currentThread().getContextClassLoader()
            );
        } catch (ClassNotFoundException e) {
            if (LOG.isWarningEnabled()) {
                LOG.warning("Class not found defined in dbObj: ", e);
            }
            return null;
        }
    }

    private Class<?> getClassCached(final String className) {
        final Class<?> cached = classNameCache.get(className);
        if (cached != null) {
            return cached;
        }

        final Class<?> loaded = doGetClass(className);
        classNameCache.put(className, loaded);

        return loaded;
    }

    @Override
    public void setCaching(final boolean caching) {
        this.cacheClassLookUps = caching;
    }

    @Override
    public boolean isCaching() {
        return cacheClassLookUps;
    }

    /**
     * @return a copy of the current class name cache
     * @deprecated unnecessarily published
     */
    @Deprecated
    public Map<String, Class> getClassNameCache() {
        return new HashMap<String, Class>(classNameCache);
    }
}
