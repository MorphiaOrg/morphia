package com.google.code.morphia;

import java.util.ArrayList;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.code.morphia.annotations.CappedAt;
import com.google.code.morphia.annotations.Indexed;
import com.google.code.morphia.annotations.PostPersist;
import com.google.code.morphia.annotations.Version;
import com.google.code.morphia.logging.MorphiaLogger;
import com.google.code.morphia.logging.MorphiaLoggerFactory;
import com.google.code.morphia.mapping.MappedClass;
import com.google.code.morphia.mapping.MappedField;
import com.google.code.morphia.mapping.Mapper;
import com.google.code.morphia.mapping.MappingException;
import com.google.code.morphia.mapping.cache.EntityCache;
import com.google.code.morphia.mapping.lazy.DatastoreHolder;
import com.google.code.morphia.mapping.lazy.proxy.ProxiedEntityReference;
import com.google.code.morphia.mapping.lazy.proxy.ProxyHelper;
import com.google.code.morphia.query.Query;
import com.google.code.morphia.query.QueryException;
import com.google.code.morphia.query.QueryImpl;
import com.google.code.morphia.query.UpdateOperations;
import com.google.code.morphia.query.UpdateOpsImpl;
import com.google.code.morphia.query.UpdateResults;
import com.google.code.morphia.utils.IndexDirection;
import com.google.code.morphia.utils.IndexFieldDef;
import com.mongodb.BasicDBObject;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.CommandResult;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.DBRef;
import com.mongodb.Mongo;
import com.mongodb.WriteConcern;
import com.mongodb.WriteResult;

/**
 * A generic (type-safe) wrapper around mongodb collections
 * 
 * @author Scott Hernandez
 */
@SuppressWarnings("unchecked")
public class DatastoreImpl implements Datastore, AdvancedDatastore {
	private static final MorphiaLogger log = MorphiaLoggerFactory.get(DatastoreImpl.class);
	
	protected Morphia morphia;
	protected Mongo mongo;
	protected DB db;
	protected WriteConcern defConcern = WriteConcern.SAFE;
	
	public DatastoreImpl(Morphia morphia, Mongo mongo) {
		this(morphia, mongo, null);
	}
	
	public DatastoreImpl(Morphia morphia, Mongo mongo, String dbName, String username, char[] password) {
		this.morphia = morphia;
		this.mongo = mongo;
		this.db = mongo.getDB(dbName);
		if (username != null) 
			this.db.authenticate(username, password);
		
		// VERY discussable
		DatastoreHolder.getInstance().set(this);
	}

	public DatastoreImpl(Morphia morphia, Mongo mongo, String dbName) {
		this(morphia, mongo, dbName, null, null);
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
		
		if (entity instanceof ProxiedEntityReference) {
			ProxiedEntityReference proxy = (ProxiedEntityReference) entity;
			return (Key<T>) proxy.__getKey();
		}
		
		entity = ProxyHelper.unwrap(entity);
		if (entity instanceof Key)
			return (Key<T>) entity;
		
		Object id = getId(entity);
		if (id == null)
			throw new MappingException("Could not get id for " + entity.getClass().getName());
		return new Key<T>((Class<T>) entity.getClass(), id);
	}
	
	protected <T, V> void delete(DBCollection dbColl, V id, WriteConcern wc) {
		WriteResult wr;
		if (wc == null)
			wr = dbColl.remove(BasicDBObjectBuilder.start().add(Mapper.ID_KEY, id).get());
		else
			wr = dbColl.remove(BasicDBObjectBuilder.start().add(Mapper.ID_KEY, id).get(), wc);
		
		throwOnError(wc, wr);
	}
	

	public <T> void delete(String kind, T id) {
		DBCollection dbColl = getDB().getCollection(kind);
		delete(dbColl, id, defConcern);
	}
	
	public <T, V> void delete(Class<T> clazz, V id, WriteConcern wc) {
		DBCollection dbColl = getCollection(clazz);
		delete(dbColl, id, wc);
	}

	public <T, V> void delete(Class<T> clazz, V id) {
		delete(clazz, id, defConcern);
	}

	public <T, V> void delete(Class<T> clazz, Iterable<V> ids) {
		Query<T> q = find(clazz).disableValidation().filter(Mapper.ID_KEY + " in", ids);
		delete(q);
	}
	
	public <T> void delete(T entity) {
		delete(entity, defConcern);
	}
	
	public <T> void delete(T entity, WriteConcern wc) {
		entity = ProxyHelper.unwrap(entity);
		if (entity instanceof Class<?>)
			throw new MappingException("Did you mean to delete all documents? -- delete(ds.createQuery(???.class))");
		try {
			Object id = getId(entity);
			delete(entity.getClass(), id, wc);
				
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public <T> void delete(Query<T> query) {
		delete(query, defConcern);
	}
	
	public <T> void delete(Query<T> query, WriteConcern wc) {
		QueryImpl<T> q = (QueryImpl<T>) query;
		DBCollection dbColl = getCollection(q.getEntityClass());
		WriteResult wr;
		if (q.getQueryObject() != null)
			if (wc == null)
				wr = dbColl.remove(q.getQueryObject());
			else
				wr = dbColl.remove(q.getQueryObject(), wc);
		else
			if (wc == null)
				wr = dbColl.remove(new BasicDBObject());
			else
				wr = dbColl.remove(new BasicDBObject(), wc);
		
		throwOnError(wc, wr);
	}
	
	public <T> void ensureIndex(Class<T> clazz, String name, IndexFieldDef[] defs, boolean unique,
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
		log.debug("Ensuring index for " + dbColl.getName() + "." + defs + " with keys " + keys);
		if (keyOpts == null) {
			log.debug("Ensuring index for " + dbColl.getName() + "." + defs + " with keys " + keys);
			dbColl.ensureIndex(keys.get());
		} else {
			log.debug("Ensuring index for " + dbColl.getName() + "." + defs + " with keys " + keys + " and opts "
					+ keyOpts);
			dbColl.ensureIndex(keys.get(), keyOpts.get());
		}
	}
	
	public <T> void ensureIndex(Class<T> type, String name, IndexDirection dir) {
		ensureIndex(type, new IndexFieldDef(name, dir));
	}
	
	public <T> void ensureIndex(Class<T> type, IndexFieldDef... fields) {
		ensureIndex(type, null, fields, false, false);
	}
	
	protected void ensureIndexes(MappedClass mc) {
		if (mc.getEntityAnnotation() == null)
			return;
		for (MappedField mf : mc.getPersistenceFields()) {
			if (mf.hasAnnotation(Indexed.class)) {
				Indexed index = mf.getAnnotation(Indexed.class);
				ensureIndex(mc.getClazz(), index.name(), new IndexFieldDef[] {new IndexFieldDef(mf.getNameToStore(), index
						.value())}, index.unique(), index.dropDups());
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
					log.debug("Created cap'd DBCollection (" + collName + ") with opts " + dbCapOpts);
				}
			}
	}
	

	public <T> Query<T> createQuery(Class<T> clazz) {
		return new QueryImpl<T>(clazz, getCollection(clazz), this);
	}

	public <T> Query<T> createQuery(Class<T> kind, DBObject q) {
		QueryImpl<T> ret = (QueryImpl<T>) createQuery(kind);
		ret.setQueryObject(q);
		return ret;
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
		return morphia.fromDBObject(clazz, ref.fetch(), createCache());
	}
	

	public <T, V> Query<T> get(Class<T> clazz, Iterable<V> ids) {
		return find(clazz).disableValidation().filter(Mapper.ID_KEY + " in", ids);
	}

	/** Queries the server to check for each DBRef */
	@SuppressWarnings("rawtypes")
	public <T> List<Key<T>> getKeysByRefs(List<DBRef> refs) {
		ArrayList<Key<T>> tempKeys = new ArrayList<Key<T>>(refs.size());
		
		Map<String, List<DBRef>> kindMap = new HashMap<String, List<DBRef>>();
		for (DBRef ref : refs) {
			if (kindMap.containsKey(ref.getRef()))
				kindMap.get(ref.getRef()).add(ref);
			else
				kindMap.put(ref.getRef(), new ArrayList<DBRef>(Collections.singletonList((DBRef) ref)));
		}
		for (String kind : kindMap.keySet()) {
			List objIds = new ArrayList();
			List<DBRef> kindRefs = kindMap.get(kind);
			for (DBRef key : kindRefs) {
				objIds.add(key.getId());
			}
			List<Key<T>> kindResults = this.<T>find(kind, null).disableValidation().filter("_id in", objIds).asKeyList();
			tempKeys.addAll(kindResults);
		}
		
		//put them back in order, minus the missing ones.
		ArrayList<Key<T>> keys = new ArrayList<Key<T>>(refs.size());
		for (DBRef ref : refs) {
			Key<T> testKey = new Key<T>(ref);
			if (tempKeys.contains(testKey))
				keys.add(testKey);
		}
		return keys;
	}

	public <T> List<T> getByKeys(Iterable<Key<T>> keys) {
		return this.getByKeys((Class<T>) null, keys);
	}

	@SuppressWarnings("rawtypes")
	public <T> List<T> getByKeys(Class<T> clazz, Iterable<Key<T>> keys) {
		
		Map<String, List<Key>> kindMap = new HashMap<String, List<Key>>();
		List<T> entities = new ArrayList<T>();
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
				objIds.add(key.getId());
			}
			List kindResults = find(kind, null).disableValidation().filter("_id in", objIds).asList();
			entities.addAll(kindResults);
		}
		
		//TODO: order them based on the incoming Keys.
		return entities;
	}
	

	public <T, V> T get(String kind, Class<T> clazz, V id) {
		List<T> results = find(kind, clazz, Mapper.ID_KEY, id, 0, 1).asList();
		if (results == null || results.size() == 0)
			return null;
		return results.get(0);
	}
	

	public <T, V> T get(Class<T> clazz, V id) {
		return find(getCollection(clazz).getName(), clazz, Mapper.ID_KEY, id, 0, 1).get();
	}
	

	public <T> T getByKey(Class<T> clazz, Key<T> key) {
		Mapper mapr = morphia.getMapper();

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
	
	@SuppressWarnings("rawtypes")
	public DBCollection getCollection(Class clazz) {
		String collName = morphia.getMapper().getCollectionName(clazz);
		DBCollection dbC = getDB().getCollection(collName);
		return dbC;
	}

	public DBCollection getCollection(Object obj) {
		if (obj == null) return null;
		return getCollection(obj.getClass());
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
	
	public <T> Iterable<Key<T>> insert(Iterable<T> entities) {
		return insert(entities, defConcern);
	}
	
	public <T> Iterable<Key<T>> insert(Iterable<T> entities, WriteConcern wc) {
		ArrayList<Key<T>> savedKeys = new ArrayList<Key<T>>();
		for (T ent : entities)
			savedKeys.add(insert(ent, wc));
		return savedKeys;
	}

	public <T> Iterable<Key<T>> insert(T...entities) {
		ArrayList<Key<T>> savedKeys = new ArrayList<Key<T>>();
		for (T ent : entities)
			savedKeys.add(insert(ent));
		return savedKeys;
	}
	
	public <T> Key<T> insert(T entity) {
		return insert(entity, defConcern);
	}
	public <T> Key<T> insert(T entity, WriteConcern wc) {
		entity = ProxyHelper.unwrap(entity);
		DBCollection dbColl = getCollection(entity);
		return insert(dbColl, entity, wc);
	}

	public <T> Key<T> insert(String kind, T entity) {
		entity = ProxyHelper.unwrap(entity);
		DBCollection dbColl = getDB().getCollection(kind);
		return insert(dbColl, entity, defConcern);
	}

	public <T> Key<T> insert(String kind, T entity, WriteConcern wc) {
		entity = ProxyHelper.unwrap(entity);
		DBCollection dbColl = getDB().getCollection(kind);
		return insert(dbColl, entity, wc);
	}

	protected <T> Key<T> insert(DBCollection dbColl, T entity) {
		return insert(dbColl, entity, defConcern);
	}
	
	protected <T> Key<T> insert(DBCollection dbColl, T entity, WriteConcern wc) {
		entity = ProxyHelper.unwrap(entity);
		Mapper mapr = morphia.getMapper();
		
		LinkedHashMap<Object, DBObject> involvedObjects = new LinkedHashMap<Object, DBObject>();
		DBObject dbObj = mapr.toDBObject(entity, involvedObjects);

		WriteResult wr;
		if (wc == null)
			wr = dbColl.insert(dbObj);
		else
			wr = dbColl.insert(dbObj, wc);

		if (dbObj.get(Mapper.ID_KEY) == null)
			throw new MappingException("Missing _id after save!");
		
		throwOnError(wc, wr);
		
		postSaveOperations(entity, dbObj, dbColl, involvedObjects);
		Key<T> key = new Key<T>(dbColl.getName(), getId(entity));
		key.setKindClass((Class<? extends T>) entity.getClass());
		
		return key;
	}

	public <T> Iterable<Key<T>> save(Iterable<T> entities) {
		return save(entities, defConcern);
	}
	
	public <T> Iterable<Key<T>> save(Iterable<T> entities, WriteConcern wc) {
		ArrayList<Key<T>> savedKeys = new ArrayList<Key<T>>();
		for (T ent : entities)
			savedKeys.add(save(ent, wc));
		return savedKeys;
		
	}	

	public <T> Iterable<Key<T>> save(T... entities) {
		ArrayList<Key<T>> savedKeys = new ArrayList<Key<T>>();
		for (T ent : entities)
			savedKeys.add(save(ent));
		return savedKeys;
	}
	
	protected <T> Key<T> save(DBCollection dbColl, T entity, WriteConcern wc) {
		entity = ProxyHelper.unwrap(entity);
		Mapper mapr = morphia.getMapper();
		MappedClass mc = mapr.getMappedClass(entity);
		
		WriteResult wr = null;
		
		LinkedHashMap<Object, DBObject> involvedObjects = new LinkedHashMap<Object, DBObject>();
		DBObject dbObj = mapr.toDBObject(entity, involvedObjects);

		//try to do an update if there is a @Version field
		wr = tryVersionedUpdate(dbColl, entity, dbObj, wc, db, mc);
		
		if(wr == null)
			if (wc == null)
				wr = dbColl.save(dbObj);
			else
				wr = dbColl.save(dbObj, wc);
		
		if (dbObj.get(Mapper.ID_KEY) == null)
			throw new MappingException("Missing _id after save!");
		
		throwOnError(wc, wr);
		
		postSaveOperations(entity, dbObj, dbColl, involvedObjects);
		Key<T> key = new Key<T>(dbColl.getName(), getId(entity));
		key.setKindClass((Class<? extends T>) entity.getClass());
		
		return key;
	}
	
	protected <T> WriteResult tryVersionedUpdate(DBCollection dbColl, T entity, DBObject dbObj, WriteConcern wc, DB db, MappedClass mc) {
		WriteResult wr = null;
		if (mc.getFieldsAnnotatedWith(Version.class).isEmpty())
			return wr;
		
		
		MappedField mfVersion = mc.getFieldsAnnotatedWith(Version.class).get(0);
		String versionKeyName = mfVersion.getNameToStore();
		Long oldVersion = (Long) mfVersion.getFieldValue(entity);
		long newVersion = VersionHelper.nextValue(oldVersion);
		dbObj.put(versionKeyName, newVersion);
		if (oldVersion != null && oldVersion > 0) {
			Object idValue = dbObj.get(Mapper.ID_KEY);
			
			UpdateResults<T> res = update(  find((Class<T>) entity.getClass(), Mapper.ID_KEY, idValue).filter(versionKeyName, oldVersion), 
											dbObj, 
											false, 
											false, 
											wc);
			
			wr = res.getWriteResult();
			
			if (res.getUpdatedCount() != 1)
				throw new ConcurrentModificationException("Entity of class " + entity.getClass().getName()
						+ " (id='" + idValue + "',version='" + oldVersion + "') was concurrently updated.");
		} else
			if (wc == null)
				wr = dbColl.save(dbObj);
			else
				wr = dbColl.save(dbObj, wc);

		//update the version.
		mfVersion.setFieldValue(entity, newVersion);
		return wr;
	}
	
	protected void throwOnError(WriteConcern wc, WriteResult wr) {
		if ( wc == null && wr.getLastConcern() == null) {
			CommandResult cr = wr.getLastError();
			if (cr != null && cr.getErrorMessage() != null && !cr.getErrorMessage().isEmpty())
				cr.throwOnError();
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
		return save(dbColl, entity, defConcern);
	}
	
	public <T> Key<T> save(T entity) {
		return save(entity, defConcern);
	}

	public <T> Key<T> save(T entity, WriteConcern wc) {
		entity = ProxyHelper.unwrap(entity);
		DBCollection dbColl = getCollection(entity);
		return save(dbColl, entity, wc);
	}

	public <T> UpdateOperations<T> createUpdateOperations(Class<T> clazz) {
		return new UpdateOpsImpl<T>(clazz, getMapper());
	}

	public <T> UpdateOperations<T> createUpdateOperations(Class<T> kind, DBObject ops) {
		UpdateOpsImpl<T> upOps = (UpdateOpsImpl<T>) createUpdateOperations(kind);
		upOps.setOps(ops);
		return upOps;
	}

	public <T> UpdateResults<T> update(Query<T> query, UpdateOperations<T> ops, boolean createIfMissing) {
		return update(query, ops, createIfMissing, defConcern);
	}
	
	public <T> UpdateResults<T> update(Query<T> query, UpdateOperations<T> ops, boolean createIfMissing, WriteConcern wc) {
		return update(query, ops, createIfMissing, false);
	}


	public <T> UpdateResults<T> update(Query<T> query, UpdateOperations<T> ops) {
		return update(query, ops, false, true);
	}
	

	public <T> UpdateResults<T> updateFirst(Query<T> query, UpdateOperations<T> ops) {
		return update(query, ops, false, false);
	}
	
	public <T> UpdateResults<T> updateFirst(Query<T> query, UpdateOperations<T> ops, boolean createIfMissing) {
		return update(query, ops, createIfMissing, defConcern);
		
	}

	public <T> UpdateResults<T> updateFirst(Query<T> query, UpdateOperations<T> ops, boolean createIfMissing, WriteConcern wc) {
		return update(query, ops, createIfMissing, false, wc);
	}
	

	public <T> UpdateResults<T> updateFirst(Query<T> query, T entity, boolean createIfMissing) {
		Mapper mapr = morphia.getMapper();
		LinkedHashMap<Object, DBObject> involvedObjects = new LinkedHashMap<Object, DBObject>();
		DBObject u = mapr.toDBObject(entity, involvedObjects);
		
		UpdateResults<T> res = update(query, u, createIfMissing, false, defConcern);
		postSaveOperations(entity, u, getCollection(entity), involvedObjects);
		return res;
	}
	
	private <T> void postSaveOperations(Object entity, DBObject dbObj, DBCollection dbColl,
			LinkedHashMap<Object, DBObject> involvedObjects) {
		Mapper mapr = morphia.getMapper();
		MappedClass mc = mapr.getMappedClass(entity);
		
		mapr.updateKeyInfo(entity, dbObj, createCache());
		
		firePostPersistForChildren(involvedObjects, mapr);
		mc.callLifecycleMethods(PostPersist.class, entity, dbObj, mapr);
	}

	@SuppressWarnings("rawtypes")
	private <T> UpdateResults<T> update(Query<T> query, UpdateOperations ops, boolean createIfMissing, boolean multi, WriteConcern wc) {
		DBObject u = ((UpdateOpsImpl) ops).getOps();
		return update(query, u, createIfMissing, multi, wc);		
	}
	
	@SuppressWarnings("rawtypes")
	private <T> UpdateResults<T> update(Query<T> query, UpdateOperations ops, boolean createIfMissing, boolean multi) {
		return update(query, ops, createIfMissing, multi, defConcern);
	}
	
	private <T> UpdateResults<T> update(Query<T> query, DBObject u, boolean createIfMissing, boolean multi, WriteConcern wc) {
		DBCollection dbColl = getCollection(((QueryImpl<T>) query).getEntityClass());
		QueryImpl<T> qImpl= (QueryImpl<T>) query;
		if ( qImpl.getSortObject() != null && qImpl.getSortObject().keySet() != null && !qImpl.getSortObject().keySet().isEmpty())
			throw new QueryException("sorting is not allowed for updates.");
		if ( qImpl.getOffset() > 0)
			throw new QueryException("a query offset is not allowed for updates.");
		if ( qImpl.getLimit() > 0)
			throw new QueryException("a query limit is not allowed for updates.");
		
		DBObject q = qImpl.getQueryObject();
		if (q == null)
			q = new BasicDBObject();

		if (log.isTraceEnabled())
			log.trace("Executing update(" + dbColl.getName() + ") for query: " + q + ", ops: " + u + ", multi: " + multi + ", upsert: " + createIfMissing);

		WriteResult wr;
		if (wc == null)
			wr = dbColl.update(q, u, createIfMissing, multi);
		else
			wr = dbColl.update(q, u, createIfMissing, multi, wc);

		throwOnError(wc, wr);
		
		return new UpdateResults<T>(wr);
	}

	public <T> T findAndDelete(Query<T> query) {
		DBCollection dbColl = getCollection(((QueryImpl<T>) query).getEntityClass());
		QueryImpl<T> qi = ((QueryImpl<T>) query);
		EntityCache cache = createCache();
		
		if (log.isTraceEnabled())
			log.trace("Executing findAndModify(" + dbColl.getName() + ") with delete ...");

		DBObject result = dbColl.findAndModify(qi.getQueryObject(), null, qi.getSortObject(), true, null, false, false);

		T entity = (T) morphia.getMapper().fromDBObject(qi.getEntityClass(), result, cache);
        return entity;
	}

	public <T> T findAndModify(Query<T> q, UpdateOperations<T> ops) {
		return findAndModify(q, ops, false);
	}

	public <T> T findAndModify(Query<T> query, UpdateOperations<T> ops, boolean oldVersion) {
		DBCollection dbColl = getCollection(((QueryImpl<T>) query).getEntityClass());
		QueryImpl<T> qi = ((QueryImpl<T>) query);

		if (log.isTraceEnabled())
			log.info("Executing findAndModify(" + dbColl.getName() + ") with update ");

		DBObject res = dbColl.findAndModify(qi.getQueryObject(), 
											null, 
											qi.getSortObject(), 
											false, 
											((UpdateOpsImpl<T>) ops).getOps(), !oldVersion, 
											false);
		
		if (res == null) 
			return null;
		else
			return (T) morphia.getMapper().fromDBObject(qi.getEntityClass(), res, createCache());
	}
	
	/** Converts a list of keys to refs */
	public static <T> List<DBRef> keysAsRefs(List<Key<T>> keys, Mapper mapr){
		ArrayList<DBRef> refs = new ArrayList<DBRef>(keys.size());
		for(Key<T> key : keys)
			refs.add(key.toRef(mapr));
		return refs;
	}
	
	/** Converts a list of refs to keys */
	@SuppressWarnings("rawtypes")
	public static <T> List<Key<T>> refsToKeys(List<DBRef> refs, Class<T> c) {
		ArrayList<Key<T>> keys = new ArrayList<Key<T>>(refs.size());
		for(DBRef ref : refs) {
			keys.add(new Key(ref));
		}
		return keys;
	}
	
	private EntityCache createCache() {
		Mapper mapper = morphia.getMapper();
		return mapper.createEntityCache();
	}
	
	public WriteConcern getDefaultWriteConcern() {return defConcern;} 
	public void setDefaultWriteConcern(WriteConcern wc) {defConcern = wc;}
}
