package com.google.code.morphia.mapping;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.code.morphia.annotations.AlsoLoad;
import com.google.code.morphia.annotations.Embedded;
import com.google.code.morphia.annotations.Id;
import com.google.code.morphia.annotations.Indexed;
import com.google.code.morphia.annotations.Property;
import com.google.code.morphia.annotations.Reference;
import com.google.code.morphia.annotations.Serialized;
import com.google.code.morphia.annotations.Version;
import com.google.code.morphia.utils.ReflectionUtils;

/**
 * Represents the mapping of this field to/from mongodb (name, annotations)
 * 
 * @author Scott Hernandez
 */
@SuppressWarnings("unchecked")
public class MappedField {
	static final Logger log = Logger.getLogger(MappedField.class.getName());
	
	// the field :)
	private Field field;
	// the constructor for the type
	private Constructor ctor;
	// the name to store in mongodb {name:value}
	private String name;
	
	// Annotations that have been found relevent to mapping
	protected Map<Class<? extends Annotation>, Annotation> mappingAnnotations = new HashMap<Class<? extends Annotation>, Annotation>();
	// The Annotations to look for when reflecting on the field (stored in the
	// mappingAnnotations)
	public static List<Class<? extends Annotation>> interestingAnnotations = new ArrayList<Class<? extends Annotation>>(
			Arrays.asList(Serialized.class, Indexed.class, Property.class, Reference.class, Embedded.class, Id.class,
					Version.class, AlsoLoad.class));
	
	// the type (T) for the Collection<T>/T[]/Map<?,T>
	private Class subType = null;
	// the type (T) for the Map<T,?>
	private Class keyType = null;
	
	// indicates the field is a single value
	private boolean isSingleValue = true;
	// indicated the type is a mongo compatible type (our version of value-type)
	private boolean isMongoType = false;
	// indicated if it implements Map interface
	private boolean isMap = false;
	// indicated if the collection is a set (or list)
	private boolean isSet = false;
	
	public MappedField(Field f) {
		f.setAccessible(true);
		field = f;
		
		for (Class<? extends Annotation> clazz : interestingAnnotations)
			addAnnotation(clazz);
		
		Class ctorType = null;
		// get the first annotation with a concreteClass that isn't Object.class
		for (Annotation an : mappingAnnotations.values()) {
			try {
				Method m = an.getClass().getMethod("concreteClass");
				m.setAccessible(true);
				Object o = m.invoke(an);
				if (o != null && !(o.equals(Object.class))) {
					ctorType = (Class) o;
					break;
				}
			} catch (NoSuchMethodException e) {
				// do nothing
			} catch (IllegalArgumentException e) {
				log.log(Level.WARNING, "There should not be an argument", e);
			} catch (Exception e) {
				log.log(Level.WARNING, "", e);
			}
		}
		
		if (ctorType != null)
			try {
				ctor = ctorType.getDeclaredConstructor();
				ctor.setAccessible(true);
			} catch (NoSuchMethodException e) {
				throw new MappingException("No usable constructor for " + field.getType().getName(), e);
			}
		else {
			// see if we can create intances of the type used for declaration
			
			try {
				ctor = field.getType().getDeclaredConstructor();
				ctor.setAccessible(true);
			} catch (NoSuchMethodException e) {
				// never mind.
			}
		}
		
		this.name = getMappedFieldName();
		Class type = f.getType();
		if (type.isArray()
				|| ReflectionUtils.implementsAnyInterface(field.getType(), Iterable.class, Collection.class,
						List.class, Set.class, Map.class)) {
			isSingleValue = false;
			// subtype of Long[], List<Long> is Long
			isMap = ReflectionUtils.implementsInterface(type, Map.class);
			isSet = ReflectionUtils.implementsInterface(type, Set.class);
			
			// get the subtype T, T[]/List<T>/Map<?,T>
			subType = (type.isArray()) ? type.getComponentType() : ReflectionUtils.getParameterizedClass(f, (isMap) ? 1
					: 0);
			if (isMap)
				keyType = ReflectionUtils.getParameterizedClass(f, 0);
		}
		
		// check the main type
		isMongoType = ReflectionUtils.isPropertyType(type);
		
		// if the main type isn't supported by the Mongo, see if the subtype is
		// works for Long[], List<Long>, Map<?, Long>etc.
		if (!isMongoType && subType != null)
			isMongoType = ReflectionUtils.isPropertyType(subType);
		
		// TODO isn´t that actual validation?
		
		if (!isMongoType && !isSingleValue && (subType == null || subType.equals(Object.class))) {
			log
					.warning("The multi-valued field '"
							+ getFullName()
							+ "' is a possible heterogenous collection. It cannot be verified. Please declare a valid type to get rid of this warning.");
			isMongoType = true;
		}
	}
	
	/** Returns the name of the field's (key)name for mongodb */
	public String getNameToStore() {
		return name;
	}
	/** Returns the name of the field's (key)name for mongodb */
	public List<String> getLoadNames() {
		ArrayList<String> names = new ArrayList<String>();
		names.add(name);
		
		AlsoLoad al = (AlsoLoad)this.mappingAnnotations.get(AlsoLoad.class);
		if (al != null && al.value() != null && al.value().length > 0)
			names.addAll( Arrays.asList(al.value()));
		
		return names;
	}
	
	/** Returns the name of the field, as declared on the class */
	public String getClassFieldName() {
		return field.getName();
	}
	
	public <T extends Annotation> T getAnnotation(Class<T> clazz) {
		return (T) mappingAnnotations.get(clazz);
	}
	
	public boolean hasAnnotation(Class ann) {
		return mappingAnnotations.containsKey(ann);
	}
	
	/**
	 * Adds the annotation, if it exists on the field.
	 * 
	 * @param clazz
	 */
	public void addAnnotation(Class<? extends Annotation> clazz) {
		if (field.isAnnotationPresent(clazz))
			this.mappingAnnotations.put(clazz, field.getAnnotation(clazz));
	}
	
	public void validate() {
		// moved to ContradictingFieldAnnotation, EmbeddedAndSerializable,
		// MapKeyDifferentFromString (this is CHANGED A BIT)
	}
	
	public String getFullName() {
		return field.getDeclaringClass().getName() + "." + field.getName();
	}
	
	/**
	 * Returns the name of the field's key-name for mongodb
	 */
	public String getMappedFieldName() {
		if (hasAnnotation(Property.class)) {
			Property mv = (Property) mappingAnnotations.get(Property.class);
			if (!mv.value().equals(Mapper.IGNORED_FIELDNAME))
				return mv.value();
		} else if (hasAnnotation(Reference.class)) {
			Reference mr = (Reference) mappingAnnotations.get(Reference.class);
			if (!mr.value().equals(Mapper.IGNORED_FIELDNAME))
				return mr.value();
		} else if (hasAnnotation(Embedded.class)) {
			Embedded me = (Embedded) mappingAnnotations.get(Embedded.class);
			if (!me.value().equals(Mapper.IGNORED_FIELDNAME))
				return me.value();
		} else if (hasAnnotation(Serialized.class)) {
			Serialized me = (Serialized) mappingAnnotations.get(Serialized.class);
			if (!me.value().equals(Mapper.IGNORED_FIELDNAME))
				return me.value();
		}
		return this.field.getName();
	}
	
	@Override
	public String toString() {
		return name + "; " + this.mappingAnnotations.toString();
	}
	
	public Class getType() {
		return field.getType();
	}
	
	public Class getMapKeyType() {
		return keyType;
	}
	
	public Class getDeclaringClass() {
		return field.getDeclaringClass();
	}
	
	public Class getSubType() {
		return subType;
	}
	
	public boolean isSingleValue() {
		return isSingleValue;
	}
	
	public boolean isMultipleValues() {
		return !isSingleValue;
	}
	
	public boolean isTypeMongoCompatible() {
		return isMongoType;
	}
	
	public boolean isMap() {
		return isMap;
	}
	
	public boolean isSet() {
		return isSet;
	}
	
	public Constructor getCTor() {
		return ctor;
	}
	
	// every time this is called, the error was just wrapped in a RTE, so that i
	// took the liberty of doing the try/catch inside
	public Object getFieldValue(Object classInst) throws IllegalArgumentException {
		try {
			field.setAccessible(true);
			return field.get(classInst);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}
	
	public void setFieldValue(Object classInst, Object value) throws IllegalArgumentException {
		try {
			field.setAccessible(true);
			field.set(classInst, value);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}
	
	public Field getField() {
		return field;
	}
	
}