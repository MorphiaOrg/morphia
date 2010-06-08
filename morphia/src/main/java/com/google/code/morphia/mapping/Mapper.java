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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.code.morphia.EntityInterceptor;
import com.google.code.morphia.Key;
import com.google.code.morphia.annotations.Embedded;
import com.google.code.morphia.annotations.Id;
import com.google.code.morphia.annotations.PostLoad;
import com.google.code.morphia.annotations.PreLoad;
import com.google.code.morphia.annotations.PrePersist;
import com.google.code.morphia.annotations.PreSave;
import com.google.code.morphia.annotations.Property;
import com.google.code.morphia.annotations.Reference;
import com.google.code.morphia.annotations.Serialized;
import com.google.code.morphia.converters.DefaultConverters;
import com.google.code.morphia.mapping.cache.first.DefaultFirstLevelCacheProvider;
import com.google.code.morphia.mapping.cache.first.FirstLevelCacheProvider;
import com.google.code.morphia.mapping.cache.first.FirstLevelEntityCache;
import com.google.code.morphia.mapping.lazy.CGLibLazyProxyFactory;
import com.google.code.morphia.mapping.lazy.DatastoreProvider;
import com.google.code.morphia.mapping.lazy.DefaultDatastoreProvider;
import com.google.code.morphia.mapping.lazy.LazyFeatureDependencies;
import com.google.code.morphia.mapping.lazy.LazyProxyFactory;
import com.google.code.morphia.mapping.lazy.proxy.ProxiedReference;
import com.google.code.morphia.mapping.lazy.proxy.ProxyHelper;
import com.google.code.morphia.utils.ReflectionUtils;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

/**
 * @author Olafur Gauti Gudmundsson
 * @author Scott Hernandez
 */
@SuppressWarnings("unchecked")
public class Mapper {

	public static final Logger logger = Logger.getLogger(Mapper.class.getName());

	public static final String ID_KEY = "_id";
	public static final String IGNORED_FIELDNAME = ".";
	public static final String CLASS_NAME_FIELDNAME = "className";

	/** Set of classes that have been validated for mapping by this mapper */
	private final ConcurrentHashMap<String, MappedClass> mappedClasses = new ConcurrentHashMap<String, MappedClass>();
	private final ConcurrentLinkedQueue<EntityInterceptor> interceptors = new ConcurrentLinkedQueue<EntityInterceptor>();
	
	// TODO: make these configurable
	private final DefaultConverters converters = new DefaultConverters();;
	private final ReferenceMapper referenceMapper = new ReferenceMapper(this, converters);
	private final EmbeddedMapper embeddedMapper = new EmbeddedMapper(this, converters);
	private final ValueMapper valueMapper = new ValueMapper(converters);
	final LazyProxyFactory proxyFactory = LazyFeatureDependencies.testDependencyFullFilled() ? new CGLibLazyProxyFactory()
			: null;
	DatastoreProvider datastoreProvider = new DefaultDatastoreProvider();
	MapperOptions opts = new MapperOptions();
	// TODO make configurable
	private FirstLevelCacheProvider firstLevelCacheProvider = new DefaultFirstLevelCacheProvider();
	
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
		
		if (ProxyHelper.isProxy(obj))
			type = ProxyHelper.getReferentClass(obj);
		
		MappedClass mc = mappedClasses.get(type.getName());
		if (mc == null) {
			// no validation
			mc = new MappedClass(type, this);
			mappedClasses.put(mc.getClazz().getName(), mc);
		}
		return mc;
	}

	public String getCollectionName(Object object) {
		if (ProxyHelper.isProxy(object))
			return getCollectionName(((ProxiedReference) object).__getReferenceObjClass());

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
	 * @param rawIdValue
	 *            Value to update with; null means skip
	 */
	public void updateKeyInfo(final Object entity, final Object rawIdValue) {
		MappedClass mc = getMappedClass(entity);
		// update id field, if there.
		if ((mc.getIdField() != null) && (rawIdValue != null)) {
			try {
				Object dbIdValue = converters.decode(mc.getIdField().getType(), rawIdValue);
				Object idValue = mc.getIdField().get(entity);
				if (idValue != null) {
					// The entity already had an id set. Check to make sure it
					// hasn't changed. That would be unexpected, and could
					// indicate a bad state.
					if (!dbIdValue.equals(idValue)) {
						throw new RuntimeException("id mismatch: " + idValue + " != " + dbIdValue + " for "
								+ entity.getClass().getName());
					}
				} else {
					mc.getIdField().set(entity, dbIdValue);
				}
			} catch (Exception e) {
				if (e.getClass().equals(RuntimeException.class)) {
					throw (RuntimeException) e;
				}

				throw new RuntimeException(e);
			}
		}
	}

	/** coverts a DBObject back to a type-safe java object */
	public Object fromDBObject(final Class entityClass, final DBObject dbObject) {
		if (dbObject == null) {
			Throwable t = new Throwable();
			logger.log(Level.SEVERE, "Somebody passed in a null dbObject; bad client!", t);
			return null;
		}

		Object entity = null;
		try {
			entity = ReflectionUtils.createInstance(entityClass, dbObject);
			fromDb(dbObject, entity);
		} finally {
			// TODO ugly, but tmp. here to keep compatibility
			if (this.firstLevelCacheProvider instanceof DefaultFirstLevelCacheProvider) {
				DefaultFirstLevelCacheProvider d = (DefaultFirstLevelCacheProvider) this.firstLevelCacheProvider;
				d.release();
			}
		}
		return entity;
	}

	/**
	 * <p>
	 * Converts a java object to a mongo-compatible object (possibly a DBObject
	 * for complex mappings)
	 * </p>
	 * <p>
	 * Used by query/update operations
	 * </p>
	 */
	public Object toMongoObject(final Object javaObj) {
		if (javaObj == null) {
			return null;
		}
		Class origClass = javaObj.getClass();
		Object newObj = converters.encode(origClass, javaObj);
		Class type = newObj.getClass();
		boolean bSameType = origClass.equals(type);
		boolean bSingleValue = true;
		Class subType = null;

		if (type.isArray()
				|| ReflectionUtils.implementsAnyInterface(type, Iterable.class, Collection.class, List.class,
						Set.class, Map.class)) {
			bSingleValue = false;
			// subtype of Long[], List<Long> is Long
			subType = (type.isArray()) ? type.getComponentType() : ReflectionUtils.getParameterizedClass(type);
		}

		if (bSameType && bSingleValue && !ReflectionUtils.isPropertyType(type)) {
			DBObject dbObj = toDBObject(javaObj);
			dbObj.removeField(CLASS_NAME_FIELDNAME);
			return dbObj;
		} else if (bSameType && !bSingleValue && !ReflectionUtils.isPropertyType(subType)) {
			ArrayList<Object> vals = new ArrayList<Object>();
			if (type.isArray()) {
				for (Object obj : (Object[]) newObj) {
					vals.add(toMongoObject(obj));
				}
			} else {
				for (Object obj : (Iterable) newObj) {
					vals.add(toMongoObject(obj));
				}
			}
			return vals;
		} else {
			return newObj;
		}
	}

	public DBObject toDBObject(final Object entity) {
		return toDBObject(entity, null);
	}

	/**
	 * <p>
	 * Converts an entity (POJO) to a DBObject
	 * </p>
	 */
	public DBObject toDBObject(Object entity, final LinkedHashMap<Object, DBObject> involvedObjects) {
		
		entity = ProxyHelper.unwrap(entity);
		BasicDBObject dbObject = new BasicDBObject();
		MappedClass mc = getMappedClass(entity);
		
		dbObject.put(CLASS_NAME_FIELDNAME, entity.getClass().getName());

		// if ( mc.getPolymorphicAnnotation() != null ) {
		// dbObject.put(CLASS_NAME_FIELDNAME,
		// entity.getClass().getCanonicalName());
		// }
		
		dbObject = (BasicDBObject) mc.callLifecycleMethods(PrePersist.class, entity, dbObject, this);
		for (MappedField mf : mc.getPersistenceFields()) {
			try {
				Class<? extends Annotation> annType = opts.defaultFieldAnnotation;
				boolean foundAnnotation = false;
				// get the annotation from the field.
				for (Class<? extends Annotation> testType : new Class[] { Id.class, Property.class, Embedded.class,
						Serialized.class, Reference.class })
					if (mf.hasAnnotation(testType)) {
						annType = testType;
						foundAnnotation = true;
						break;
					}
				if (Id.class.equals(annType)) {
					Object dbVal = mf.getFieldValue(entity);
					if (dbVal != null)
						dbObject.put(ID_KEY, converters.encode(ReflectionUtils.asObjectIdMaybe(dbVal)));
				} else if (Property.class.equals(annType) || Serialized.class.equals(annType)
						|| mf.isTypeMongoCompatible())
					valueMapper.toDBObject(entity, mf, dbObject, opts);
				else if (Reference.class.equals(annType))
					referenceMapper.toDBObject(entity, mf, dbObject, opts);
				else if (Embedded.class.equals(annType)) {
					embeddedMapper.toDBObject(entity, mf, dbObject, involvedObjects, opts);
					if (!foundAnnotation)
						logger.fine("No annotation was found, embedding " + mf);
					
				} else {
					logger.warning("Ignoring field: " + mf.getFullName() + " [type:" + mf.getType().getSimpleName()
							+ "]");
				}

			} catch (Exception e) {
				throw new MappingException("Error mapping field:" + mf.getFullName(), e);
			}
		}
		if (involvedObjects != null) {
			involvedObjects.put(entity, dbObject);
		}
		mc.callLifecycleMethods(PreSave.class, entity, dbObject, this);
		return dbObject;
	}
	
	Object fromDb(DBObject dbObject, final Object entity) {
		// check the history key (a key is the namespace + id)
		
		if (dbObject.containsField(ID_KEY)) {
			if (getMappedClass(entity).getIdField() != null) {
				
				String id = dbObject.get(ID_KEY).toString();
				Key key = new Key(entity.getClass(), id);
				FirstLevelEntityCache entityCache = firstLevelCacheProvider.getEntityCache();
				Object cachedInstance = entityCache.get(key);
				if (cachedInstance != null)
					return cachedInstance;
				else
					entityCache.put(key, entity); // to avoid stackOverflow in
													// Recursive refs

			}
		}

		// String cacheKey = (!dbObject.containsField(ID_KEY)) ? null : "[" +
		// dbObject.getString(ID_KEY) + "]";
		// if (entityCache.get() == null) {
		// entityCache.set(new HashMap<String, Object>());
		// }
		// if (cacheKey != null) {
		// if (entityCache.get().containsKey(cacheKey)) {
		// return entityCache.get().get(cacheKey);
		// } else {
		// entityCache.get().put(cacheKey, entity);
		// }
		// }

		MappedClass mc = getMappedClass(entity);
		
		dbObject = (BasicDBObject) mc.callLifecycleMethods(PreLoad.class, entity, dbObject, this);
		try {
			for (MappedField mf : mc.getPersistenceFields()) {
				if (mf.hasAnnotation(Id.class)) {
					if (dbObject.get(ID_KEY) != null) {
						mf.setFieldValue(entity, converters.decode(mf.getType(), dbObject.get(ID_KEY)));
					}
				} else if (mf.hasAnnotation(Property.class) || mf.hasAnnotation(Serialized.class)
						|| mf.isTypeMongoCompatible())
					valueMapper.fromDBObject(dbObject, mf, entity);
				else if (mf.hasAnnotation(Embedded.class))
					embeddedMapper.fromDBObject(dbObject, mf, entity);
				else if (mf.hasAnnotation(Reference.class))
					referenceMapper.fromDBObject(dbObject, mf, entity);
				else
					logger.warning("Ignoring field: " + mf.getFullName() + " [type:" + mf.getType().getName() + "]");
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		if (dbObject.containsField(ID_KEY)) {
			if (getMappedClass(entity).getIdField() != null) {
				
				String id = dbObject.get(ID_KEY).toString();
				Key key = new Key(entity.getClass(), id);
				FirstLevelEntityCache entityCache = firstLevelCacheProvider.getEntityCache();

					entityCache.put(key, entity);
			}
		}
		mc.callLifecycleMethods(PostLoad.class, entity, dbObject, this);
		return entity;
	}
	
	public FirstLevelCacheProvider getFirstLevelCacheProvider() {
		return firstLevelCacheProvider;
	}
}
