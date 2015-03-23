package org.mongodb.morphia.mapping;


import com.mongodb.DBObject;
import org.mongodb.morphia.ObjectFactory;
import org.mongodb.morphia.annotations.ConstructorArgs;
import org.mongodb.morphia.logging.Logger;
import org.mongodb.morphia.logging.MorphiaLoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * @author ScottHernandez
 */
public class DefaultCreator implements ObjectFactory {
    private static final Logger LOG = MorphiaLoggerFactory.get(DefaultCreator.class);

    public <T> T createInstance(final Class<T> clazz) {
        return createInst(clazz);
    }

    public <T> T createInstance(final Class<T> clazz, final DBObject dbObj) {
        Class<T> c = getClass(dbObj);
        if (c == null) {
            c = clazz;
        }
        return createInstance(c);
    }

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

    @SuppressWarnings("unchecked")
    private <T> Class<T>  getClass(final DBObject dbObj) {
        // see if there is a className value
        final String className = (String) dbObj.get(Mapper.CLASS_NAME_FIELDNAME);
        Class c = null;
        if (className != null) {
            // try to Class.forName(className) as defined in the dbObject first,
            // otherwise return the entityClass
            try {
                c = Class.forName(className, true, getClassLoaderForClass());
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

    public Map createMap(final MappedField mf) {
        return (Map) newInstance(mf != null ? mf.getCTor() : null, HashMap.class);
    }

    public List createList(final MappedField mf) {
        return (List) newInstance(mf.getCTor(), ArrayList.class);
    }

    public Set createSet(final MappedField mf) {
        return (Set) newInstance(mf.getCTor(), HashSet.class);
    }


    public static <T> T createInst(final Class<T> clazz) {
        try {
            return getNoArgsConstructor(clazz).newInstance();
        } catch (InstantiationException e) {
            throw new RuntimeException(e.getMessage(), e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e.getMessage(), e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    /**
     * creates an instance of testType (if it isn't Object.class or null) or fallbackType
     */
    private static Object newInstance(final Constructor tryMe, final Class fallbackType) {
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
}
