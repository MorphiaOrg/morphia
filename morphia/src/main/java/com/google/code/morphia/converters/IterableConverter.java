/**
 * 
 */
package com.google.code.morphia.converters;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import com.google.code.morphia.ObjectFactory;
import com.google.code.morphia.mapping.MappedField;
import com.google.code.morphia.mapping.MappingException;
import com.google.code.morphia.utils.ReflectionUtils;

/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 * @author scotthernandez
 */

@SuppressWarnings({"unchecked","rawtypes"})
public class IterableConverter extends TypeConverter {
	private final DefaultConverters chain;
	
	public IterableConverter(DefaultConverters chain) {
		this.chain = chain;
	}
	
	@Override
	protected
	boolean isSupported(Class c, MappedField mf) {
		if (mf != null)
			return mf.isMultipleValues() && !mf.isMap(); //&& !mf.isTypeMongoCompatible();
		else
			return c.isArray() || ReflectionUtils.implementsInterface(c, Iterable.class);
	}
	
	@Override
	public
	Object decode(Class targetClass, Object fromDBObject, MappedField mf) throws MappingException {
		if (mf == null || fromDBObject == null) return fromDBObject;
		
		Class subtypeDest = mf.getSubClass();
		Collection vals = null;
		
		if (fromDBObject.getClass().isArray()) {
			vals = new ArrayList();
			for(Object o : (Object[])fromDBObject)
				vals.add(chain.decode( (subtypeDest != null) ? subtypeDest : o.getClass(), o));
		} else if (fromDBObject instanceof Iterable) {
			// map back to the java datatype
			// (List/Set/Array[])
			vals = createNewCollection(mf);
			for (Object o : (Iterable) fromDBObject)
				vals.add(chain.decode((subtypeDest != null) ? subtypeDest : o.getClass(), o));
		}

		if (mf.getType().isArray()) {
			Object[] retArray = ReflectionUtils.convertToArray(subtypeDest, (ArrayList)vals);
			return retArray;
		} else
			return vals;
	}
	
	private Collection<?> createNewCollection(final MappedField mf) {
		ObjectFactory of = mapr.getOptions().objectFactory;
		return mf.isSet() ? of.createSet(mf) : of.createList(mf);
	}
	
	@Override
	public
	Object encode(Object value, MappedField f) {
		
		if (value == null)
			return null;
		
		Iterable<?> iterableValues = null;
		
		if (value.getClass().isArray()) {
			
			if (Array.getLength(value) == 0) {
				return value;
			}

			if (value.getClass().getComponentType().isPrimitive())
				return value;
			
			iterableValues = Arrays.asList((Object[]) value);
		} else {
			// cast value to a common interface
			iterableValues = (Iterable<?>) value;
		}
		
		List values = new ArrayList();
		if (f != null && f.getSubClass() != null) {
			for (Object o : iterableValues) {
				values.add(chain.encode(f.getSubClass(), o));
			}
		} else {
			for (Object o : iterableValues) {
				values.add(chain.encode(o));
			}
		}
		if (values.size() > 0) {
			return values;
		} else
			return null;
	}
}
