package com.google.code.morphia.query;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import com.google.code.morphia.Key;
import com.google.code.morphia.annotations.Entity;
import com.google.code.morphia.annotations.Reference;
import com.google.code.morphia.annotations.Serialized;
import com.google.code.morphia.logging.Logr;
import com.google.code.morphia.logging.MorphiaLoggerFactory;
import com.google.code.morphia.mapping.MappedClass;
import com.google.code.morphia.mapping.MappedField;
import com.google.code.morphia.mapping.Mapper;
import com.google.code.morphia.mapping.Serializer;
import com.google.code.morphia.utils.ReflectionUtils;
import com.mongodb.DBObject;

public class FieldCriteria extends AbstractCriteria implements Criteria {
	private static final Logr log = MorphiaLoggerFactory.get(Mapper.class);

	private String field;
	private FilterOperator operator;
	private Object value;
	
	@SuppressWarnings("unchecked")
	protected FieldCriteria(QueryImpl<?> query, String field, FilterOperator operator, Object value, boolean validateNames, boolean validateTypes) {
		StringBuffer sb = new StringBuffer(field); //validate might modify prop string to translate java field name to db field name
		MappedField mf = validate(query.getEntityClass(), query.getDatastore().getMapper(), sb, operator, value, validateNames, validateTypes);
		//The field we are filtering on, in the java object; only known if we are validating
		field = sb.toString();

		Mapper mapr = query.getDatastore().getMapper();
		
		Object mappedValue;
		MappedClass mc = null;
		try {
			if (value != null && !ReflectionUtils.isPropertyType(value.getClass()) && !ReflectionUtils.implementsInterface(value.getClass(), Iterable.class))
				if (mf != null && !mf.isTypeMongoCompatible())
					mc = mapr.getMappedClass((mf.isSingleValue()) ? mf.getType() : mf.getSubClass());
				else
					mc = mapr.getMappedClass(value);
		} catch (Exception e) {
			//Ignore these. It is likely they related to mapping validation that is unimportant for queries (the query will fail/return-empty anyway)
			log.debug("Error during mapping of filter criteria: ", e);
		}
	
		//convert the value to Key (DBRef) if it is a entity/@Reference or the field type is Key
		if ((mf!=null && (mf.hasAnnotation(Reference.class) || mf.getType().isAssignableFrom(Key.class)))
				|| (mc != null && mc.getEntityAnnotation() != null)) {
			try {
				Key<?> k = (value instanceof Key) ? (Key<?>)value : query.getDatastore().getKey(value);
				mappedValue = k.toRef(mapr);
			} catch (Exception e) {
				log.debug("Error converting value(" + value + ") to reference.", e);
				mappedValue = mapr.toMongoObject(value);
			}
		}
		else if (mf!=null && mf.hasAnnotation(Serialized.class))
			try {
				mappedValue = Serializer.serialize(value, !mf.getAnnotation(Serialized.class).disableCompression());
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		else if (value instanceof DBObject)
			mappedValue = value;
		else
			mappedValue = mapr.toMongoObject(value);
		
		Class<?> type = (mappedValue != null) ? mappedValue.getClass() : null;
		
		//convert single values into lists for $in/$nin
		if (type != null && (operator == FilterOperator.IN || operator == FilterOperator.NOT_IN) && !type.isArray() && !ReflectionUtils.implementsAnyInterface(type, Iterable.class) ) {
			mappedValue = Collections.singletonList(mappedValue);
		}
		
		this.field = field;
		this.operator = operator;
		this.value = mappedValue;
	}
	
	/** Validate the path, and value type, returning the mappedfield for the field at the path 
	 * @param value2 */
	@SuppressWarnings("rawtypes")
	public static MappedField validate(Class clazz, Mapper mapr, StringBuffer origProp, FilterOperator operator, Object value, boolean validateNames, boolean validateTypes) {
		//TODO: cache validations (in static?).
		
		MappedField mf = null;
		String prop = origProp.toString();
		boolean hasTranslations = false;
		
		if (validateNames) {
			String[] parts = prop.split("\\.");
			if (clazz == null) return null;
			
			MappedClass mc = mapr.getMappedClass(clazz);
			for(int i=0; ; ) {
				String part = parts[i];
				mf = mc.getMappedField(part);
				
				//translate from java field name to stored field name
				if (mf == null) {
					mf = mc.getMappedFieldByJavaField(part);
				    if (mf == null) throw new QueryException("The field '" + part + "' could not be found in '" + clazz.getName() + 
				    										"' while validating - " + prop + 
				    										"; if you wish to continue please disable validation.");
				    hasTranslations = true;
				    parts[i] = mf.getNameToStore();
				}
				
				i++;
				if (mf.isMap()) {
					//skip the map key validation, and move to the next part
					i++;
				}
				
				//catch people trying to search into @Reference/@Serialized fields
				if (i < parts.length && !canQueryPast(mf))
					throw new QueryException("Can not use dot-notation past '" + part + "' could not be found in '" + clazz.getName()+ "' while validating - " + prop);
				
				if (i >= parts.length) break;
				//get the next MappedClass for the next field validation
				mc = mapr.getMappedClass((mf.isSingleValue()) ? mf.getType() : mf.getSubClass());
			}
			
			//record new property string if there has been a translation to any part
			if (hasTranslations) {
    			origProp.setLength(0); // clear existing content
    			origProp.append(parts[0]);
	    		for (int i = 1; i < parts.length; i++) {
	    		    origProp.append('.');
		    	    origProp.append(parts[i]);
    			}
    		}
	
			if (validateTypes)
				if (	 (mf.isSingleValue() && !isCompatibleForCriteria(mf.getType(), operator, value)) || 
						((mf.isMultipleValues() && !isCompatibleForCriteria(mf.getSubClass(), operator, value)))) {
		
					Throwable t = new Throwable();
					StackTraceElement ste = getFirstClientLine(t);
					if (log.isWarningEnabled())
						log.warning("Datatypes for the query may be inconsistent; searching with an instance of "
								+ value.getClass().getName() + " when the field " + mf.getDeclaringClass().getName()+ "." + mf.getJavaFieldName()
								+ " is a " + mf.getType().getName() + (ste == null ? "" : "\r\n --@--" + ste));
					if (log.isDebugEnabled())
						log.debug("Location of warning:\r\n", t);
				}			
		}
		return mf;
	}
	
	@SuppressWarnings("unchecked")
	public void addTo(DBObject obj) {
		if (FilterOperator.EQUAL.equals(this.operator)) {
			obj.put(this.field, this.value); // no operator, prop equals value
			
		} else {
			Object inner = obj.get(this.field); // operator within inner object

			if (!(inner instanceof Map)) {
				inner = new HashMap<String, Object>();
				obj.put(this.field, inner);
			}
			
			((Map<String, Object>)inner).put(this.operator.val(), this.value);
		}
	}

	/** Returns if the MappedField is a Reference or Serilized  */
	protected static boolean canQueryPast(MappedField mf) {
		return !(mf.hasAnnotation(Reference.class) || mf.hasAnnotation(Serialized.class));
	}

	protected static boolean isCompatibleForCriteria(Class<?> type, FilterOperator operator, Object value) {
		if (value == null || type == null) 
			return true;
		else if (operator.equals(FilterOperator.EXISTS) && (value instanceof Boolean))
			return true;
		else if (operator.equals(FilterOperator.IN) && (value.getClass().isArray() || ReflectionUtils.implementsAnyInterface(value.getClass(), Iterable.class, Map.class)))
			return true;
		else if (operator.equals(FilterOperator.NOT_IN) && (value.getClass().isArray() || ReflectionUtils.implementsAnyInterface(value.getClass(), Iterable.class, Map.class)))
			return true;
		else if (operator.equals(FilterOperator.ALL) && (value.getClass().isArray() || ReflectionUtils.implementsAnyInterface(value.getClass(), Iterable.class, Map.class)))
			return true;
		else if (value instanceof Integer && (int.class.equals(type) || long.class.equals(type) || Long.class.equals(type)))
			return true;
		else if ((value instanceof Integer || value instanceof Long) && (double.class.equals(type) || Double.class.equals(type)))
			return true;
		else if (value instanceof Pattern && String.class.equals(type))
			return true;
		else if (value.getClass().getAnnotation(Entity.class) != null && Key.class.equals(type))
			return true;
		else if (value instanceof List<?>)
			return true;
		else if (!value.getClass().isAssignableFrom(type) &&
				//hack to let Long match long, and so on
				!value.getClass().getSimpleName().toLowerCase().equals(type.getSimpleName().toLowerCase())) {
			return false;
		}
		return true;
	}
	
	/** Return the first {@link StackTraceElement} not in our code (package). */
	protected static StackTraceElement getFirstClientLine(Throwable t) {
		for(StackTraceElement ste : t.getStackTrace())
			if ( 	!ste.getClassName().startsWith("com.google.code.morphia") && 
					!ste.getClassName().startsWith("sun.reflect") && 
					!ste.getClassName().startsWith("org.junit") && 
					!ste.getClassName().startsWith("org.eclipse") && 
					!ste.getClassName().startsWith("java.lang"))
				return ste;
		
		return null;
	}
	
	@Override
	public String toString() {
		return this.field + " = " + this.value;
	}
}
