/**
 * 
 */
package com.google.code.morphia.mapping;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.code.morphia.ObjectFactory;
import com.mongodb.DBObject;

/**
 * @author ScottHernandez
 */
@SuppressWarnings({"unchecked","rawtypes"})
public class DefaultCreator implements ObjectFactory {
	//TODO: move code from reflectionUtils into here.
	
	/* (non-Javadoc)
	 * @see com.google.code.morphia.ObjectFactory#createInstance(java.lang.Class)
	 */
	public Object createInstance(Class clazz) {
        try
        {
            return getNoArgsConstructor(clazz).newInstance();
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
	}
	
	/* (non-Javadoc)
	 * @see com.google.code.morphia.ObjectFactory#createInstance(java.lang.Class, com.mongodb.DBObject)
	 */
	public Object createInstance(Class clazz, DBObject dbObj) {
		// see if there is a className value
		String className = (String) dbObj.get(Mapper.CLASS_NAME_FIELDNAME);
		Class c = clazz;
		if (className != null) {
			// try to Class.forName(className) as defined in the dbObject first,
			// otherwise return the entityClass
			c = getClassForName(className, clazz);
		}
		return createInstance(c);	}
	
	/* (non-Javadoc)
	 * @see com.google.code.morphia.ObjectFactory#createInstance(com.google.code.morphia.mapping.MappedField, com.mongodb.DBObject)
	 */
	public Object createInstance(MappedField mf, DBObject dbObj) {
		// see if there is a className value
		return createInstance(mf.getConcreteType(), dbObj);
	}

	public Map createMap(MappedField mf) {
		return (Map) newInstance(mf.getCTor(), HashMap.class);
	}

	public List createList(MappedField mf) {
		return (List) newInstance(mf.getCTor(), ArrayList.class);
	}

	public Set createSet(MappedField mf) {
		return (Set) newInstance(mf.getCTor(), HashSet.class);
	}
	
    /** creates an instance of testType (if it isn't Object.class or null) or fallbackType */
    private Object newInstance(final Constructor tryMe, final Class fallbackType) {
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
    
	private Constructor getNoArgsConstructor(final Class ctorType) {
		try {
			Constructor ctor = ctorType.getDeclaredConstructor();
			ctor.setAccessible(true);
			return ctor;
		} catch (NoSuchMethodException e) {
			throw new MappingException("No usable constructor for " + ctorType.getName(), e);
		}
	}
	/**
	 * gets the Class for some classname, or if the className is not found,
	 * return the defaultClass instance
	 */
	private static Class getClassForName(final String className, final Class defaultClass) {
		try {
			Class c = Class.forName(className, true, Thread.currentThread().getContextClassLoader());
			return c;
		} catch (ClassNotFoundException ex) {
			return defaultClass;
		}
	}

}
