package org.mongodb.morphia.mapping;


import com.mongodb.DBObject;
import org.mongodb.morphia.ObjectFactory;
import org.mongodb.morphia.annotations.ConstructorArgs;
import org.mongodb.morphia.mapping.classinfo.ClassInfoPersister;
import org.mongodb.morphia.mapping.classinfo.DefaultClassInfoPersister;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * @author ScottHernandez
 */
public class DefaultCreator implements ObjectFactory {

    private final ClassInfoPersister classInfoPersister;

    /**
     * Creates a new DefaultCreator with no options
     */
    public DefaultCreator() {
        classInfoPersister = new DefaultClassInfoPersister();
    }

    /**
     * Creates a new DefaultCreator with options
     *
     * @param classInfoPersister the strategy for getting class info out of data
     */
    public DefaultCreator(final ClassInfoPersister classInfoPersister) {
        this.classInfoPersister = classInfoPersister;
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

    @Override
    public <T> T createInstance(final Class<T> clazz, final DBObject dbObj) {
        Class<T> c = classInfoPersister.getClass(dbObj, clazz);
        return createInstance(c != null ? c : clazz);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object createInstance(final Mapper mapper, final MappedField mf, final DBObject dbObj) {
        Class c = classInfoPersister.getClass(dbObj, mf.getType());
        if (c == null) {
            c = mf.isSingleValue() ? mf.getConcreteType() : mf.getSubClass();
            if (c.equals(Object.class)) {
                c = mf.getConcreteType();
            }
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
                // TODO: run converters and stuff against these. Kinda like the List of List stuff,
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
    public List createList(final MappedField mf) {
        return newInstance(mf != null ? mf.getCTor() : null, ArrayList.class);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map createMap(final MappedField mf) {
        return newInstance(mf != null ? mf.getCTor() : null, HashMap.class);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Set createSet(final MappedField mf) {
        return newInstance(mf != null ? mf.getCTor() : null, HashSet.class);
    }

    /**
     * @return the cache of classnames
     */
    @Deprecated
    public Map<String, Class> getClassNameCache() {
        if (classInfoPersister instanceof DefaultClassInfoPersister) {
            return ((DefaultClassInfoPersister) classInfoPersister).getClassCache();
        }
        return new HashMap<String, Class>();
    }

    @Deprecated
    protected ClassLoader getClassLoaderForClass() {
        return Thread.currentThread().getContextClassLoader();
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
        return createInstance(fallbackType);
    }

}
