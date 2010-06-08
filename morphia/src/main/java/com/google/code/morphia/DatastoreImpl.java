package com.google.code.morphia;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import com.google.code.morphia.annotations.CappedAt;
import com.google.code.morphia.annotations.Indexed;
import com.google.code.morphia.annotations.PostPersist;
import com.google.code.morphia.mapping.MappedClass;
import com.google.code.morphia.mapping.MappedField;
import com.google.code.morphia.mapping.Mapper;
import com.google.code.morphia.mapping.MappingException;
import com.google.code.morphia.mapping.cache.first.FirstLevelEntityCache;
import com.google.code.morphia.mapping.lazy.DatastoreHolder;
import com.google.code.morphia.mapping.lazy.proxy.ProxyHelper;
import com.google.code.morphia.query.Query;
import com.google.code.morphia.query.QueryImpl;
import com.google.code.morphia.query.UpdateOperations;
import com.google.code.morphia.query.UpdateOpsImpl;
import com.google.code.morphia.query.UpdateResults;
import com.google.code.morphia.utils.IndexDirection;
import com.google.code.morphia.utils.IndexFieldDef;
import com.google.code.morphia.utils.ReflectionUtils;
import com.mongodb.BasicDBObject;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.DBRef;
import com.mongodb.Mongo;
import com.mongodb.DB.WriteConcern;

/**
 * A generic (type-safe) wrapper around mongodb collections
 * 
 * @author Scott Hernandez
 */
@SuppressWarnings("unchecked")
public class DatastoreImpl implements Datastore, AdvancedDatastore {
	private static final Logger log = Logger.getLogger(DatastoreImpl.class.getName());
	
	protected Morphia morphia;
	protected Mongo mongo;
	protected DB db;
	
	public DatastoreImpl(Morphia morphia, Mongo mongo) {
		this(morphia, mongo, null);
	}
	
	public DatastoreImpl(Morphia morphia, Mongo mongo, String dbName) {
		this.morphia = morphia;
		this.mongo = mongo;
		this.db = mongo.getDB(dbName);
		
		// VERY discussable
		DatastoreHolder.getInstance().set(this);
	}
	
	public <T, V> DBRef createRef(Class<T> clazz, V id) {
		if (id == null)
			throw new MappingException("Could not get id for " + clazz.getName());
		return new DBRef(getDB(), getCollection(clazz).getName(), id);
	}
	

	public <T> DBRef createRef(T entity) {
		entity = ProxyHelper.unwrap(entity);
		Object id = getId(entity);
		if (id == null)
			throw new MappingException("Could not get id for " + entity.getClass().getName());
		return createRef(entity.getClass(), id);
	}
	

	public <T> Key<T> getKey(T entity) {
		entity = ProxyHelper.unwrap(entity);
		if (entity instanceof Key)
			return (Key<T>) entity;
		
		Object id = getId(entity);
		if (id == null)
			throw new MappingException("Could not get id for " + entity.getClass().getName());
		return new Key<T>((Class<T>) entity.getClass(), id);
	}
	
	protected <T, V> void delete(DBCollection dbColl, V id) {
		dbColl.remove(BasicDBObjectBuilder.start().add(Mapper.ID_KEY, ReflectionUtils.asObjectIdMaybe(id)).get());
	}
	

	public <T> void delete(String kind, T id) {
		DBCollection dbColl = getDB().getCollection(kind);
		delete(dbColl, id);
	}
	

	public <T, V> void delete(Class<T> clazz, V id) {
		
		FirstLevelEntityCache ec = morphia.getMapper().getFirstLevelCacheProvider().getEntityCache();
		Key key = new Key(clazz, id);
		ec.removeByKey(key);

		DBCollection dbColl = getCollection(clazz);
		delete(dbColl, id);
	}

	public <T, V> void delete(Class<T> clazz, Iterable<V> ids) {
		DBCollection dbColl = getCollection(clazz);			
		DBObject q = null;
		//TODO: replace with cursor.getQuery() in next update of the driver (pull request in).
		try {
			DBCursor cursor = ((QueryImpl<T>) find(clazz, Mapper.ID_KEY, ids)).prepareCursor();
			Field f = cursor.getClass().getField("_query");
			f.setAccessible(true);
			q = (DBObject) f.get(cursor);
		} catch (Exception e) {
			//do nothing, fall back to less efficient method below
		}
		
		if ( q!=null )
			dbColl.remove(q);
		else
			for (V id : ids)
				delete(clazz, id);
	}
	

	public <T> void delete(T entity) {
		entity = ProxyHelper.unwrap(entity);
		try {
			Object id = getId(entity);
			delete(entity.getClass(), id);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	

	public <T> void delete(Query<T> query) {
		QueryImpl<T> q = (QueryImpl<T>) query;
		DBCollection dbColl = getCollection(q.getEntityType());
		if (q.getQueryObject() != null)
			dbColl.remove(q.getQueryObject());
		else
			dbColl.remove(new BasicDBObject());
	}
	

	public <T> void ensureIndex(Class<T> clazz, String name, Set<IndexFieldDef> defs, boolean unique,
			boolean dropDupsOnCreate) {
		BasicDBObjectBuilder keys = BasicDBObjectBuilder.start();
		BasicDBObjectBuilder keyOpts = null;
		for (IndexFieldDef def : defs) {
			String fieldName = def.getField();
			IndexDirection dir = def.getDirection();
			if (dir == IndexDirection.BOTH)
				keys.add(fieldName, 1).add(fieldName, -1);
			else
				keys.add(fieldName, (dir == IndexDirection.ASC) ? 1 : -1);
		}
		
		if (name != null && !name.isEmpty()) {
			if (keyOpts == null)
				keyOpts = new BasicDBObjectBuilder();
			keyOpts.add("name", name);
		}
		if (unique) {
			if (keyOpts == null)
				keyOpts = new BasicDBObjectBuilder();
			keyOpts.add("unique", true);
			if (dropDupsOnCreate)
				keyOpts.add("dropDups", true);
		}
		
		DBCollection dbColl = getCollection(clazz);
		log.fine("Ensuring index for " + dbColl.getName() + "." + defs + " with keys " + keys);
		if (keyOpts == null) {
			log.fine("Ensuring index for " + dbColl.getName() + "." + defs + " with keys " + keys);
			dbColl.ensureIndex(keys.get());
		} else {
			log.fine("Ensuring index for " + dbColl.getName() + "." + defs + " with keys " + keys + " and opts "
					+ keyOpts);
			dbColl.ensureIndex(keys.get(), keyOpts.get());
		}
	}
	

	public <T> void ensureIndex(Class<T> type, String name, IndexDirection dir) {
		ensureIndex(type, null, Collections.singleton(new IndexFieldDef(name, dir)), false, false);
	}
	

	public <T> void ensureIndex(Class<T> type, Set<IndexFieldDef> fields) {
		ensureIndex(type, fields);
	}
	
	protected void ensureIndexes(MappedClass mc) {
		if (mc.getEntityAnnotation() == null)
			return;
		for (MappedField mf : mc.getPersistenceFields()) {
			if (mf.hasAnnotation(Indexed.class)) {
				Indexed index = mf.getAnnotation(Indexed.class);
				ensureIndex(mc.getClazz(), index.name(), Collections.singleton(new IndexFieldDef(mf.getName(), index
						.value())), index.unique(), index.dropDups());
			}
		}
	}
	

	public <T> void ensureIndexes(Class<T> clazz) {
		MappedClass mc = morphia.getMapper().getMappedClass(clazz);
		ensureIndexes(mc);
	}
	

	public void ensureIndexes() {
		// loops over mappedClasses and call ensureIndex for each @Entity object
		// (for now)
		for (MappedClass mc : morphia.getMappedClasses().values()) {
			ensureIndexes(mc);
		}
	}
	

	public void ensureCaps() {
		Mapper mapr = morphia.getMapper();
		for (MappedClass mc : mapr.getMappedClasses().values())
			if (mc.getEntityAnnotation() != null && mc.getEntityAnnotation().cap().value() > 0) {
				CappedAt cap = mc.getEntityAnnotation().cap();
				String collName = mapr.getCollectionName(mc.getClazz());
				BasicDBObjectBuilder dbCapOpts = BasicDBObjectBuilder.start("capped", true);
				if (cap.value() > 0)
					dbCapOpts.add("size", cap.value());
				if (cap.count() > 0)
					dbCapOpts.add("max", cap.count());
				DB db = getDB();
				if (db.getCollectionNames().contains(collName)) {
					DBObject dbResult = db.command(BasicDBObjectBuilder.start("collstats", collName).get());
					if (dbResult.containsField("capped")) {
						// TODO: check the cap options.
						log.warning("DBCollection already exists is cap'd already; doing nothing. " + dbResult);
					} else {
						log.warning("DBCollection already exists with same name(" + collName
								+ ") and is not cap'd; not creating cap'd version!");
					}
				} else {
					getDB().createCollection(collName, dbCapOpts.get());
					log.fine("Created cap'd DBCollection (" + collName + ") with opts " + dbCapOpts);
				}
			}
	}
	

	public <T> Query<T> createQuery(Class<T> clazz) {
		return new QueryImpl<T>(clazz, getCollection(clazz), this);
	}
	

	public <T> Query<T> find(String kind, Class<T> clazz) {
		return new QueryImpl<T>(clazz, getDB().getCollection(kind), this);
	}
	

	public <T> Query<T> find(Class<T> clazz) {
		return createQuery(clazz);
	}
	

	public <T, V> Query<T> find(Class<T> clazz, String property, V value) {
		Query<T> query = createQuery(clazz);
		return query.filter(property, value);
	}
	

	public <T, V> Query<T> find(String kind, Class<T> clazz, String property, V value, int offset, int size) {
		Query<T> query = find(kind, clazz);
		query.offset(offset);
		query.limit(size);
		return query.filter(property, value);
	}
	

	public <T, V> Query<T> find(Class<T> clazz, String property, V value, int offset, int size) {
		Query<T> query = createQuery(clazz);
		query.offset(offset);
		query.limit(size);
		return query.filter(property, value);
	}
	

	public <T> T get(Class<T> clazz, DBRef ref) {
		return morphia.fromDBObject(clazz, ref.fetch());
	}
	

	public <T, V> Query<T> get(Class<T> clazz, Iterable<V> ids) {
		List objIds = new ArrayList();
		for (V id : ids) {
			objIds.add(ReflectionUtils.asObjectIdMaybe(id));
		}
		return find(clazz, Mapper.ID_KEY + " in", objIds);
	}


	public <T> List<T> getByKeys(Iterable<Key<T>> keys) {
		return this.getByKeys((Class<T>) null, keys);
	}
	
	// TODO scott: should return a collection, to make obvious, that it has no
	// predefined order?
	public <T> List<T> getByKeys(Class<T> clazz, Iterable<Key<T>> keys) {
		
		Map<String, List<Key>> kindMap = new HashMap<String, List<Key>>();
		List<T> results = new ArrayList<T>();
		// String clazzKind = (clazz==null) ? null :
		// getMapper().getCollectionName(clazz);
		for (Key<?> key : keys) {
			key.updateKind(getMapper());
			
			// if (clazzKind != null && !key.getKind().equals(clazzKind))
			// throw new IllegalArgumentException("Types are not equal (" +
			// clazz + "!=" + key.getKindClass() +
			// ") for key and method parameter clazz");
			//
			if (kindMap.containsKey(key.getKind()))
				kindMap.get(key.getKind()).add(key);
			else
				kindMap.put(key.getKind(), new ArrayList<Key>(Collections.singletonList((Key) key)));
		}
		for (String kind : kindMap.keySet()) {
			List objIds = new ArrayList();
			List<Key> kindKeys = kindMap.get(kind);
			for (Key key : kindKeys) {
				objIds.add(ReflectionUtils.asObjectIdMaybe(key.getId()));
			}
			List kindResults = find(kind, null).filter("_id in", objIds).asList();
			results.addAll(kindResults);
		}
		return results;
	}
	

	public <T, V> T get(String kind, Class<T> clazz, V id) {
		List<T> results = find(kind, clazz, Mapper.ID_KEY, id, 0, 1).asList();
		if (results == null || results.size() == 0)
			return null;
		return results.get(0);
	}
	

	public <T, V> T get(Class<T> clazz, V id) {
		List<T> results = find(getCollection(clazz).getName(), clazz, Mapper.ID_KEY, id, 0, 1).asList();
		if (results == null || results.size() == 0)
			return null;
		return results.get(0);
	}
	

	public <T> T getByKey(Class<T> clazz, Key<T> key) {
		Mapper mapr = morphia.getMapper();
		//		
		// TODO us experimental
		FirstLevelEntityCache ec = mapr.getFirstLevelCacheProvider().getEntityCache();
		Object cached = ec.get(key);
		if (cached != null) {
			return (T) cached;
		}

		String kind = mapr.getCollectionName(clazz);
		String keyKind = key.updateKind(mapr);
		if (!kind.equals(keyKind))
			throw new RuntimeException("collection names don't match for key and class: " + kind + " != " + keyKind);
		
		return get(clazz, key.getId());
	}
	

	public <T> T get(T entity) {
		entity = ProxyHelper.unwrap(entity);
		Object id = getId(entity);
		if (id == null)
			throw new MappingException("Could not get id for " + entity.getClass().getName());
		return (T) get(entity.getClass(), id);
	}
	
	public DBCollection getCollection(Class clazz) {
		String collName = morphia.getMapper().getCollectionName(clazz);
		return getDB().getCollection(collName);
	}

	public DBCollection getCollection(Object obj) {
		String collName = morphia.getMapper().getCollectionName(obj);
		return getDB().getCollection(collName);
	}
	

	public <T> long getCount(T entity) {
		entity = ProxyHelper.unwrap(entity);
		return getCollection(entity).getCount();
	}
	

	public <T> long getCount(Class<T> clazz) {
		return getCollection(clazz).getCount();
	}
	

	public long getCount(String kind) {
		return getDB().getCollection(kind).getCount();
	}
	

	public <T> long getCount(Query<T> query) {
		return query.countAll();
	}
	

	public Mongo getMongo() {
		return this.mongo;
	}
	

	public DB getDB() {
		return db;
	}
	
	protected Object getId(Object entity) {
		entity = ProxyHelper.unwrap(entity);
		MappedClass mc;
		String keyClassName = entity.getClass().getName();
		if (morphia.getMappedClasses().containsKey(keyClassName))
			mc = morphia.getMappedClasses().get(keyClassName);
		else
			mc = new MappedClass(entity.getClass(), getMapper());
		
		try {
			return mc.getIdField().get(entity);
		} catch (Exception e) {
			return null;
		}
	}
	
	public Mapper getMapper() {
		return this.morphia.getMapper();
	}
	

	public <T> Iterable<Key<T>> save(Iterable<T> entities) {
		ArrayList<Key<T>> savedKeys = new ArrayList<Key<T>>();
		// for now, do it one at a time.
		for (T ent : entities)
			savedKeys.add(save(ent));
		return savedKeys;
		
	}
	

	public <T> Iterable<Key<T>> save(T... entities) {
		ArrayList<Key<T>> savedKeys = new ArrayList<Key<T>>();
		// for now, do it one at a time.
		for (T ent : entities)
			savedKeys.add(save(ent));
		return savedKeys;
	}
	
	protected <T> Key<T> save(DBCollection dbColl, T entity) {

		entity = ProxyHelper.unwrap(entity);
		Mapper mapr = morphia.getMapper();
		MappedClass mc = mapr.getMappedClass(entity);
		


		DB db = dbColl.getDB();
		// TODO scary message from driver ... db.requestStart();
		try {
			

			LinkedHashMap<Object, DBObject> involvedObjects = new LinkedHashMap<Object, DBObject>();
			DBObject dbObj = mapr.toDBObject(entity, involvedObjects);
			
			if (mc.hasVersionField()) {
				String versionKeyName = mc.getMappedVersionField().getMappedFieldName();
				Long oldVersion = (Long) mc.getMappedVersionField().getFieldValue(entity);
				long newVersion = VersionHelper.nextValue(oldVersion);
				dbObj.put(versionKeyName, newVersion);
				if (oldVersion != null && oldVersion > 0) {
					Object idValue = dbObj.get(Mapper.ID_KEY);
					
					UpdateResults<T> res = update(find((Class<T>) entity.getClass(), Mapper.ID_KEY, idValue).filter(
							versionKeyName, oldVersion), dbObj, false, false);
					
					if (res.getHadError())
						throw new MappingException("Error: " + res.getError());
					
					if (res.getUpdatedCount() != 1)
						throw new ConcurrentModificationException("Entity of class " + entity.getClass().getName()
								+ " (id='" + idValue + "',version='" + oldVersion + "') was concurrently updated.");
				} else {
					dbColl.save(dbObj);
				}
				
				mc.getMappedVersionField().setFieldValue(entity, newVersion);
			} else
				dbColl.save(dbObj);
			
			if (dbObj.get(Mapper.ID_KEY) == null)
				throw new MappingException("Missing _id after save!");
			
			if (dbColl.getWriteConcern() == WriteConcern.STRICT) {
				DBObject lastErr = db.getLastError();
				if (lastErr.get("err") != null)
					throw new MappingException("Error: " + lastErr.toString());
			}
			postSaveOperations(entity, dbObj, dbColl, involvedObjects);
			Key<T> key = new Key<T>(dbColl.getName(), getId(entity));
			key.setKindClass((Class<? extends T>) entity.getClass());
			
			//			
			// TODO us experimental
			FirstLevelEntityCache ec = mapr.getFirstLevelCacheProvider().getEntityCache();
			ec.removeByKey(key);

			return key;
		} finally {
			// TODO scary message from driver ... db.requestDone();
		}
	}
	
	private void firePostPersistForChildren(LinkedHashMap<Object, DBObject> involvedObjects, Mapper mapr) {
		for (Map.Entry<Object, DBObject> e : involvedObjects.entrySet()) {
			Object entity = e.getKey();
			DBObject dbObj = e.getValue();
			MappedClass mc = mapr.getMappedClass(entity);
			
			mc.callLifecycleMethods(PostPersist.class, entity, dbObj, mapr);
		}
	}
	

	public <T> Key<T> save(String kind, T entity) {
		entity = ProxyHelper.unwrap(entity);
		DBCollection dbColl = getDB().getCollection(kind);
		return save(dbColl, entity);
	}
	

	public <T> Key<T> save(T entity) {
		entity = ProxyHelper.unwrap(entity);
		DBCollection dbColl = getCollection(entity);
		return save(dbColl, entity);
	}
	

	public UpdateOperations createUpdateOperations() {
		return new UpdateOpsImpl(getMapper());
	}


	public <T> UpdateResults<T> update(Query<T> query, UpdateOperations ops, boolean createIfMissing) {
		return update(query, ops, createIfMissing, false);
	}


	public <T> UpdateResults<T> update(Query<T> query, UpdateOperations ops) {
		return update(query, ops, false, true);
	}
	

	public <T> UpdateResults<T> updateFirst(Query<T> query, UpdateOperations ops) {
		return update(query, ops, false, false);
	}
	

	public <T> UpdateResults<T> updateFirst(Query<T> query, UpdateOperations ops, boolean createIfMissing) {
		return update(query, ops, createIfMissing, false);
	}
	

	public <T> UpdateResults<T> updateFirst(Query<T> query, T entity, boolean createIfMissing) {
		Mapper mapr = morphia.getMapper();
		LinkedHashMap<Object, DBObject> involvedObjects = new LinkedHashMap<Object, DBObject>();
		DBObject u = mapr.toDBObject(entity, involvedObjects);
		
		UpdateResults<T> res = update(query, u, createIfMissing, false);
		postSaveOperations(entity, u, getCollection(entity), involvedObjects);
		return res;
	}
	
	private <T> void postSaveOperations(Object entity, DBObject dbObj, DBCollection dbColl,
			LinkedHashMap<Object, DBObject> involvedObjects) {
		Mapper mapr = morphia.getMapper();
		MappedClass mc = mapr.getMappedClass(entity);
		
		mapr.updateKeyInfo(entity, dbObj.get(Mapper.ID_KEY));
		
		firePostPersistForChildren(involvedObjects, mapr);
		mc.callLifecycleMethods(PostPersist.class, entity, dbObj, mapr);
	}
	
	private <T> UpdateResults<T> update(Query<T> query, UpdateOperations ops, boolean createIfMissing, boolean multi) {
		DBObject u = ((UpdateOpsImpl) ops).getOps();
		return update(query, u, createIfMissing, multi);
	}
	
	private <T> UpdateResults<T> update(Query<T> query, DBObject u, boolean createIfMissing, boolean multi) {
		DBCollection dbColl = getCollection(((QueryImpl<T>) query).getEntityType());
		DBObject q = ((QueryImpl<T>) query).getQueryObject();
		if (q == null)
			q = new BasicDBObject();
		dbColl.update(q, u, createIfMissing, multi);
		DBObject dbObj = dbColl.getDB().getLastError();
		return new UpdateResults<T>(dbObj);
	}
}
