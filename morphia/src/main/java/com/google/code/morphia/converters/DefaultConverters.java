/**
 * 
 */
package com.google.code.morphia.converters;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import com.google.code.morphia.mapping.MappedField;
import com.google.code.morphia.mapping.Mapper;
import com.google.code.morphia.mapping.MapperOptions;
import com.google.code.morphia.mapping.MappingException;
import com.mongodb.DBObject;

/**
 * implements chain of responsibility for encoders
 * 
 * @author Uwe Schaefer, (us@thomas-daily.de)
 */
@SuppressWarnings({"unchecked","rawtypes"})
public class DefaultConverters {
	private static final Logger log = Logger.getLogger(DefaultConverters.class.getName());
	
	private List<TypeConverter> untypedTypeEncoders = new LinkedList<TypeConverter>();
	private Map<Class,List<TypeConverter>> tcMap = new HashMap<Class,List<TypeConverter>>();
	
	public DefaultConverters() {
		// some converters are commented out since the passthrough converter is enabled.
		
//		addConverter(new PassthroughConverter(ObjectId.class));
//		addConverter(new PassthroughConverter(DBRef.class));
		addConverter(new PassthroughConverter(byte[].class));
		addConverter(new EnumSetConverter());
		addConverter(new EnumConverter());
		addConverter(new StringConverter());
		addConverter(new CharacterConverter());
		addConverter(new ByteConverter());
		addConverter(new BooleanConverter());
		addConverter(new DoubleConverter());
		addConverter(new FloatConverter());
		addConverter(new LongConverter());
		addConverter(new LocaleConverter());
		addConverter(new ShortConverter());
		addConverter(new IntegerConverter());
		addConverter(new SerializedObjectConverter());
		addConverter(new CharArrayConverter());
		addConverter(new DateConverter());
		addConverter(new KeyConverter());
		addConverter(new MapOfValuesConverter(this));
		addConverter(new IterableConverter(this));

		//generic converter that will just pass things through.
		addConverter(new PassthroughConverter());
	}
	
	public void addConverter(TypeConverter tc) {
		if (tc.getSupportedTypes() != null)
			for(Class c : tc.getSupportedTypes())
				addTypedConverter(c, tc);
		else
			untypedTypeEncoders.add(tc);
	}
	
	private void addTypedConverter(Class type, TypeConverter tc) {
		if (tcMap.containsKey(type)) { 
			tcMap.get(type).add(tc);
			log.warning("Added duplicate converter for " + type + " ; " + tcMap.get(type));
		} else {
			ArrayList<TypeConverter> vals = new ArrayList<TypeConverter>();
			vals.add(tc);
			tcMap.put(type, vals);
		}
	}
	public void fromDBObject(final DBObject dbObj, final MappedField mf, final Object targetEntity) {
		Object object = mf.getDbObjectValue(dbObj);
		if (object == null) {
			processMissingField(mf);
		} else {
			TypeConverter enc = getEncoder(mf);
			Object decodedValue = enc.decode(mf.getType(), object, mf);
			try { 
				mf.setFieldValue(targetEntity, decodedValue);
			} catch (IllegalArgumentException e) {
				throw new MappingException("Error setting value from converter (" + 
						enc.getClass().getSimpleName() + ") for " + mf.getFullName() + " to " + decodedValue);
			}
		}
	}
	
	protected void processMissingField(final MappedField mf) {
		//we ignore missing values.
	}
	
	private TypeConverter getEncoder(final MappedField mf) {
		List<TypeConverter> tcs = tcMap.get(mf.getType());
		if(tcs != null) {
			if (tcs.size() > 1)
				log.warning("Duplicate converter for " + mf.getType() + ", returning first one from " + tcs);
			return tcs.get(0);
		}
		
		for (TypeConverter tc : untypedTypeEncoders)
			if(tc.canHandle(mf))
				return tc;
		
		throw new ConverterNotFoundException("Cannot find encoder for " + mf.getType() + " as need for "
				+ mf.getFullName());
	}
	
	private TypeConverter getEncoder(final Class c) {
		List<TypeConverter> tcs = tcMap.get(c);
		if(tcs != null) {
			if (tcs.size() > 1)
				log.warning("Duplicate converter for " + c + ", returning first one from " + tcs);
			return tcs.get(0);
		}
		
		for (TypeConverter tc : untypedTypeEncoders)
			if(tc.canHandle(c))
				return tc;
		
		throw new ConverterNotFoundException("Cannot find encoder for " + c.getName());
	}
	
	public void toDBObject(final Object containingObject, final MappedField mf, final DBObject dbObj, MapperOptions opts) {
		TypeConverter enc = getEncoder(mf);
		Object fieldValue = mf.getFieldValue(containingObject);
		Object encoded = enc.encode(fieldValue, mf);
		if (encoded != null || opts.storeNulls) {
			dbObj.put(mf.getNameToStore(), encoded);
		}
	}
	
	public Object decode(Class c, Object fromDBObject) {
		if (c == null)
			c = fromDBObject.getClass();
		return getEncoder(c).decode(c, fromDBObject);
	}
	
	public Object encode(Object o) {
		return encode(o.getClass(), o);
	}
	
	public Object encode(Class c, Object o) {
		return getEncoder(c).encode(o);
	}

	public void setMapper(Mapper mapr) {
		for(List<TypeConverter> tcs : tcMap.values())
			for(TypeConverter tc : tcs)
				tc.setMapper(mapr);
		for(TypeConverter tc : untypedTypeEncoders)
			tc.setMapper(mapr);
	}
}
