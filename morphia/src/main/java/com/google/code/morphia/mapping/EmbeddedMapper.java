/**
 * 
 */
package com.google.code.morphia.mapping;

import java.lang.reflect.Modifier;
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
			//run converters
			if (mapr.converters.hasDbObjectConverter(mf) || mapr.converters.hasDbObjectConverter(entity.getClass())) {
				mapr.converters.toDBObject(entity, mf, dbObject, mapr.getOptions());
				return;
			}

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

	private void writeCollection(final MappedField mf, final BasicDBObject dbObject, Map<Object, DBObject> involvedObjects, String name, Object fieldValue, Mapper mapr) {
		Iterable coll = (Iterable) fieldValue;
		if (coll != null) {
			List values = new ArrayList();
			for (Object o : coll) {
				if (mapr.converters.hasSimpleValueConverter(mf) || mapr.converters.hasSimpleValueConverter(o.getClass()))
					values.add(mapr.converters.encode(o));
				else {
					DBObject dbObj = mapr.toDBObject(o, involvedObjects);
					if (	!mf.getSubType().isInterface() && !Modifier.isAbstract(mf.getSubType().getModifiers()) && 
							mf.getSubType().equals(o.getClass()) && !(dbObj instanceof BasicDBList)) {
						dbObj.removeField(Mapper.CLASS_NAME_FIELDNAME);
					}
					values.add(dbObj);
				}
			}
			if (values.size() > 0 || mapr.getOptions().storeEmpties) {
				dbObject.put(name, values);
			}
		}
	}

	private void writeMap(final MappedField mf, final BasicDBObject dbObject, Map<Object, DBObject> involvedObjects, String name, Object fieldValue, Mapper mapr) {
		Map<String, Object> map = (Map<String, Object>) fieldValue;
		if (map != null) {
			BasicDBObject values = new BasicDBObject();
			
			for (Map.Entry<String, Object> entry : map.entrySet()) {
				Object entryVal = entry.getValue();
				Object val;
				
				if (entryVal == null)
					val = null;
				else if(mapr.converters.hasSimpleValueConverter(mf) || mapr.converters.hasSimpleValueConverter(entryVal.getClass()))
					val = mapr.converters.encode(entryVal);
				else {
					val = mapr.toDBObject(entryVal, involvedObjects);
				
					if (	!mf.getSubType().isInterface() && !Modifier.isAbstract(mf.getSubType().getModifiers()) && 
							!(val instanceof BasicDBList) && mf.getSubType().equals(entryVal.getClass()))
						((DBObject)val).removeField(Mapper.CLASS_NAME_FIELDNAME);
				}
				
				String strKey = mapr.converters.encode(entry.getKey()).toString();
				values.put(strKey, val);
			}
			
			if (values.size() > 0 || mapr.getOptions().storeEmpties)
				dbObject.put(name, values);
		}
	}
	
	public void fromDBObject(final DBObject dbObject, final MappedField mf, final Object entity, EntityCache cache, Mapper mapr) {
		try {
			if (mf.isMap()) {
				readMap(dbObject, mf, entity, cache, mapr);
			} else if (mf.isMultipleValues()) {
				readCollection(dbObject, mf, entity, cache, mapr);
			} else {
				// single element
				
				Object dbVal = mf.getDbObjectValue(dbObject);
				if (dbVal != null) {
					boolean isDBObject = dbVal instanceof DBObject;
					
					//run converters						
					if (isDBObject && (mapr.converters.hasDbObjectConverter(mf) || mapr.converters.hasDbObjectConverter(mf.getType()))) {
						mapr.converters.fromDBObject(((DBObject)dbVal), mf, entity);
						return;
					} else {
						Object refObj = null;
						if (mapr.converters.hasSimpleValueConverter(mf) || mapr.converters.hasSimpleValueConverter(mf.getType()))
							refObj = mapr.converters.decode(mf.getType(), dbVal, mf);
						else {
							refObj = ReflectionUtils.createInstance(mf, ((DBObject)dbVal));
							refObj = mapr.fromDb(((DBObject)dbVal), refObj, cache);
						}
						if (refObj != null) {
							mf.setFieldValue(entity, refObj);
						}
					}
				}
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private void readCollection(final DBObject dbObject, final MappedField mf, final Object entity, EntityCache cache, Mapper mapr) {
		// multiple documents in a List
		Class newEntityType = mf.getSubType();
		Collection values = (Collection) ReflectionUtils.newInstance(mf.getCTor(), (!mf.isSet()) ? ArrayList.class : HashSet.class);
		
		Object dbVal = mf.getDbObjectValue(dbObject);
		if (dbVal != null) {
			
			List<BasicDBObject> dbVals = (dbVal instanceof List) ? (List<BasicDBObject>) dbVal : Collections.singletonList((BasicDBObject) dbVal);
			
			for (BasicDBObject dbObj : dbVals) {
				Object newEntity;
				
				//run converters
				if (mapr.converters.hasSimpleValueConverter(mf) || mapr.converters.hasSimpleValueConverter(mf.getSubType()))
					newEntity = mapr.converters.decode(mf.getSubType(), dbObj, mf);
				else {
					newEntity = ReflectionUtils.createInstance(newEntityType, dbObj);
					newEntity = mapr.fromDb(dbObj, newEntity, cache);
				}
				
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

	private void readMap(final DBObject dbObject, final MappedField mf, final Object entity, EntityCache cache, Mapper mapr) {
		Map map = (Map) ReflectionUtils.newInstance(mf.getCTor(), HashMap.class);
		
		BasicDBObject dbVal = (BasicDBObject) mf.getDbObjectValue(dbObject);
		if (dbVal != null) {
			Class newEntityType = mf.getSubType();
			for (Map.Entry entry : dbVal.entrySet()) {
				Object val = entry.getValue();
				Object newEntity = null;
				
				//run converters
				if (val != null) {
					if (	mapr.converters.hasSimpleValueConverter(mf) || 
							mapr.converters.hasSimpleValueConverter(mf.getSubType()))
						newEntity = mapr.converters.decode(mf.getSubType(), val, mf);
					else {
						
						if (!(val instanceof DBObject))
							throw new MappingException("Embedded element isn't a DBObject! -- " + val.getClass());
						
						DBObject dbObj = (DBObject) val;
						newEntity = ReflectionUtils.createInstance(newEntityType, dbObj);
						newEntity = mapr.fromDb(dbObj, newEntity, cache);
					}
				}

				Object objKey = mapr.converters.decode(mf.getMapKeyType(), entry.getKey());
				map.put(objKey, newEntity);
			}
		}
		
		if (map.size() > 0) {
			mf.setFieldValue(entity, map);
		}
	}
}
