/**
 * 
 */
package com.google.code.morphia.mapping;

import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Collection;
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
					Object val;
					if (ReflectionUtils.implementsAnyInterface(o.getClass(), Collection.class, Map.class))
						val = mapr.toMongoObject(o, true);
					else
						val = mapr.toDBObject(o, involvedObjects);

					if (	val != null && val instanceof DBObject && 
							!ReflectionUtils.implementsAnyInterface(val.getClass(), Collection.class, Map.class) && 
							!mf.getSubClass().isInterface() && !Modifier.isAbstract(mf.getSubClass().getModifiers()) && 
							mf.getSubClass().equals(o.getClass())) {
						((DBObject) val).removeField(Mapper.CLASS_NAME_FIELDNAME);
					}
					values.add(val);
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
					if (ReflectionUtils.implementsAnyInterface(entryVal.getClass(), Collection.class, Map.class))
						val = mapr.toMongoObject(entryVal, true);
					else
						val = mapr.toDBObject(entryVal, involvedObjects);
				
					if (	val != null && val instanceof DBObject && 
							!ReflectionUtils.implementsAnyInterface(val.getClass(), Collection.class, Map.class) && 
							!mf.getSubClass().isInterface() && !Modifier.isAbstract(mf.getSubClass().getModifiers()) && 
							mf.getSubClass().equals(entryVal.getClass()))
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
		Collection values = (Collection) ReflectionUtils.newInstance(mf.getCTor(), (!mf.isSet()) ? ArrayList.class : HashSet.class);
		
		Object dbVal = mf.getDbObjectValue(dbObject);
		if (dbVal != null) {
			
			List dbVals = null;
			if (dbVal instanceof List)
				dbVals = (List) dbVal;
			else {
				dbVals = new BasicDBList();
				dbVals.add(dbVal);
			}
			
			for (Object o : dbVals) {
				
				DBObject dbObj = (DBObject) o;
				Object newEntity = null;
				
				if (dbObj != null) {
					//run converters
					if (mapr.converters.hasSimpleValueConverter(mf) || mapr.converters.hasSimpleValueConverter(mf.getSubClass()))
						newEntity = mapr.converters.decode(mf.getSubClass(), dbObj, mf);
					else {
						newEntity = readMapOrCollectionOrEntity(dbObj, mf, cache, mapr);
					}
				}
				
				values.add(newEntity);
			}
		}
		if (values.size() > 0) {
			if (mf.getType().isArray()) {
				Object[] array = ReflectionUtils.convertToArray(mf.getSubClass(), ReflectionUtils.iterToList(values));
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
			for (Map.Entry entry : dbVal.entrySet()) {
				Object val = entry.getValue();
				Object newEntity = null;
				
				//run converters
				if (val != null) {
					if (	mapr.converters.hasSimpleValueConverter(mf) || 
							mapr.converters.hasSimpleValueConverter(mf.getSubClass()))
						newEntity = mapr.converters.decode(mf.getSubClass(), val, mf);
					else {
						if(val instanceof DBObject)
							newEntity = readMapOrCollectionOrEntity((DBObject) val, mf, cache, mapr);
						else
							throw new MappingException("Embedded element isn't a DBObject! How can it be that is a " + val.getClass());

					}
				}

				Object objKey = mapr.converters.decode(mf.getMapKeyClass(), entry.getKey());
				map.put(objKey, newEntity);
			}
		}
		
		if (map.size() > 0) {
			mf.setFieldValue(entity, map);
		}
	}

	private Object readMapOrCollectionOrEntity(DBObject dbObj, MappedField mf, EntityCache cache, Mapper mapr) {
		if(ReflectionUtils.implementsAnyInterface(mf.getSubClass(), Iterable.class, Map.class)) {
			MapOrCollectionMF mocMF = new MapOrCollectionMF((ParameterizedType)mf.getSubType());
			mapr.fromDb(dbObj, mocMF, cache);
//			if(mocMF.isMap())
//				readMap(dbObj, mocMF, mocMF, cache, mapr);
//			else
//				readCollection(dbObj, mocMF, mocMF, cache, mapr);
			return mocMF.getValue();
		} else {
			Object newEntity = ReflectionUtils.createInstance(mf.getSubClass(), dbObj);
			return mapr.fromDb(dbObj, newEntity, cache);
		}
	} 

}
