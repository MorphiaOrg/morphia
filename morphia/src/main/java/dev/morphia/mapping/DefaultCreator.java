package dev.morphia.mapping;


import dev.morphia.ObjectFactory;
import dev.morphia.annotations.ConstructorArgs;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;


public class DefaultCreator implements ObjectFactory {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultCreator.class);

    private Map<String, Class> classNameCache = new ConcurrentHashMap<>();

    private MapperOptions options;

    /**
     * Creates a new DefaultCreator with no options
     */
    public DefaultCreator() {
    }

    /**
     * Creates a new DefaultCreator with options
     *
     * @param options the options to apply
     */
    public DefaultCreator(final MapperOptions options) {
        this.options = options;
    }

    private static <T> Constructor<T> getNoArgsConstructor(final Class<T> type) {
        try {
            final Constructor<T> constructor = type.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor;
        } catch (NoSuchMethodException e) {
            throw new MappingException("No usable constructor for " + type.getName(), e);
        }
    }

    /**
     * @param clazz the Class of the type to create
     * @param <T>   the type of the class
     * @return the new instance
     * @deprecated use {@link #createInstance(Class)} instead
     */
    @Deprecated
    public <T> T createInst(final Class<T> clazz) {
        return createInstance(clazz);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T createInstance(final Class<T> clazz) {
        try {
            return getNoArgsConstructor(clazz).newInstance();
        } catch (Exception e) {
            if (Map.class.isAssignableFrom(clazz)) {
                return (T) createMap(null);
            } else if (Set.class.isAssignableFrom(clazz)) {
                return (T) createSet(null);
            } else if (Collection.class.isAssignableFrom(clazz)) {
                return (T) createList(null);
            }
            throw new MappingException("No usable constructor for " + clazz.getName(), e);
        }
    }

    @Override
    public <T> T createInstance(final Class<T> clazz, final Document document) {
        Class<T> c = getClass(document);
        if (c == null) {
            c = clazz;
        }
        return createInstance(c);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object createInstance(final Mapper mapper, final MappedField mf, final Document document) {
        Class c = getClass(document);
        if (c == null) {
            c = mf.getNormalizedType();
        }
        try {
            return createInstance(c, document);
        } catch (RuntimeException e) {
            final ConstructorArgs argAnn = mf.getAnnotation(ConstructorArgs.class);
            if (argAnn == null) {
                throw e;
            }
            //TODO: now that we have a mapper, get the arg types that way by getting the fields by name. + Validate names
            final Object[] args = new Object[argAnn.value().length];
            final Class[] argTypes = new Class[argAnn.value().length];
            for (int i = 0; i < argAnn.value().length; i++) {
                // TODO: run converters and stuff against these. Kinda like the List of List stuff,
                // using a fake MappedField to hold the value
                final Object val = document.get(argAnn.value()[i]);
                args[i] = val;
                argTypes[i] = val.getClass();
            }
            try {
                final Constructor constructor = c.getDeclaredConstructor(argTypes);
                constructor.setAccessible(true);
                return constructor.newInstance(args);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public List createList(final MappedField mf) {
        return createInstance(ArrayList.class);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map createMap(final MappedField mf) {
        return createInstance(HashMap.class);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Set createSet(final MappedField mf) {
        return createInstance(HashSet.class);
    }

    /**
     * @return the cache of classnames
     */
    public Map<String, Class> getClassNameCache() {
        HashMap<String, Class> copy = new HashMap<>();
        copy.putAll(classNameCache);
        return copy;
    }

    protected ClassLoader getClassLoaderForClass() {
        return Thread.currentThread().getContextClassLoader();
    }

    @SuppressWarnings("unchecked")
    public <T> Class<T> getClass(final Document document) {
        // see if there is a className value
        Class c = null;
        if (document.containsKey(options.getDiscriminatorField())) {
            final String className = (String) document.get(options.getDiscriminatorField());
            // try to Class.forName(className) as defined in the documentect first,
            // otherwise return the entityClass
            try {
                if (options.isCacheClassLookups()) {
                    c = classNameCache.get(className);
                    if (c == null) {
                        c = Class.forName(className, true, getClassLoaderForClass());
                        classNameCache.put(className, c);
                    }
                } else {
                    c = Class.forName(className, true, getClassLoaderForClass());
                }
            } catch (ClassNotFoundException e) {
                if (LOG.isWarnEnabled()) {
                    LOG.warn("Class not found defined in document: ", e);
                }
            }
        }
        return c;
    }

}
