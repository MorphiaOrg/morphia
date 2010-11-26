package com.google.code.morphia.query;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.google.code.morphia.Key;
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
	private static final Logr log = MorphiaLoggerFactory.get(FieldCriteria.class);

	protected final String field;
	protected final FilterOperator operator;
	protected final Object value;
	
	@SuppressWarnings("unchecked")
	protected FieldCriteria(QueryImpl<?> query, String field, FilterOperator op, Object value, boolean validateNames, boolean validateTypes) {
		StringBuffer sb = new StringBuffer(field); //validate might modify prop string to translate java field name to db field name
		MappedField mf = Mapper.validate(query.getEntityClass(), query.getDatastore().getMapper(), sb, op, value, validateNames, validateTypes);
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
				mappedValue = mapr.keyToRef(k);
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
		
		Class<?> type = (mappedValue == null) ?  null : mappedValue.getClass();
		
		//convert single values into lists for $in/$nin
		if (type != null && (op == FilterOperator.IN || op == FilterOperator.NOT_IN) && !type.isArray() && !Iterable.class.isAssignableFrom(type)) {
			mappedValue = Collections.singletonList(mappedValue);
		}
		
		//TODO: investigate and/or add option to control this.
		if (op == FilterOperator.ELEMENT_MATCH && mappedValue instanceof DBObject)
			((DBObject)mappedValue).removeField(Mapper.ID_KEY);
		
		this.field = field;
		this.operator = op;
		this.value = mappedValue;
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

	@Override
	public String toString() {
		return this.field + " " + this.operator.val() + " " + this.value;
	}
}
