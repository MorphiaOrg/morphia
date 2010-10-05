/**
 * Copyright (C) 2010 Olafur Gauti Gudmundsson Licensed under the Apache
 * License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law
 * or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package com.google.code.morphia.mapping;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.google.code.morphia.EntityInterceptor;
import com.google.code.morphia.Key;
import com.google.code.morphia.annotations.Embedded;
import com.google.code.morphia.annotations.Id;
import com.google.code.morphia.annotations.NotSaved;
import com.google.code.morphia.annotations.PostLoad;
import com.google.code.morphia.annotations.PreLoad;
import com.google.code.morphia.annotations.PrePersist;
import com.google.code.morphia.annotations.PreSave;
import com.google.code.morphia.annotations.Property;
import com.google.code.morphia.annotations.Reference;
import com.google.code.morphia.annotations.Serialized;
import com.google.code.morphia.converters.DefaultConverters;
import com.google.code.morphia.logging.Logr;
import com.google.code.morphia.logging.MorphiaLoggerFactory;
import com.google.code.morphia.mapping.cache.DefaultEntityCache;
import com.google.code.morphia.mapping.cache.EntityCache;
import com.google.code.morphia.mapping.lazy.DatastoreProvider;
import com.google.code.morphia.mapping.lazy.DefaultDatastoreProvider;
import com.google.code.morphia.mapping.lazy.LazyFeatureDependencies;
import com.google.code.morphia.mapping.lazy.LazyProxyFactory;
import com.google.code.morphia.mapping.lazy.proxy.ProxyHelper;
import com.google.code.morphia.utils.ReflectionUtils;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

/**
 * 
 * <p>This is the heart of Morphia and takes care of mapping from/to POJOs/DBObjects<p>
 * <p>This class is thread-safe and keeps various "cached" data which should speed up processing.</p>
 * 
 * @author Olafur Gauti Gudmundsson
 * @author Scott Hernandez
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class Mapper {

	public static final Logr logger = MorphiaLoggerFactory.get(Mapper.class);

	/** The @{@link Id} field name that is stored with mongodb.*/
	public static final String ID_KEY = "_id";
	/** Special name that can never be used. Used as default for some fields to indicate default state.*/
	public static final String IGNORED_FIELDNAME = ".";
	/** Special field used by morphia to support various possibly loading issues; will be replaced when discriminators are implemented to support polymorphism*/
	public static final String CLASS_NAME_FIELDNAME = "className";

	/** Set of classes that registered by this mapper */
	private final ConcurrentHashMap<String, MappedClass> mappedClasses = new ConcurrentHashMap<String, MappedClass>();
	private final ConcurrentLinkedQueue<EntityInterceptor> interceptors = new ConcurrentLinkedQueue<EntityInterceptor>();
	
	private MapperOptions opts = new MapperOptions();
	// TODO: make these configurable
	LazyProxyFactory proxyFactory = LazyFeatureDependencies.createDefaultProxyFactory();
	DatastoreProvider datastoreProvider = new DefaultDatastoreProvider();
	DefaultConverters converters = new DefaultConverters();;

	public Mapper() {
		converters.setMapper(this);
	}

	public Mapper(MapperOptions opts) {
		converters.setMapper(this);
		this.opts = opts;
	}

	/**
	 * <p>
	 * Adds an {@link EntityInterceptor}
	 * </p>
	 */
	public void addInterceptor(final EntityInterceptor ei) {
		interceptors.add(ei);
	}
	
	/**
	 * <p>
	 * Gets list of {@link EntityInterceptor}s
	 * </p>
	 */
	public Collection<EntityInterceptor> getInterceptors() {
		return interceptors;
	}

	public MapperOptions getOptions() {
		return this.opts;
	}

	public void setOptions(MapperOptions options) {
		this.opts = options;
	}

	public boolean isMapped(final Class c) {
		return mappedClasses.containsKey(c.getName());
	}

	public void addMappedClass(final Class c) {
		MappedClass mc = new MappedClass(c, this);
		mc.validate();
		mappedClasses.put(c.getName(), mc);
	}

	public MappedClass addMappedClass(final MappedClass mc) {
		mc.validate();
		mappedClasses.put(mc.getClazz().getName(), mc);
		return mc;
	}

	public Map<String, MappedClass> getMappedClasses() {
		return mappedClasses;
	}

	/**
	 * <p>
	 * Gets the {@link MappedClass} for the object (type). If it isn't mapped,
	 * create a new class and cache it (without validating).
	 * </p>
	 */
	public MappedClass getMappedClass(final Object obj) {
		if (obj == null) {
			return null;
		}
		
		Class type = (obj instanceof Class) ? (Class) obj : obj.getClass();
		if (ProxyHelper.isProxy(obj)) {
			type = ProxyHelper.getReferentClass(obj);
		}

		MappedClass mc = mappedClasses.get(type.getName());
		if (mc == null) {
			// no validation
			mc = new MappedClass(type, this);
			mappedClasses.put(mc.getClazz().getName(), mc);
		}
		return mc;
	}

	public String getCollectionName(Object object) {
		MappedClass mc = getMappedClass(object);
		return mc.getCollectionName();
	}
	
	/**
	 *<p>
	 * Updates the @{@link Id} fields.
	 * </p>
	 * 
	 * @param entity
	 *            The object to update
	 * @param dbObj
	 *            Value to update with; null means skip
	 */
	public void updateKeyInfo(final Object entity, final DBObject dbObj, EntityCache cache) {
		MappedClass mc = getMappedClass(entity);

		// update id field, if there.		
		if ((mc.getIdField() != null) && (dbObj != null) && (dbObj.get(ID_KEY) != null)) {
			try {
				MappedField mf = mc.getMappedField(ID_KEY);
				Object oldIdValue = mc.getIdField().get(entity);
				setIdValue(entity, mf, dbObj, cache);
				Object dbIdValue = mc.getIdField().get(entity);
				if (oldIdValue != null) {
					// The entity already had an id set. Check to make sure it
					// hasn't changed. That would be unexpected, and could
					// indicate a bad state.
					if (!dbIdValue.equals(oldIdValue)) {
						mf.setFieldValue(entity, oldIdValue);//put the value back...
						
						throw new RuntimeException("@Id mismatch: " + oldIdValue + " != " + dbIdValue + " for "
								+ entity.getClass().getName());
					}
				} else {
					mc.getIdField().set(entity, dbIdValue);
				}
			} catch (Exception e) {
				if (e.getClass().equals(RuntimeException.class)) {
					throw (RuntimeException) e;
				}

				throw new RuntimeException("Error setting @Id field after save/insert.", e);
			}
		}
	}

	/** Converts a DBObject back to a type-safe java object (POJO)
	 * @param entityClass The type to return, or use; can be overridden by the @see Mapper.CLASS_NAME_FIELDNAME in the DBObject
	 **/
	public Object fromDBObject(final Class entityClass, final DBObject dbObject, EntityCache cache) {
		if (dbObject == null) {
			Throwable t = new Throwable();
			logger.error("Somebody passed in a null dbObject; bad client!", t);
			return null;
		}

		Object entity = null;
		entity = ReflectionUtils.createInstance(entityClass, dbObject);
		entity = fromDb(dbObject, entity, cache);
		return entity;
	}

	/**
	 * <p>
	 * Converts a java object to a mongo-compatible object (possibly a DBObject
	 * for complex mappings). Very similar to {@link Mapper.toDBObject}
	 * </p>
	 * <p>
	 * Used (mainly) by query/update operations
	 * </p>
	 */
	public Object toMongoObject(final Object javaObj) {
		if (javaObj == null) {
			return null;
		}
		Class origClass = javaObj.getClass();
		Object newObj = converters.encode(origClass, javaObj);
		if (newObj == null) {
			logger.warning("converted " + javaObj + " to null");
			return newObj;
		}
		Class type = newObj.getClass();
		boolean bSameType = origClass.equals(type);
		
		//TODO: think about this logic a bit more. 
		//Even if the converter changed it, should it still be processed?
		
		
		//The converter ran, and produced another type.
		if (!bSameType)
			return newObj;
		else {
			
			boolean isSingleValue = true;
			boolean isMap = false;
			Class subType = null;
	
			if (type.isArray() || ReflectionUtils.implementsAnyInterface(type, Iterable.class, Map.class)) {
				isSingleValue = false;
				isMap = ReflectionUtils.implementsInterface(type, Map.class);
				// subtype of Long[], List<Long> is Long
				subType = (type.isArray()) ? type.getComponentType() : ReflectionUtils.getParameterizedClass(type, (isMap) ? 1 : 0);
			}
	
			if (isSingleValue && !ReflectionUtils.isPropertyType(type)) {
				DBObject dbObj = toDBObject(newObj);
				dbObj.removeField(CLASS_NAME_FIELDNAME);
				return dbObj;
			} else if (newObj instanceof DBObject) {
				return newObj;
			} else if (isMap) {
				if (ReflectionUtils.isPropertyType(subType))
					return toDBObject(newObj);
				else {
					HashMap m = new HashMap();
					for(Map.Entry e : (Iterable<Map.Entry>)((Map)newObj).entrySet())
						m.put(e.getKey(), toMongoObject(e.getValue()));

					return toDBObject(m);
				}
			//Set/List but needs elements converted
			} else if (!isSingleValue && !ReflectionUtils.isPropertyType(subType)) {
				ArrayList<Object> vals = new ArrayList<Object>();
				if (type.isArray()) 
					for (Object obj : (Object[]) newObj)
						vals.add(toMongoObject(obj));
				else
					for (Object obj : (Iterable) newObj) 
						vals.add(toMongoObject(obj));
	
				return vals;
			} else {
				return newObj;
			}
		}
	}

	/**
	 * <p>
	 * Converts an entity (POJO) to a DBObject; A special field will be added to keep track of the class: {@link Mapper.CLASS_NAME_FIELDNAME}
	 * </p>
	 * @param entity The POJO
	 */
	public DBObject toDBObject(final Object entity) {
		return toDBObject(entity, null);
	}

	/**
	 * <p> Converts an entity (POJO) to a DBObject (for use with low-level driver); A special field will be added to keep track of the class: {@link Mapper.CLASS_NAME_FIELDNAME} </p>
	 * @param entity The POJO
	 * @param involvedObjects A Map of (already converted) POJOs
	 */
	public DBObject toDBObject(Object entity, Map<Object, DBObject> involvedObjects) {
		
		BasicDBObject dbObject = new BasicDBObject();
		MappedClass mc = getMappedClass(entity);
		
		if (mc.getEntityAnnotation() == null || !mc.getEntityAnnotation().noClassnameStored())
			dbObject.put(CLASS_NAME_FIELDNAME, entity.getClass().getName());

		dbObject = (BasicDBObject) mc.callLifecycleMethods(PrePersist.class, entity, dbObject, this);
		for (MappedField mf : mc.getPersistenceFields()) {
			try {
				Class<? extends Annotation> annType = null;
				
				//skip not saved fields.
				if (mf.hasAnnotation(NotSaved.class))
						continue;
			
				// get the annotation from the field.
				for (Class<? extends Annotation> testType : new Class[] { 	Id.class, 
																			Property.class, 
																			Embedded.class, 
																			Serialized.class, 
																			Reference.class }) {
					if (mf.hasAnnotation(testType)) {
						annType = testType;
						break;
					}
				}
				
				if (Id.class.equals(annType)) {
					Object idVal = mf.getFieldValue(entity);
					if (idVal != null) {
						if (!mf.isTypeMongoCompatible() && !converters.hasSimpleValueConverter(mf)) {
							opts.embeddedMapper.toDBObject(entity, mf, dbObject, involvedObjects, this);
						} else {
							Object dbVal = converters.encode(idVal);							
							dbObject.put(ID_KEY, dbVal);
						}
					}
				} else if (Property.class.equals(annType) || Serialized.class.equals(annType)
						|| mf.isTypeMongoCompatible() || (converters.hasSimpleValueConverter(mf)))
					opts.valueMapper.toDBObject(entity, mf, dbObject, involvedObjects, this);
				else if (Reference.class.equals(annType))
					opts.referenceMapper.toDBObject(entity, mf, dbObject, involvedObjects, this);
				else if (Embedded.class.equals(annType)) {
					opts.embeddedMapper.toDBObject(entity, mf, dbObject, involvedObjects, this);
				} else {
					logger.debug("No annotation was found, embedding " + mf);
					opts.embeddedMapper.toDBObject(entity, mf, dbObject, involvedObjects, this);
				}

			} catch (Exception e) {
				throw new MappingException("Error mapping field:" + mf.getFullName(), e);
			}
		}
		if (involvedObjects != null)
			involvedObjects.put(entity, dbObject);

		mc.callLifecycleMethods(PreSave.class, entity, dbObject, this);
		return dbObject;
	}
	
	Object fromDb(DBObject dbObject, final Object entity, EntityCache cache) {
		// check the history key (a key is the namespace + id)
		
		if (dbObject.containsField(ID_KEY) && getMappedClass(entity).getIdField() != null
				&& getMappedClass(entity).getEntityAnnotation() != null) {
			Key key = new Key(entity.getClass(), dbObject.get(ID_KEY));
			Object cachedInstance = cache.getEntity(key);
			if (cachedInstance != null)
				return cachedInstance;
			else
				cache.putEntity(key, entity); // to avoid stackOverflow in
												// recursive refs
		}

		MappedClass mc = getMappedClass(entity);
		
		dbObject = (BasicDBObject) mc.callLifecycleMethods(PreLoad.class, entity, dbObject, this);
		try {
			for (MappedField mf : mc.getPersistenceFields()) {
				if (mf.hasAnnotation(Id.class)) {
					setIdValue(entity, mf, dbObject, cache);
				} else if (mf.hasAnnotation(Property.class) || mf.hasAnnotation(Serialized.class)
						|| mf.isTypeMongoCompatible() || converters.hasSimpleValueConverter(mf))
					opts.valueMapper.fromDBObject(dbObject, mf, entity, cache, this);
				else if (mf.hasAnnotation(Embedded.class))
					opts.embeddedMapper.fromDBObject(dbObject, mf, entity, cache, this);
				else if (mf.hasAnnotation(Reference.class))
					opts.referenceMapper.fromDBObject(dbObject, mf, entity, cache, this);
				else {
					opts.embeddedMapper.fromDBObject(dbObject, mf, entity, cache, this);
				}
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		if (dbObject.containsField(ID_KEY) && getMappedClass(entity).getIdField() != null) {
			Key key = new Key(entity.getClass(), dbObject.get(ID_KEY));
			cache.putEntity(key, entity);
		}
		mc.callLifecycleMethods(PostLoad.class, entity, dbObject, this);
		return entity;
	}
	
	private void setIdValue(Object entity, MappedField mf, DBObject dbObject, EntityCache cache) {
		if (dbObject.get(ID_KEY) != null) {
			Object dbVal = dbObject.get(ID_KEY);
			Object idVal = null;
			
			if (!mf.isTypeMongoCompatible() && !converters.hasSimpleValueConverter(mf)) {
				opts.embeddedMapper.fromDBObject(dbObject, mf, entity, cache, this);
				idVal = mf.getFieldValue(entity);
			} else {
				idVal = converters.decode(mf.getType(), dbObject.get(ID_KEY), mf);							
				mf.setFieldValue(entity, idVal);
			}
			
			if (idVal == null)
				throw new MappingException(String.format("@Id field (_id='" + dbVal +"') was converted to null"));
		}
	}

	// TODO might be better to expose via some "options" object?
	public DefaultConverters getConverters() {
		return converters;
	}
	
	public EntityCache createEntityCache() {
		return new DefaultEntityCache();// TODO choose impl
	}
}
