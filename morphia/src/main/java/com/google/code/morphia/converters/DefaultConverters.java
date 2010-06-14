/**
 * 
 */
package com.google.code.morphia.converters;

import java.util.LinkedList;
import java.util.List;

import com.google.code.morphia.mapping.MappedField;
import com.google.code.morphia.mapping.Mapper;
import com.google.code.morphia.mapping.MapperOptions;
import com.mongodb.DBObject;

/**
 * implements chain of responsibility for encoders
 * 
 * @author Uwe Schaefer, (us@thomas-daily.de)
 */
@SuppressWarnings("unchecked")
public class DefaultConverters {
	
	private List<TypeConverter> knownEncoders = new LinkedList<TypeConverter>();
	
	// constr. will change
	public DefaultConverters() {
		
		knownEncoders.add(new EnumSetConverter());
		knownEncoders.add(new ObjectIdConverter());
		knownEncoders.add(new EnumConverter());
		knownEncoders.add(new StringConverter());
		knownEncoders.add(new CharacterConverter());
		knownEncoders.add(new ByteConverter());
		knownEncoders.add(new BooleanConverter());
		knownEncoders.add(new DoubleConverter());
		knownEncoders.add(new FloatConverter());
		knownEncoders.add(new LongConverter());
		knownEncoders.add(new LocaleConverter());
		knownEncoders.add(new ShortConverter());
		knownEncoders.add(new IntegerConverter());
		knownEncoders.add(new SerializedObjectConverter());
		knownEncoders.add(new PrimitiveByteArrayConverter());
		knownEncoders.add(new CharArrayConverter());
		knownEncoders.add(new DateConverter());
		knownEncoders.add(new KeyConverter());
		knownEncoders.add(new DBRefConverter());
		knownEncoders.add(new MapOfValuesConverter(this));
		knownEncoders.add(new CollectionConverter(this));
		
		// TODO discuss: maybe a config parameter? last resort
		knownEncoders.add(new PassthroughConverter());
	}
	
	public void fromDBObject(final DBObject dbObj, final MappedField mf, final Object targetEntity) {
		Object object = dbObj.get(mf.getMappedFieldName());
		if (object == null) {
			processMissingField(mf);
		} else {
			TypeConverter enc = getEncoder(mf);
			mf.setFieldValue(targetEntity, enc.decode(mf.getType(), object, mf));
		}
	}
	
	protected void processMissingField(final MappedField mf) {
		//we ignore missing values.
	}
	
	private TypeConverter getEncoder(final MappedField mf) {
		for (TypeConverter e : knownEncoders) {
			if (e.canHandle(mf)) {
				return e;
			}
		}
		throw new ConverterNotFoundException("Cannot find encoder for " + mf.getType() + " as need for "
				+ mf.getFullName());
	}
	
	private TypeConverter getEncoder(final Class c) {
		for (TypeConverter e : knownEncoders) {
			if (e.canHandle(c)) {
				return e;
			}
		}
		throw new ConverterNotFoundException("Cannot find encoder for " + c.getName());
	}
	
	public void toDBObject(final Object containingObject, final MappedField mf, final DBObject dbObj, MapperOptions opts) {
		TypeConverter enc = getEncoder(mf);
		Object fieldValue = mf.getFieldValue(containingObject);
		Object encoded = enc.encode(fieldValue, mf);
		if (encoded != null || opts.storeNulls) {
			dbObj.put(mf.getMappedFieldName(), encoded);
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
		for(TypeConverter tc : knownEncoders)
			tc.setMapper(mapr);
	}
}
