/**
 * 
 */
package com.google.code.morphia.mapping;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import com.google.code.morphia.mapping.cache.EntityCache;
import com.google.code.morphia.utils.ReflectionUtils;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

@SuppressWarnings({"unchecked","rawtypes"})
class EmbeddedMapper implements CustomMapper{
	public void toDBObject(final Object entity, final MappedField mf, final BasicDBObject dbObject,
			Map<Object, DBObject> involvedObjects, Mapper mapr) {
		String name = mf.getNameToStore();
		
		Object fieldValue = mf.getFieldValue(entity);
		
		if (mf.isMap()) {
			writeMap(mf, dbObject, involvedObjects, name, fieldValue, mapr);
		} else if (mf.isMultipleValues()) {
			writeCollection(mf, dbObject, involvedObjects, name, fieldValue, mapr);
		} else {
			DBObject dbObj = fieldValue == null ? null : mapr.toDBObject(fieldValue, involvedObjects);
			if (dbObj != null) {
				
				if (mf.getType().equals(fieldValue.getClass()) && !(dbObj instanceof BasicDBList))
					dbObj.removeField(Mapper.CLASS_NAME_FIELDNAME);
				
				if (dbObj.keySet().size() > 0 || mapr.getOptions().storeEmpties) {
					dbObject.put(name, dbObj);
				}
			}
		}
	}

	private void writeCollection(final MappedField mf, final BasicDBObject dbObject,
			Map<Object, DBObject> involvedObjects, String name, Object fieldValue, Mapper mapr) {
		Iterable coll = (Iterable) fieldValue;
		if (coll != null) {
			List values = new ArrayList();
			for (Object o : coll) {
				DBObject dbObj = mapr.toDBObject(o, involvedObjects);
				if (mf.getSubType().equals(o.getClass()) && !(dbObj instanceof BasicDBList)) {
					dbObj.removeField(Mapper.CLASS_NAME_FIELDNAME);
				}
				values.add(dbObj);
			}
			if (values.size() > 0 || mapr.getOptions().storeEmpties) {
				dbObject.put(name, values);
			}
		}
	}

	private void writeMap(final MappedField mf, final BasicDBObject dbObject,
			Map<Object, DBObject> involvedObjects, String name, Object fieldValue, Mapper mapr) {
		Map<String, Object> map = (Map<String, Object>) fieldValue;
		if (map != null) {
			BasicDBObject values = new BasicDBObject();
			for (Map.Entry<String, Object> entry : map.entrySet()) {
				Object entryVal = entry.getValue();
				DBObject convertedVal = mapr.toDBObject(entryVal, involvedObjects);
				
				if (mf.getSubType().equals(entryVal.getClass()) && !(convertedVal instanceof BasicDBList)) {
					convertedVal.removeField(Mapper.CLASS_NAME_FIELDNAME);
				}
				
				String strKey = mapr.converters.encode(entry.getKey()).toString();
				values.put(strKey, convertedVal);
			}
			if (values.size() > 0 || mapr.getOptions().storeEmpties) {
				dbObject.put(name, values);
			}
		}
	}
	
	public void fromDBObject(final DBObject dbObject, final MappedField mf, final Object entity, EntityCache cache, Mapper mapr) {
		try {
			if (mf.isMap()) {
				readMap(dbObject, mf, entity, cache, mapr);
			} else if (mf.isMultipleValues()) {
				readCollection(dbObject, mf, entity, cache, mapr);
			} else {
				// single document
				BasicDBObject dbVal = (BasicDBObject) mf.getDbObjectValue(dbObject);
				if (dbVal != null) {
					Object refObj = ReflectionUtils.createInstance(mf, dbVal);
					refObj = mapr.fromDb(dbVal, refObj, cache);
					if (refObj != null) {
						mf.setFieldValue(entity, refObj);
					}
				}
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private void readCollection(final DBObject dbObject, final MappedField mf, final Object entity,
 EntityCache cache, Mapper mapr) {
		// multiple documents in a List
		Class newEntityType = mf.getSubType();
		Collection values = (Collection) ReflectionUtils.newInstance(mf.getCTor(), (!mf.isSet()) ? ArrayList.class : HashSet.class);
		
		Object dbVal = mf.getDbObjectValue(dbObject);
		if (dbVal != null) {
			
			List<BasicDBObject> dbVals = (dbVal instanceof List) ? (List<BasicDBObject>) dbVal : Collections.singletonList((BasicDBObject) dbVal);
			
			for (BasicDBObject dbObj : dbVals) {
				Object newEntity = ReflectionUtils.createInstance(newEntityType, dbObj);
				newEntity = mapr.fromDb(dbObj, newEntity, cache);
				values.add(newEntity);
			}
		}
		if (values.size() > 0) {
			if (mf.getType().isArray()) {
				Object[] array = ReflectionUtils.convertToArray(mf.getSubType(), ReflectionUtils.iterToList(values));
				mf.setFieldValue(entity, array);
			} else {
				mf.setFieldValue(entity, values);
			}
		}
	}

	private void readMap(final DBObject dbObject, final MappedField mf, final Object entity,
 EntityCache cache, Mapper mapr) {
		Map map = (Map) ReflectionUtils.newInstance(mf.getCTor(), HashMap.class);
		
		BasicDBObject dbVal = (BasicDBObject) mf.getDbObjectValue(dbObject);
		if (dbVal != null) {
			for (Map.Entry entry : dbVal.entrySet()) {
				Object newEntity = ReflectionUtils.createInstance(mf.getSubType(), (BasicDBObject) entry.getValue());
				
				newEntity = mapr.fromDb((BasicDBObject) entry.getValue(), newEntity, cache);
				Object objKey = mapr.converters.decode(mf.getMapKeyType(), entry.getKey());
				map.put(objKey, newEntity);
			}
		}
		
		if (map.size() > 0) {
			mf.setFieldValue(entity, map);
		}
	}
}
