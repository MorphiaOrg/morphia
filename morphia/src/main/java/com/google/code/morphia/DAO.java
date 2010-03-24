package com.google.code.morphia;

import com.google.code.morphia.annotations.PostPersist;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.Mongo;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Olafur Gauti Gudmundsson
 */
@SuppressWarnings("unchecked")
public class DAO<T,K extends Serializable> {

    protected final Class<T> entityClass;
    protected final String dbName, collectionName;
    protected final Mongo mongo;
    protected final Morphia morphia;

    public DAO( Class<T> entityClass, Mongo mongo, Morphia morphia, String dbName ) {
        this(entityClass, mongo, morphia, dbName, morphia.getMapper().getCollectionName(entityClass));
    }

    public DAO( Class<T> entityClass, Mongo mongo, Morphia morphia, String dbName, String collectionName ) {
        this.entityClass = entityClass;
        this.mongo = mongo;
        this.morphia = morphia;
        this.dbName = dbName;
        this.collectionName = collectionName;
        this.morphia.map(entityClass);
    }

    protected DBCollection collection() {
        return mongo.getDB(dbName).getCollection(collectionName);
    }

    public void save( T entity ) {
		DBObject dbObj = morphia.toDBObject(entity);
		collection().save(dbObj);
		dbObj.put(Mapper.COLLECTION_NAME_KEY, collectionName);
		morphia.getMapper().updateKeyInfo(entity, dbObj);
        morphia.getMapper().getMappedClass(entity).callLifecycleMethods(PostPersist.class, entity, dbObj);
    }

    public void delete( T entity ) {
        try {
            K id = (K) morphia.getMappedClasses().get(entity.getClass().getName()).idField.get(entity);
            deleteById(id);
        } catch ( Exception e ) {
            throw new RuntimeException(e);
        }
    }

    public void deleteById( K id ) {
        collection().remove(new BasicDBObject(Mapper.ID_KEY, Mapper.asObjectIdMaybe(id)));
    }

    protected void deleteByQuery( DBObject query ) {
        collection().remove(query);
    }

    public T get( K id ) {
        BasicDBObject dbObject = (BasicDBObject) collection().findOne(Mapper.asObjectIdMaybe(id));
        if ( dbObject == null ) {
            return null;
        } else {
            return map(dbObject);
        }
    }

    public List<K> getIds( Map<String,Object> query ) {
        DBCursor cursor = collection().find(new BasicDBObject(query), new BasicDBObject(Mapper.ID_KEY, "1"));
        List<K> ids = new ArrayList<K>();
        while ( cursor.hasNext() ) {
            ids.add((K)cursor.next().get(Mapper.ID_KEY));
        }
        return ids;
    }

    public boolean exists(String key, String value) {
        return exists(new BasicDBObject(key, value));
    }
    public boolean exists(String key, int value) {
        return exists(new BasicDBObject(key, value));
    }
    public boolean exists(String key, long value) {
        return exists(new BasicDBObject(key, value));
    }
    public boolean exists(String key, double value) {
        return exists(new BasicDBObject(key, value));
    }
    public boolean exists(String key, boolean value) {
        return exists(new BasicDBObject(key, value));
    }
    public boolean exists(String key, Enum value) {
        return exists(new BasicDBObject(key, value.name()));
    }
    public boolean exists( Map<String,Object> query ) {
        return getCount(query) > 0;
    }

    public long getCount() {
        return collection().getCount();
    }
    public long getCount(String key, String value) {
        return getCount(new BasicDBObject(key, value));
    }
    public long getCount(String key, int value) {
        return getCount(new BasicDBObject(key, value));
    }
    public long getCount(String key, long value) {
        return getCount(new BasicDBObject(key, value));
    }
    public long getCount(String key, double value) {
        return getCount(new BasicDBObject(key, value));
    }
    public long getCount(String key, boolean value) {
        return getCount(new BasicDBObject(key, value));
    }
    public long getCount(String key, Enum value) {
        return getCount(new BasicDBObject(key, value.name()));
    }
    public long getCount( Map<String,Object> query ) {
        return collection().getCount(new BasicDBObject(query));
    }

    public T findOne(String key, String value) {
        return findOne(new BasicDBObject(key, value));
    }
    public T findOne(String key, int value) {
        return findOne(new BasicDBObject(key, value));
    }
    public T findOne(String key, long value) {
        return findOne(new BasicDBObject(key, value));
    }
    public T findOne(String key, double value) {
        return findOne(new BasicDBObject(key, value));
    }
    public T findOne(String key, boolean value) {
        return findOne(new BasicDBObject(key, value));
    }
    public T findOne(String key, Enum value) {
        return findOne(new BasicDBObject(key, value.name()));
    }
    public T findOne( Map<String,Object> query ) {
        return map((BasicDBObject)collection().findOne(new BasicDBObject(query)));
    }

    public List<T> find(String key, String value) {
        return find(new BasicDBObject(key, value));
    }
    public List<T> find(String key, int value) {
        return find(new BasicDBObject(key, value));
    }
    public List<T> find(String key, long value) {
        return find(new BasicDBObject(key, value));
    }
    public List<T> find(String key, double value) {
        return find(new BasicDBObject(key, value));
    }
    public List<T> find(String key, boolean value) {
        return find(new BasicDBObject(key, value));
    }
    public List<T> find(String key, Enum value) {
        return find(new BasicDBObject(key, value.name()));
    }

    public List<T> find(String key, String value, int startIndex, int resultSize) {
        return find(new BasicDBObject(key, value), startIndex, resultSize);
    }
    public List<T> find(String key, int value, int startIndex, int resultSize) {
        return find(new BasicDBObject(key, value), startIndex, resultSize);
    }
    public List<T> find(String key, long value, int startIndex, int resultSize) {
        return find(new BasicDBObject(key, value), startIndex, resultSize);
    }
    public List<T> find(String key, double value, int startIndex, int resultSize) {
        return find(new BasicDBObject(key, value), startIndex, resultSize);
    }
    public List<T> find(String key, boolean value, int startIndex, int resultSize) {
        return find(new BasicDBObject(key, value), startIndex, resultSize);
    }
    public List<T> find(String key, Enum value, int startIndex, int resultSize) {
        return find(new BasicDBObject(key, value.name()), startIndex, resultSize);
    }

    public List<T> find(Map<String,Object> query) {
        return find(query, -1, -1);
    }
    public List<T> find(Map<String,Object> query, int startIndex, int resultSize) {
        return toList(collection().find(new BasicDBObject(query)), startIndex, resultSize);
    }

    public List<T> findAll() {
        return findAll(-1, -1);
    }
    public List<T> findAll(int startIndex, int resultSize) {
        return toList(collection().find(), startIndex, resultSize);
    }

    public void dropCollection() {
        collection().drop();
    }

    protected T map( BasicDBObject dbObject ) {
        return morphia.fromDBObject(entityClass, dbObject);
    }

    protected List<T> toList( DBCursor cursor ) {
        return toList(cursor, -1, -1);
    }
    
    protected List<T> toList( DBCursor cursor, int startIndex, int resultSize ) {
        if ( startIndex > 0 ) {
            cursor.skip(startIndex);
        }
        if ( resultSize > 0 ) {
            cursor.limit(resultSize);
        }
        List<T> list = new ArrayList<T>();
        while ( cursor.hasNext() ) {
            list.add(map((BasicDBObject) cursor.next()));
        }
        return list;
    }
}
