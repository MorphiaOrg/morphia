package org.mongodb.morphia.mapping;


import com.mongodb.DBObject;
import org.mongodb.morphia.ObjectFactory;
import org.mongodb.morphia.annotations.ConstructorArgs;
import org.mongodb.morphia.logging.Logger;
import org.mongodb.morphia.logging.MorphiaLoggerFactory;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;


/**
 * @author ScottHernandez
 */
public class DefaultCreator implements ObjectFactory {

    private static final Logger LOG = MorphiaLoggerFactory.get(DefaultCreator.class);

    private Map<String, Class> classNameCache = new ConcurrentHashMap<String, Class>();

    private MapperOptions options = null;

    public DefaultCreator() {
    }

    public DefaultCreator(final MapperOptions options) {
        this.options = options;
    }

    /**
     * creates an instance of testType (if it isn't Object.class or null) or fallbackType
     */
    private <T> T newInstance(final Constructor<T> tryMe, final Class<T> fallbackType) {
        if (tryMe != null) {
            tryMe.setAccessible(true);
            try {
                return tryMe.newInstance();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return createInst(fallbackType);
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

    @Override
    public <T> T createInstance(final Class<T> clazz) {
        return createInst(clazz);
    }

    @Override
    public <T> T createInstance(final Class<T> clazz, final DBObject dbObj) {
        Class<T> c = getClass(dbObj);
        if (c == null) {
            c = clazz;
        }
        return createInstance(c);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object createInstance(final Mapper mapper, final MappedField mf, final DBObject dbObj) {
        Class c = getClass(dbObj);
        if (c == null) {
            c = mf.isSingleValue() ? mf.getConcreteType() : mf.getSubClass();
        }
        try {
            return createInstance(c, dbObj);
        } catch (RuntimeException e) {
            final ConstructorArgs argAnn = mf.getAnnotation(ConstructorArgs.class);
            if (argAnn == null) {
                throw e;
            }
            //TODO: now that we have a mapper, get the arg types that way by getting the fields by name. + Validate names
            final Object[] args = new Object[argAnn.value().length];
            final Class[] argTypes = new Class[argAnn.value().length];
            for (int i = 0; i < argAnn.value().length; i++) {
                //TODO: run converters and stuff against these. Kinda like the List of List stuff, 
                // using a fake MappedField to hold the value
                final Object val = dbObj.get(argAnn.value()[i]);
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
    public Map createMap(final MappedField mf) {
        return newInstance(mf != null ? mf.getCTor() : null, HashMap.class);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List createList(final MappedField mf) {
        return newInstance(mf != null ? mf.getCTor() : null, ArrayList.class);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Set createSet(final MappedField mf) {
        return newInstance(mf != null ? mf.getCTor() : null, HashSet.class);
    }

    @SuppressWarnings("unchecked")
    private <T> Class<T> getClass(final DBObject dbObj) {
        // see if there is a className value
        Class c = null;
        if (dbObj.containsField(Mapper.CLASS_NAME_FIELDNAME)) {
            final String className = (String) dbObj.get(Mapper.CLASS_NAME_FIELDNAME);
            // try to Class.forName(className) as defined in the dbObject first,
            // otherwise return the entityClass
            try {
                if (options != null && options.isCacheClassLookups()) {
                    c = classNameCache.get(className);
                    if (c == null) {
                        c = Class.forName(className, true, getClassLoaderForClass());
                        classNameCache.put(className, c);
                    }
                } else {
                    c = Class.forName(className, true, getClassLoaderForClass());
                }
            } catch (ClassNotFoundException e) {
                if (LOG.isWarningEnabled()) {
                    LOG.warning("Class not found defined in dbObj: ", e);
                }
            }
        }
        return c;
    }

    protected ClassLoader getClassLoaderForClass() {
        return Thread.currentThread().getContextClassLoader();
    }

    @SuppressWarnings("unchecked")
    public <T> T createInst(final Class<T> clazz) {
        try {
            return getNoArgsConstructor(clazz).newInstance();
        } catch (Exception e) {
            if (Collection.class.isAssignableFrom(clazz)) {
                return (T) createList(null);
            } else if (Map.class.isAssignableFrom(clazz)) {
                return (T) createMap(null);
            } else if (Set.class.isAssignableFrom(clazz)) {
                return (T) createSet(null);
            }
            throw new MappingException("No usable constructor for " + clazz.getName(), e);
        }
    }

    public Map<String, Class> getClassNameCache() {
        HashMap<String, Class> copy = new HashMap<String, Class>();
        copy.putAll(classNameCache);
        return copy;
    }

}
