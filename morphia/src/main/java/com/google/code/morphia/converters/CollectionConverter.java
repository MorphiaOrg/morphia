/**
 * 
 */
package com.google.code.morphia.converters;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.code.morphia.mapping.MappedField;
import com.google.code.morphia.mapping.MappingException;
import com.google.code.morphia.utils.ReflectionUtils;

/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 */
@SuppressWarnings("unchecked")
public class CollectionConverter extends TypeConverter {
	private final DefaultConverters chain;
	//TODO: should this be Iterable<T>, not a Collection<T> converter?
	
	public CollectionConverter(DefaultConverters chain) {
		this.chain = chain;
	}
	
	@Override
	boolean canHandle(Class c, MappedField optionalExtraInfo) {
		return c.isArray() || ReflectionUtils.isCollection(c);
	}
	
	@Override
	Object decode(Class targetClass, Object fromDBObject, MappedField mf) throws MappingException {
		Collection list = (Collection) fromDBObject;
		if (mf == null)
			return list;
		
		// FIXME we rely on subtype here... is this possible without?

		Class subtype = mf.getSubType();
		if (subtype != null) {
			// map back to the java datatype
			// (List/Set/Array[])
			Collection values = createCollection(mf);
			for (Object o : list) {
				values.add(chain.decode(subtype, o));
			}
			list = values;
		}
		
		if (mf.getType().isArray()) {
			return ReflectionUtils.convertToArray(subtype, list);
		}
		
		return list;

	}
	
	private Collection createCollection(final MappedField mf) {
		Collection values;
		
		if (!mf.isSet()) {
			values = (List) ReflectionUtils.newInstance(mf.getCTor(), ArrayList.class);
		} else {
			values = (Set) ReflectionUtils.newInstance(mf.getCTor(), HashSet.class);
		}
		return values;
	}
	
	@Override
	Object encode(Object value, MappedField f) {
		
		if (value == null)
			return null;
		
		Iterable iterableValues = null;
		
		if (value.getClass().isArray()) {
			
			if (Array.getLength(value) == 0) {
				return value;
			}

			if (value.getClass().getComponentType().isPrimitive())
				return value;
			
			iterableValues = Arrays.asList((Object[]) value);
		} else {
			// cast value to a common interface
			iterableValues = (Iterable) value;
		}
		
		List values = new ArrayList();
		if (f != null && f.getSubType() != null) {
			for (Object o : iterableValues) {
				values.add(chain.encode(f.getSubType(), o));
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
