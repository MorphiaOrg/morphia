package com.google.code.morphia;

import java.util.ArrayList;

import com.google.code.morphia.annotations.PostPersist;
import com.google.code.morphia.utils.IndexDirection;
import com.google.code.morphia.utils.Key;
import com.mongodb.BasicDBObject;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.DBRef;
import com.mongodb.Mongo;

/**
 * 
 * @author Scott Hernandez
 */
@SuppressWarnings("unchecked")
public class DatastoreImpl implements Datastore {

	Morphia morphia;
	Mongo mongo;
	String dbName;

	public DatastoreImpl(Morphia morphia, Mongo mongo) {
		this(morphia, mongo, null);
	}
	
	public DatastoreImpl(Morphia morphia, Mongo mongo, String dbName) {
		this.morphia = morphia; this.mongo = mongo; this.dbName = dbName;
	}

	@Override
	public Mongo getMongo() {
		return this.mongo;
	}

	@Override
	public Morphia getMorphia() {
		return this.morphia;
	}

	@Override
	public DB getDB() {
		return (dbName == null) ? null : mongo.getDB(dbName);
	}
	public DBCollection getCollection(Class clazz) {
		String collName = morphia.getMapper().getCollectionName(clazz);
		return mongo.getDB(dbName).getCollection(collName);
	}
	
	public DBCollection getCollection(Object obj) {
		String collName = morphia.getMapper().getCollectionName(obj);
		return mongo.getDB(dbName).getCollection(collName);
	}

	protected Object asObjectIdMaybe(Object id) {
		return Mapper.asObjectIdMaybe(id);
	}

	private Class getEntityClass(Object clazzOrEntity) {
		return (clazzOrEntity instanceof Class) ? (Class) clazzOrEntity : clazzOrEntity.getClass();
	}

	@Override
	public <T> T get(Object clazzOrEntity, Object id) {
		DBObject query = BasicDBObjectBuilder.start().add(Mapper.ID_KEY, asObjectIdMaybe(id)).get();
		DBObject obj =  getCollection(clazzOrEntity).findOne(query);
		if (obj == null) return null;
		return (T)morphia.fromDBObject(getEntityClass(clazzOrEntity), (BasicDBObject) obj);
	}

	@Override
	public <T> Query<T> get(Object clazzOrEntity, Object[] ids) {
		for (int i = 0; i < ids.length; i++) {
			ids[i] = asObjectIdMaybe(ids[i]);
		}
		return find(clazzOrEntity, Mapper.ID_KEY + " in", ids);
	}

	@Override
	public <T> T get(Object clazzOrEntity, long id) {
		return (T)get(clazzOrEntity, (Object)id);
	}
	
	@Override
	public <T> Query<T> get(Object clazzOrEntity, long[] ids) {
		ArrayList<Long> listIds = new ArrayList<Long>(ids.length);
		
		for (long id: ids) 
			listIds.add(id);
		
		return get(clazzOrEntity, listIds.toArray());
	}	

	@Override
	public <T> T get(Object clazzOrEntity, String id) {
		return (T)get(clazzOrEntity, (Object)id);
	}

	@Override
	public <T> Query<T> get(Object clazzOrEntity, String[] ids) {
		return get(clazzOrEntity, (Object[])ids);
	}

	@Override
	public <T> Query<T> find(Object clazzOrEntity) {
		return new QueryImpl<T>(getEntityClass(clazzOrEntity), getCollection(clazzOrEntity), this);
	}

	@Override
	public <T> void delete(T entity) {
		try {
			Object id = morphia.getMappedClasses().get(entity.getClass().getName()).idField.get(entity);
			delete(entity, id);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public <T> void delete(Object clazzOrEntity, long id) {
		delete(clazzOrEntity, (Object)id);
	}

	@Override
	public <T> void delete(Object clazzOrEntity, String id) {
		delete(clazzOrEntity, (Object)id);
	}
	
	@Override
	public <T> void delete(Object clazzOrEntity, Object id) {
		DBCollection dbColl = getCollection(clazzOrEntity);
		dbColl.remove(BasicDBObjectBuilder.start().add(Mapper.ID_KEY, asObjectIdMaybe(id)).get());
	}
	
	@Override
	public <T> void delete(Object clazzOrEntity, long[] ids) {
		for (long id : ids) {
			delete(clazzOrEntity, id);
		}		
	}

	@Override
	public <T> void delete(Object clazzOrEntity, String[] ids) {
		for (String id : ids)
			delete(clazzOrEntity, id);
	}
	
	@Override
	public <T> Key<T> save(T entity) {
		DBObject dbObj = morphia.toDBObject(entity);
		DBCollection dbColl = getCollection(entity);
		dbColl.save(dbObj);
		dbObj.put(Mapper.COLLECTION_NAME_KEY, dbColl.getName());
		morphia.getMapper().updateKeyInfo(entity, dbObj);
        morphia.getMapper().getMappedClass(entity).callLifecycleMethods(PostPersist.class, entity, dbObj);
		return new Key<T>(dbColl.getName(), dbObj.get(Mapper.ID_KEY));
    }

	
	@Override
	public <T> Iterable<Key<T>> save(Iterable<T> entities) {
		ArrayList<Key<T>> savedKeys = new ArrayList<Key<T>>();
		//for now, do it one at a time.
		for(T ent : entities)
			savedKeys.add(save(ent));
		return savedKeys;

	}

	@Override
	public <T> long getCount(Object clazzOrEntity) {
		return getCollection(clazzOrEntity).getCount();
	}

	@Override
	public <T> Query<T> find(Object clazzOrEntity, String property, Object value) {
		Query<T> query = find(clazzOrEntity);
		return query.filter(property, value);
	}

	@Override
	public <T> Query<T> find(Object clazzOrEntity, String property, Object value, int offset, int size) {
		Query<T> query = find(clazzOrEntity);
		query.offset(offset); query.limit(size);
		return query.filter(property, value);
	}

	@Override
	public <T> long getCount(Query<T> query) {
		return query.countAll();
	}

	protected Object getId(Object entity) {
		MappedClass mc;
		String keyClassName = entity.getClass().getName();
		if (morphia.getMappedClasses().containsKey(keyClassName))
			mc = morphia.getMappedClasses().get(keyClassName);
		else
			mc = new MappedClass(entity.getClass());
		
		try {
			return mc.idField.get(entity);
		} catch (Exception e) {
			return null;
		}
	}
	@Override
	public <T> T get(Object entityOrRef) {
		DBRef ref = null;
		if(entityOrRef instanceof DBRef) ref = (DBRef)entityOrRef;
		
		if(ref==null) {
			Object id = getId(entityOrRef);
			if (id == null) throw new MongoMappingException("Could not get id for " + entityOrRef.getClass().getName());
			return (T) get(entityOrRef, id);
		} else {
			//TODO replace with reflection code?
			return null;
		}
	}

	@Override
	public DBRef createRef(Object entity) {
		Object id = getId(entity);
		if (id == null) throw new MongoMappingException("Could not get id for " + entity.getClass().getName());
		return new DBRef(getDB(), getCollection(entity).getName(), id);
	}

	@Override
	public DBRef createRef(Object clazzOrEntity, Object id) {
		if (id == null) throw new MongoMappingException("Could not get id for " + clazzOrEntity.getClass().getName());
		return new DBRef(getDB(), getCollection(clazzOrEntity).getName(), id);
	}

	@Override
	public void ensureIndex(Object clazzOrEntity, String name, IndexDirection dir) {
		BasicDBObjectBuilder keyBuilder = BasicDBObjectBuilder.start();
		if(dir == IndexDirection.BOTH)
			keyBuilder.add(name, 1).add(name, -1);
		else
			keyBuilder.add(name, (dir == IndexDirection.ASC)? 1 : -1);
		
		getCollection(clazzOrEntity).ensureIndex(keyBuilder.get());
	}

	@Override
	public void ensureSuggestedIndexes() {
		//TODO loop over mappedClasses and call ensureIndex for each one on non-embedded objects (for now)
	}
	
	
}
