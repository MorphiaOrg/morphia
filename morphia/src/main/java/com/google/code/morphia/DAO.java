package com.google.code.morphia;

import com.google.code.morphia.annotations.PostPersist;
import com.google.code.morphia.mapping.MappedClass;
import com.google.code.morphia.mapping.Mapper;
import com.google.code.morphia.query.Sort;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.Mongo;
import java.io.Serializable;
import java.lang.reflect.ParameterizedType;

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
        this.entityClass = entityClass;
        this.mongo = mongo;
        this.morphia = morphia;
        this.dbName = dbName;
        this.collectionName = morphia.getMapper().getCollectionName(entityClass);
        this.morphia.map(entityClass);
    }

    protected DAO( Mongo mongo, Morphia morphia, String dbName ) {
        this.entityClass = ((Class<T>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0]);
        this.mongo = mongo;
        this.morphia = morphia;
        this.dbName = dbName;
        this.collectionName = morphia.getMapper().getCollectionName(entityClass);
        this.morphia.map(entityClass);
    }

    protected DBCollection collection() {
        return mongo.getDB(dbName).getCollection(collectionName);
    }

    public void save( T entity ) {
		DBObject dbObj = morphia.toDBObject(entity);
		collection().save(dbObj);
		morphia.getMapper().updateKeyInfo(entity, dbObj.get(Mapper.ID_KEY) ,collectionName);
        morphia.getMapper().getMappedClass(entity).callLifecycleMethods(PostPersist.class, entity, dbObj);
    }

    /**
     * Updates the first object matched by the constraints with the modifiers supplied.
     */
    public void updateFirst( Constraints c, Modifiers m ) {
        if ( m.getOperations().isEmpty() ) {
            throw new IllegalArgumentException("No modifiers were specified");
        }
        collection().update(new BasicDBObject(c.getQuery()), new BasicDBObject(m.getOperations()));
    }

    /**
     * Updates all objects matched by the constraints with the modifiers supplied.
     */
    public void update( Constraints c, Modifiers m ) {
        if ( m.getOperations().isEmpty() ) {
            throw new IllegalArgumentException("No modifiers were specified");
        }
        collection().update(new BasicDBObject(c.getQuery()), new BasicDBObject(m.getOperations()), false, true);
    }

    public void delete( T entity ) {
        try {
            K id = (K) morphia.getMappedClasses().get(entity.getClass().getName()).getIdField().get(entity);
            deleteById(id);
        } catch ( Exception e ) {
            throw new RuntimeException(e);
        }
    }

    public void deleteById( K id ) {
        deleteMatching(new Constraints(Mapper.ID_KEY, Mapper.asObjectIdMaybe(id)));
    }

    public void deleteMatching( Constraints c ) {
        collection().remove(new BasicDBObject(c.getQuery()));
    }

    protected void deleteByQuery( DBObject query ) {
        collection().remove(query);
    }

    public T get( K id ) {
        BasicDBObject dbObject = (BasicDBObject) collection().findOne(Mapper.asObjectIdMaybe(id));
        return dbObject != null ? map(dbObject) : null;
    }

    public Results<K> findIds( String key, Object value ) {
        return findIds(new Constraints(key, value));
    }
    public Results<K> findIds() {
        return findIds(new Constraints());
    }
    public Results<K> findIds( Constraints c ) {
        DBCursor cursor = collection().find(new BasicDBObject(c.getQuery()), new BasicDBObject(Mapper.ID_KEY, "1"));
        applyToCursor(cursor, c.getStartIndex(), c.getResultSize(), c.getSort());
        return new ResultsImpl<K>(null, cursor, morphia.getMapper(), true);
    }

    public boolean exists(String key, Object value) {
        return exists(new Constraints(key, value));
    }
    public boolean exists(Constraints c) {
        return count(c) > 0;
    }

    public long count() {
        return collection().getCount();
    }
    public long count(String key, Object value) {
        return count(new Constraints(key, value));
    }
    public long count(Constraints c) {
        return collection().getCount(new BasicDBObject(c.getQuery()));
    }

    public T findOne(String key, Object value) {
        return findOne(new Constraints(key, value));
    }
    public T findOne( Constraints c ) {
        if ( c.getFields() != null && !c.getFields().isEmpty() ) {
            BasicDBObject dbObject = (BasicDBObject)collection().findOne(new BasicDBObject(c.getQuery()), new BasicDBObject(c.getFields()));
            return dbObject != null ? map(dbObject) : null;
        } else {
            BasicDBObject dbObject = (BasicDBObject)collection().findOne(new BasicDBObject(c.getQuery()));
            return dbObject != null ? map(dbObject) : null;
        }
    }

    public Results<T> find() {
        return find(new Constraints());
    }

    public Results<T> find( Constraints c ) {
        DBCursor cursor;
        if ( c.getFields() != null && !c.getFields().isEmpty() ) {
            cursor = collection().find(new BasicDBObject(c.getQuery()), new BasicDBObject(c.getFields()));
        } else {
            cursor = collection().find(new BasicDBObject(c.getQuery()));
        }
        applyToCursor(cursor, c.getStartIndex(), c.getResultSize(), c.getSort());
        return new ResultsImpl<T>(entityClass, cursor, morphia.getMapper(), false);
    }

    public void dropCollection() {
        collection().drop();
    }

    protected T map( BasicDBObject dbObject ) {
        return morphia.fromDBObject(entityClass, dbObject);
    }

    protected void applyToCursor( DBCursor cursor, int startIndex, int resultSize, Sort sort ) {
        if ( sort != null && !sort.getFields().isEmpty() ) {
            BasicDBObject orderBy = new BasicDBObject();
            for ( Sort.SortField s : sort.getFields() ) {
                orderBy.put(s.getName(), s.isAscending() ? 1 : -1);
            }
            cursor.sort(orderBy);
        }
        if ( startIndex > 0 ) {
            cursor.skip(startIndex);
        }
        if ( resultSize > 0 ) {
            cursor.limit(resultSize);
        }
    }
}
