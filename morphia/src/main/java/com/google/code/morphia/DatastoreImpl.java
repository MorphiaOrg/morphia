package com.google.code.morphia;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.google.code.morphia.annotations.CappedAt;
import com.google.code.morphia.annotations.Entity;
import com.google.code.morphia.annotations.Index;
import com.google.code.morphia.annotations.Indexed;
import com.google.code.morphia.annotations.Indexes;
import com.google.code.morphia.annotations.PostPersist;
import com.google.code.morphia.annotations.Reference;
import com.google.code.morphia.annotations.Serialized;
import com.google.code.morphia.annotations.Version;
import com.google.code.morphia.logging.Logr;
import com.google.code.morphia.logging.MorphiaLoggerFactory;
import com.google.code.morphia.mapping.MappedClass;
import com.google.code.morphia.mapping.MappedField;
import com.google.code.morphia.mapping.Mapper;
import com.google.code.morphia.mapping.MappingException;
import com.google.code.morphia.mapping.cache.EntityCache;
import com.google.code.morphia.mapping.lazy.DatastoreHolder;
import com.google.code.morphia.mapping.lazy.proxy.ProxyHelper;
import com.google.code.morphia.query.FilterOperator;
import com.google.code.morphia.query.Query;
import com.google.code.morphia.query.QueryException;
import com.google.code.morphia.query.QueryImpl;
import com.google.code.morphia.query.UpdateException;
import com.google.code.morphia.query.UpdateOperations;
import com.google.code.morphia.query.UpdateOpsImpl;
import com.google.code.morphia.query.UpdateResults;
import com.google.code.morphia.utils.Assert;
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
@SuppressWarnings({"unchecked", "deprecation" })
public class DatastoreImpl implements Datastore, AdvancedDatastore {
	private static final Logr log = MorphiaLoggerFactory.get(DatastoreImpl.class);
	
//	final protected Morphia morphia;
	final protected Mapper mapr;
	final protected Mongo mongo;
	final protected DB db;
	protected WriteConcern defConcern = WriteConcern.SAFE;
	
	public DatastoreImpl(Morphia morphia, Mongo mongo) {
		this(morphia, mongo, null);
	}
	
	public DatastoreImpl(Morphia morphia, Mongo mongo, String dbName, String username, char[] password) {
//		this.morphia = morphia;
		this.mapr = morphia.getMapper();
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
	
	@Deprecated
	protected Object getId(Object entity) {
		return mapr.getId(entity);
	}

	@Deprecated // use mapper instead.
	public <T> Key<T> getKey(T entity) {
		return mapr.getKey(entity);
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
		DBCollection dbColl = getCollection(kind);
		delete(dbColl, id, defConcern);
	}
	
	public <T, V> void delete(Class<T> clazz, V id, WriteConcern wc) {
		DBCollection dbColl = getCollection(clazz);
		delete(dbColl, id, wc);
	}

	public <T, V> void delete(Class<T> clazz, V id) {
		delete(clazz, id, getWriteConcern(clazz));
	}

	public <T, V> void delete(Class<T> clazz, Iterable<V> ids) {
		Query<T> q = find(clazz).disableValidation().filter(Mapper.ID_KEY + " in", ids);
		delete(q);
	}
	
	public <T> void delete(T entity) {
		delete(entity, getWriteConcern(entity));
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
		delete(query, getWriteConcern(query.getEntityClass()));
	}
	
	public <T> void delete(Query<T> query, WriteConcern wc) {
		QueryImpl<T> q = (QueryImpl<T>) query;
		DBCollection dbColl = getCollection(q.getEntityClass());
		WriteResult wr;
		
		if (q.getSortObject() != null || q.getOffset() != 0 || q.getLimit() > 0)
			throw new QueryException("Delete does not allow sort/offset/limit query options.");
		
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

	public <T> void ensureIndex(Class<T> type, String fields) {
		ensureIndex(type, null, fields, false, false);
	}

	public <T> void ensureIndex(Class<T> clazz, String name, IndexFieldDef[] defs, boolean unique, boolean dropDupsOnCreate) {
		ensureIndex(clazz, name, defs, unique, dropDupsOnCreate, false);
	}

	public <T> void ensureIndex(Class<T> clazz, String name, String fields, boolean unique, boolean dropDupsOnCreate) {
		ensureIndex(clazz, name, QueryImpl.parseSortString(fields), unique, dropDupsOnCreate, false, false);
	}

	public <T> void ensureIndex(Class<T> clazz, String name, String fields, boolean unique, boolean dropDupsOnCreate, boolean background) {
		ensureIndex(clazz, name, QueryImpl.parseSortString(fields), unique, dropDupsOnCreate, background, false);
	}
	protected <T> void ensureIndex(Class<T> clazz, String name, BasicDBObject fields, boolean unique, boolean dropDupsOnCreate, boolean background, boolean sparse) {
		//validate field names and translate them to the stored values
		BasicDBObject keys = new BasicDBObject();
		for(Entry<String, Object> entry : fields.entrySet()){
			StringBuffer sb = new StringBuffer(entry.getKey());
			Mapper.validate(clazz, mapr, sb, FilterOperator.IN, "", true, false);
			keys.put(sb.toString(), entry.getValue());
		}
		
		BasicDBObjectBuilder keyOpts = new BasicDBObjectBuilder();
		if (name != null && name.length() > 0) {
			keyOpts.add("name", name);
		}
		if (unique) {
			keyOpts.add("unique", true);
			if (dropDupsOnCreate)
				keyOpts.add("dropDups", true);
		}

		if (background)
			keyOpts.add("background", true);
		if (sparse)
			keyOpts.add("sparse", true);
		
		DBCollection dbColl = getCollection(clazz);
		
		BasicDBObject opts = (BasicDBObject) keyOpts.get();
		if (opts.isEmpty()) {
			log.debug("Ensuring index for " + dbColl.getName() + " with keys:" + keys);
			dbColl.ensureIndex(keys);
		} else {
			log.debug("Ensuring index for " + dbColl.getName() + " with keys:" + keys + " and opts:" + opts);
			dbColl.ensureIndex(keys, opts);
		}

		//TODO: remove this once using 2.4 driver does this in ensureIndex
		CommandResult cr = dbColl.getDB().getLastError();
		cr.throwOnError();
	}
	
	@SuppressWarnings({ "rawtypes"})
	public void ensureIndex(Class clazz, String name, IndexFieldDef[] defs, boolean unique, boolean dropDupsOnCreate, boolean background) {
		BasicDBObjectBuilder keys = BasicDBObjectBuilder.start();

		for (IndexFieldDef def : defs) {
			String fieldName = def.getField();
			IndexDirection dir = def.getDirection();
			keys.add(fieldName, dir.toIndexValue());
		}
		
		ensureIndex(clazz, name, (BasicDBObject) keys.get(), unique, dropDupsOnCreate, background, false);
	}
	
	public <T> void ensureIndex(Class<T> type, String name, IndexDirection dir) {
		ensureIndex(type, new IndexFieldDef(name, dir));
	}

	public <T> void ensureIndex(Class<T> type, IndexFieldDef... fields) {
		ensureIndex(type, null, fields, false, false);
	}
	
	public <T> void ensureIndex(Class<T> type, boolean background, IndexFieldDef... fields) {
		ensureIndex(type, null, fields, false, false, background);
	}
	
	protected void ensureIndexes(MappedClass mc, boolean background) {
		ensureIndexes(mc, background, new ArrayList<MappedClass>(), new ArrayList<MappedField>());
	}
	
	protected void ensureIndexes(MappedClass mc, boolean background, ArrayList<MappedClass> parentMCs, ArrayList<MappedField> parentMFs) {
		if (parentMCs.contains(mc))
			return;
		
		//skip embedded types
		if (mc.getEmbeddedAnnotation() != null && (parentMCs == null || parentMCs.isEmpty()))
			return;

		//Ensure indexes from class annotation
		Indexes idxs = (Indexes) mc.getAnnotation(Indexes.class);
		if (idxs != null && idxs.value() != null && idxs.value().length > 0)
			for(Index index : idxs.value()) {
				BasicDBObject fields = QueryImpl.parseSortString(index.value());
				ensureIndex(mc.getClazz(), index.name(), fields, index.unique(), index.dropDups(), index.background() ? index.background() : background, index.sparse() ? index.sparse() : false );
			}

		//Ensure indexes from field annotations, and embedded entities
		for (MappedField mf : mc.getPersistenceFields()) {
			if (mf.hasAnnotation(Indexed.class)) {
				Indexed index = mf.getAnnotation(Indexed.class);
				StringBuilder field = new StringBuilder();
				Class<?> indexedClass = (parentMCs.isEmpty() ? mc : parentMCs.get(0)).getClazz();
				if (!parentMCs.isEmpty())
					for(MappedField pmf : parentMFs)
						field.append(pmf.getNameToStore()).append(".");
				
				field.append(mf.getNameToStore());
				
				ensureIndex(indexedClass, index.name(), new BasicDBObject(field.toString(), index.value().toIndexValue()), index.unique(), index.dropDups(), index.background() ? index.background() : background , index.sparse() ? index.sparse() : false);
			}
			
			if (!mf.isTypeMongoCompatible() && !mf.hasAnnotation(Reference.class) && !mf.hasAnnotation(Serialized.class)) {
				ArrayList<MappedClass> newParentClasses = (ArrayList<MappedClass>) parentMCs.clone();
				ArrayList<MappedField> newParents = (ArrayList<MappedField>) parentMFs.clone();
				newParentClasses.add(mc);
				newParents.add(mf);
				ensureIndexes(mapr.getMappedClass(mf.isSingleValue() ? mf.getType() : mf.getSubClass()), background, newParentClasses, newParents);
			}
		}
	}
	
	public <T> void ensureIndexes(Class<T> clazz) {
		ensureIndexes(clazz, false);
	}
	
	public <T> void ensureIndexes(Class<T> clazz, boolean background) {
		MappedClass mc = mapr.getMappedClass(clazz);
		ensureIndexes(mc, background);
	}

	public void ensureIndexes() {
		ensureIndexes(false);
	}

	public void ensureIndexes(boolean background) {
		// loops over mappedClasses and call ensureIndex for each @Entity object
		// (for now)
		for (MappedClass mc : mapr.getMappedClasses()) {
			ensureIndexes(mc, background);
		}
	}
	

	public void ensureCaps() {
		for (MappedClass mc : mapr.getMappedClasses())
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
		return new QueryImpl<T>(kind, getCollection(kind), this, q);
	}

	public <T> Query<T> find(String kind, Class<T> clazz) {
		return new QueryImpl<T>(clazz, getCollection(kind), this);
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
		return (T)mapr.fromDBObject(clazz, ref.fetch(), createCache());
	}
	

	public <T, V> Query<T> get(Class<T> clazz, Iterable<V> ids) {
		return find(clazz).disableValidation().filter(Mapper.ID_KEY + " in", ids);
	}

	/** Queries the server to check for each DBRef */
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
			List<Object> objIds = new ArrayList<Object>();
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
			Key<T> testKey = mapr.refToKey(ref);
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
			mapr.updateKind(key);
			
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
			List<Object> objIds = new ArrayList<Object>();
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
		String kind = mapr.getCollectionName(clazz);
		String keyKind = mapr.updateKind(key);
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

	public Key<?> exists(Object entityOrKey) {
		entityOrKey = ProxyHelper.unwrap(entityOrKey);
		Key<?> key = getKey(entityOrKey);
		Object id = key.getId();
		if (id == null)
			throw new MappingException("Could not get id for " + entityOrKey.getClass().getName());
		
		String collName = key.getKind();
		if (collName == null)
			collName = getCollection(key.getKindClass()).getName();
		
		return find(collName, key.getKindClass()).filter(Mapper.ID_KEY, key.getId()).getKey();
	}
	
	@SuppressWarnings("rawtypes")
	public DBCollection getCollection(Class clazz) {
		String collName = mapr.getCollectionName(clazz);
		DBCollection dbC = getDB().getCollection(collName);
		return dbC;
	}

	public DBCollection getCollection(Object obj) {
		if (obj == null) return null;
		return getCollection(obj.getClass());
	}
	
	protected DBCollection getCollection(String kind) {
		if (kind == null) return null;
		return getDB().getCollection(kind);
	}

	public <T> long getCount(T entity) {
		entity = ProxyHelper.unwrap(entity);
		return getCollection(entity).getCount();
	}
	

	public <T> long getCount(Class<T> clazz) {
		return getCollection(clazz).getCount();
	}
	

	public long getCount(String kind) {
		return getCollection(kind).getCount();
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
	
	public Mapper getMapper() {
		return mapr;
	}
	
	public <T> Iterable<Key<T>> insert(Iterable<T> entities) {
		//TODO: try not to create two iterators...
		Object first = entities.iterator().next();
		return insert(entities, getWriteConcern(first));
	}
	
	public <T> Iterable<Key<T>> insert(Iterable<T> entities, WriteConcern wc) {
		ArrayList<DBObject> ents = entities instanceof List ? new ArrayList<DBObject>(((List<T>)entities).size()) : new ArrayList<DBObject>();

		Map<Object, DBObject> involvedObjects = new LinkedHashMap<Object, DBObject>();
		T lastEntity = null;
		for (T ent : entities) {
			ents.add(entityToDBObj(ent, involvedObjects));
			lastEntity = ent;
		}
		
		DBCollection dbColl = getCollection(lastEntity);
		WriteResult wr = null;
		
		DBObject[] dbObjs = new DBObject[ents.size()];
		dbColl.insert(ents.toArray(dbObjs), wc);
		
		throwOnError(wc, wr);
		
		ArrayList<Key<T>> savedKeys = new ArrayList<Key<T>>();
		Iterator<T> entitiesIT = entities.iterator();
		Iterator<DBObject> dbObjsIT = ents.iterator();
		
		while (entitiesIT.hasNext()) {
			T entity = entitiesIT.next();
			DBObject dbObj = dbObjsIT.next();
			savedKeys.add(postSaveGetKey(entity, dbObj, dbColl, involvedObjects));
		}
		
		return savedKeys;
	}

	public <T> Iterable<Key<T>> insert(T...entities) {
		return insert(Arrays.asList(entities), getWriteConcern(entities[0]));
	}
	
	public <T> Key<T> insert(T entity) {
		return insert(entity, getWriteConcern(entity));
	}
	public <T> Key<T> insert(T entity, WriteConcern wc) {
		entity = ProxyHelper.unwrap(entity);
		DBCollection dbColl = getCollection(entity);
		return insert(dbColl, entity, wc);
	}

	public <T> Key<T> insert(String kind, T entity) {
		entity = ProxyHelper.unwrap(entity);
		DBCollection dbColl = getCollection(kind);
		return insert(dbColl, entity, getWriteConcern(entity));
	}

	public <T> Key<T> insert(String kind, T entity, WriteConcern wc) {
		entity = ProxyHelper.unwrap(entity);
		DBCollection dbColl = getCollection(kind);
		return insert(dbColl, entity, wc);
	}
	
	protected <T> Key<T> insert(DBCollection dbColl, T entity, WriteConcern wc) {
		LinkedHashMap<Object, DBObject> involvedObjects = new LinkedHashMap<Object, DBObject>();
		DBObject dbObj = entityToDBObj(entity, involvedObjects);
		WriteResult wr;
		if (wc == null)
			wr = dbColl.insert(dbObj);
		else
			wr = dbColl.insert(dbObj, wc);

		throwOnError(wc, wr);

		return postSaveGetKey(entity, dbObj, dbColl, involvedObjects);

	}

	protected DBObject entityToDBObj(Object entity, Map<Object, DBObject> involvedObjects) {
		entity = ProxyHelper.unwrap(entity);
		DBObject dbObj = mapr.toDBObject(entity, involvedObjects);
		return dbObj;
	}
	
	/** call postSaveOperations and returns Key for entity */
	protected <T> Key<T> postSaveGetKey(T entity, DBObject dbObj, DBCollection dbColl, Map<Object, DBObject> involvedObjects){
		if (dbObj.get(Mapper.ID_KEY) == null)
			throw new MappingException("Missing _id after save!");
		
		postSaveOperations(entity, dbObj, dbColl, involvedObjects);
		Key<T> key = new Key<T>(dbColl.getName(), getId(entity));
		key.setKindClass((Class<? extends T>) entity.getClass());
		
		return key;		
	}
	
	public <T> Iterable<Key<T>> save(Iterable<T> entities) {
		Object first = null;
		try {
			first = entities.iterator().next();
		} catch (Exception e) {
			//do nothing
		}
		return save(entities, getWriteConcern(first));
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
		MappedClass mc = mapr.getMappedClass(entity);
		
		WriteResult wr = null;
		
		//involvedObjects is used not only as a cache but also as a list of what needs to be called for life-cycle methods at the end.
		LinkedHashMap<Object, DBObject> involvedObjects = new LinkedHashMap<Object, DBObject>();
		DBObject dbObj = entityToDBObj(entity, involvedObjects);

		//try to do an update if there is a @Version field
		wr = tryVersionedUpdate(dbColl, entity, dbObj, wc, db, mc);
		
		if(wr == null)
			if (wc == null)
				wr = dbColl.save(dbObj);
			else
				wr = dbColl.save(dbObj, wc);

		throwOnError(wc, wr);
		return postSaveGetKey(entity, dbObj, dbColl, involvedObjects);
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
			if (cr != null && cr.getErrorMessage() != null && cr.getErrorMessage().length() > 0)
				cr.throwOnError();
		}
	}
	private void firePostPersistForChildren(Map<Object, DBObject> involvedObjects) {
		for (Map.Entry<Object, DBObject> e : involvedObjects.entrySet()) {
			Object entity = e.getKey();
			DBObject dbObj = e.getValue();
			MappedClass mc = mapr.getMappedClass(entity);
			mc.callLifecycleMethods(PostPersist.class, entity, dbObj, mapr);
		}
	}
	
	public <T> Key<T> save(String kind, T entity) {
		entity = ProxyHelper.unwrap(entity);
		DBCollection dbColl = getCollection(kind);
		return save(dbColl, entity, getWriteConcern(entity));
	}
	
	public <T> Key<T> save(T entity) {
		return save(entity, getWriteConcern(entity));
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
		return update(query, ops, createIfMissing, getWriteConcern(query.getEntityClass()));
	}
	
	public <T> UpdateResults<T> update(Query<T> query, UpdateOperations<T> ops, boolean createIfMissing, WriteConcern wc) {
		return update(query, ops, createIfMissing, false);
	}

	public <T> UpdateResults<T> update(T ent, UpdateOperations<T> ops) {
		MappedClass mc = mapr.getMappedClass(ent);
		Query<T> q = (Query<T>) createQuery(mc.getClazz());
		q.disableValidation().filter(Mapper.ID_KEY, getId(ent));
		
		if (mc.getFieldsAnnotatedWith(Version.class).size() > 0) {
			MappedField versionMF = mc.getFieldsAnnotatedWith(Version.class).get(0);
			Long oldVer = (Long)versionMF.getFieldValue(ent);
			q.filter(versionMF.getNameToStore(), oldVer);
			ops.set(versionMF.getNameToStore(), VersionHelper.nextValue(oldVer));
		}

		return update(q, ops);
	}

	public <T> UpdateResults<T> update(Key<T> key, UpdateOperations<T> ops) {
		Class<T> clazz = (Class<T>) key.getKindClass();
		if (clazz == null)
			clazz = (Class<T>) mapr.getClassFromKind(key.getKind());
		return updateFirst(createQuery(clazz).disableValidation().filter(Mapper.ID_KEY, key.getId()), ops);
	}

	public <T> UpdateResults<T> update(Query<T> query, UpdateOperations<T> ops) {
		return update(query, ops, false, true);
	}
	

	public <T> UpdateResults<T> updateFirst(Query<T> query, UpdateOperations<T> ops) {
		return update(query, ops, false, false);
	}
	
	public <T> UpdateResults<T> updateFirst(Query<T> query, UpdateOperations<T> ops, boolean createIfMissing) {
		return update(query, ops, createIfMissing, getWriteConcern(query.getEntityClass()));
		
	}

	public <T> UpdateResults<T> updateFirst(Query<T> query, UpdateOperations<T> ops, boolean createIfMissing, WriteConcern wc) {
		return update(query, ops, createIfMissing, false, wc);
	}
	
	public <T> UpdateResults<T> updateFirst(Query<T> query, T entity, boolean createIfMissing) {
		LinkedHashMap<Object, DBObject> involvedObjects = new LinkedHashMap<Object, DBObject>();
		DBObject dbObj = mapr.toDBObject(entity, involvedObjects);
		
		UpdateResults<T> res = update(query, dbObj, createIfMissing, false, getWriteConcern(entity));
		
		//update _id field
		CommandResult gle = res.getWriteResult().getCachedLastError();
		if(gle != null && res.getInsertedCount() > 0)
			dbObj.put(Mapper.ID_KEY, res.getNewId());

		postSaveOperations(entity, dbObj, getCollection(entity), involvedObjects);
		return res;
	}
	
	public <T> Key<T> merge(T entity) {
		return merge(entity, getWriteConcern(entity));
	}

	public <T> Key<T> merge(T entity, WriteConcern wc) {
		LinkedHashMap<Object, DBObject> involvedObjects = new LinkedHashMap<Object, DBObject>();
		DBObject dbObj = mapr.toDBObject(entity, involvedObjects);
		Key<T> key = getKey(entity);
		entity = ProxyHelper.unwrap(entity);
		Object id = getId(entity);
		if (id == null)
			throw new MappingException("Could not get id for " + entity.getClass().getName());
		Query<T> query = (Query<T>) createQuery(entity.getClass()).filter(Mapper.ID_KEY, id);

		//remove (immutable) _id field for update.
		dbObj.removeField(Mapper.ID_KEY);
		UpdateResults<T> res = update(query, new BasicDBObject("$set", dbObj), false, false, wc);
		
		//check for updated count if we have a gle
		CommandResult gle = res.getWriteResult().getCachedLastError();
		if(gle != null && res.getUpdatedCount() == 0)
			throw new UpdateException("Not updated: " + gle);

		postSaveOperations(entity, dbObj, getCollection(entity), involvedObjects);
		return key;
	}
	
	private <T> void postSaveOperations(Object entity, DBObject dbObj, DBCollection dbColl,
			Map<Object, DBObject> involvedObjects) {
		MappedClass mc = mapr.getMappedClass(entity);
		
		mapr.updateKeyInfo(entity, dbObj, createCache());
		
		firePostPersistForChildren(involvedObjects);
		mc.callLifecycleMethods(PostPersist.class, entity, dbObj, mapr);
	}

	@SuppressWarnings("rawtypes")
	private <T> UpdateResults<T> update(Query<T> query, UpdateOperations ops, boolean createIfMissing, boolean multi, WriteConcern wc) {
		DBObject u = ((UpdateOpsImpl) ops).getOps();
		if (((UpdateOpsImpl) ops).isIsolated()) {
			Query<T> q = query.clone();
			q.disableValidation().filter("$atomic", true);
			return update(q, u, createIfMissing, multi, wc);		
		}
		return update(query, u, createIfMissing, multi, wc);		
	}
	
	@SuppressWarnings("rawtypes")
	private <T> UpdateResults<T> update(Query<T> query, UpdateOperations ops, boolean createIfMissing, boolean multi) {
		return update(query, ops, createIfMissing, multi, getWriteConcern(query.getEntityClass()));
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

		DBObject result = dbColl.findAndModify(qi.getQueryObject(), qi.getFieldsObject(), qi.getSortObject(), true, null, false, false);

		if (result != null) {
			T entity = (T) mapr.fromDBObject(qi.getEntityClass(), result, cache);
	        return entity;
		}
		
		return null;
	}

	public <T> T findAndModify(Query<T> q, UpdateOperations<T> ops) {
		return findAndModify(q, ops, false);
	}

	public <T> T findAndModify(Query<T> query, UpdateOperations<T> ops, boolean oldVersion) {
		return findAndModify(query,ops,oldVersion, false); 
	}
	
	public <T> T findAndModify(Query<T> query, UpdateOperations<T> ops, boolean oldVersion, boolean createIfMissing) {
		QueryImpl<T> qi = ((QueryImpl<T>) query);
		DBCollection dbColl = getCollection((qi).getEntityClass());

		if (log.isTraceEnabled())
			log.info("Executing findAndModify(" + dbColl.getName() + ") with update ");

		DBObject res = dbColl.findAndModify(qi.getQueryObject(), 
											qi.getFieldsObject(), 
											qi.getSortObject(), 
											false, 
											((UpdateOpsImpl<T>) ops).getOps(), !oldVersion, 
											createIfMissing);
		
		if (res == null) 
			return null;
		else
			return (T) mapr.fromDBObject(qi.getEntityClass(), res, createCache());
	}
	
	@SuppressWarnings("rawtypes")
	public <T> MapreduceResults<T> mapReduce(MapreduceType type, Query query, String map, String reduce, String finalize, Map<String, Object> scopeFields, Class<T> outputType) {
		Assert.parametersNotNull("map", map); Assert.parameterNotEmpty(map, "map");
		Assert.parametersNotNull("reduce", reduce);	Assert.parameterNotEmpty(reduce, "reduce");

		QueryImpl<T> qi = ((QueryImpl<T>) query);
		DBCollection dbColl = getCollection((qi).getEntityClass());

		if (log.isTraceEnabled())
			log.info("Executing mapReduce(" + dbColl.getName() + ") with query(" + qi.toString() 
					+ ") map(" + map +") reduce(" + reduce + ") finalize(" + finalize + ") scope(" + scopeFields +")");

		//TODO replace this with the 2.4 driver impl.
		String outColl = mapr.getCollectionName(outputType);
		BasicDBObjectBuilder bldr = BasicDBObjectBuilder.start("mapreduce", mapr.getCollectionName(qi.getEntityClass()));

		switch (type) {
		case REDUCE:
			bldr.push("out").add("reduce",outColl).pop();
			break;
		case MERGE:
			bldr.push("out").add("merge",outColl).pop();
			break;
		case INLINE:
			bldr.push("out").add("inline",1).pop();
			break;
		default:
			bldr.add("out", outColl);
			break;
		}

		if (qi.getOffset() != 0 || qi.getFieldsObject() != null)
			throw new QueryException("mapReduce does not allow the offset/retrievedFields query options.");

		if(qi.getQueryObject() != null)
			bldr.add("query", qi.getQueryObject());
		if(qi.getLimit() > 0)
			bldr.add("limit", qi.getLimit());
		if(qi.getSortObject() != null)
			bldr.add("sort", qi.getSortObject());
		
		bldr.add("map", map);
		bldr.add("reduce", reduce);
		
		if(finalize != null && finalize.length() > 0)
			bldr.add("finalize", finalize);
		
		if(scopeFields != null && scopeFields.size() > 0)
			bldr.add("scope", mapr.toMongoObject(null, null, scopeFields));
		
		DBObject dbObj = bldr.get();
		CommandResult cr = dbColl.getDB().command(dbObj);
		cr.throwOnError();
		MapreduceResults mrRes = (MapreduceResults) mapr.fromDBObject(MapreduceResults.class, cr, createCache());
		
		QueryImpl baseQ = null;
		if (!MapreduceType.INLINE.equals(type))
			baseQ = new QueryImpl(outputType, db.getCollection(mrRes.getOutputCollectionName()), this);
		//TODO Handle inline case and create an iterator/able.
		
		mrRes.setBits(type, baseQ);
		return mrRes;
	}

	/** Converts a list of keys to refs */
	public static <T> List<DBRef> keysAsRefs(List<Key<T>> keys, Mapper mapr){
		ArrayList<DBRef> refs = new ArrayList<DBRef>(keys.size());
		for(Key<T> key : keys)
			refs.add(mapr.keyToRef(key));
		return refs;
	}
	
	/** Converts a list of refs to keys */
	public static <T> List<Key<T>> refsToKeys(Mapper mapr, List<DBRef> refs, Class<T> c) {
		ArrayList<Key<T>> keys = new ArrayList<Key<T>>(refs.size());
		for(DBRef ref : refs) {
			keys.add((Key<T>)mapr.refToKey(ref));
		}
		return keys;
	}
	
	private EntityCache createCache() {
		return mapr.createEntityCache();
	}
	/** Gets the write concern for entity or returns the default write concern for this datastore */
	public WriteConcern getWriteConcern(Object clazzOrEntity) {
		WriteConcern wc = null;
		if (clazzOrEntity != null) {
			Entity entityAnn = getMapper().getMappedClass(clazzOrEntity).getEntityAnnotation();
			if(entityAnn != null && entityAnn.concern() != null && entityAnn.concern() != "" )
				wc = WriteConcern.valueOf(entityAnn.concern());
		}
		
		if (wc == null)
			wc = defConcern;
		
		return wc;
	}
	
	public WriteConcern getDefaultWriteConcern() {return defConcern;} 
	public void setDefaultWriteConcern(WriteConcern wc) {defConcern = wc;}
}
