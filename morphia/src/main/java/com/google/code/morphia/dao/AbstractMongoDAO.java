/**
 * Copyright (C) 2010 Olafur Gauti Gudmundsson
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.code.morphia.dao;

import com.google.code.morphia.Mapper;
import com.google.code.morphia.Morphia;
import com.google.code.morphia.annotations.PostPersist;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.ObjectId;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Olafur Gauti Gudmundsson
 */
@SuppressWarnings("unchecked")
public abstract class AbstractMongoDAO<T> implements MongoDAO<T> {

    private final Class<T> entityClass;
    private final Morphia morphia;

    public AbstractMongoDAO( Class<T> entityClass, Morphia morphia ) {
        this.entityClass = entityClass;
        this.morphia = morphia;
    }

    protected abstract DBCollection collection();

    @Override
    public long getCount() {
        return collection().getCount();
    }

    @Override
    public void removeById(String id) {
        collection().remove(new BasicDBObject("_id", new ObjectId(id)));
    }

    @Override
    public T save(T entity) {
        BasicDBObject obj = (BasicDBObject) morphia.toDBObject(entity);
        obj.put(Mapper.COLLECTION_NAME_KEY, collection().getName());
        collection().save(obj);
        morphia.getMapper().getMappedClass(entity).callLifecycleMethods(PostPersist.class, entity, obj);
        return get(obj.get("_id").toString());
    }

    @Override
    public boolean exists(String key, String value) {
        return exists(new BasicDBObject(key, value));
    }

    @Override
    public boolean exists(String key, int value) {
        return exists(new BasicDBObject(key, value));
    }

    @Override
    public boolean exists(String key, long value) {
        return exists(new BasicDBObject(key, value));
    }

    @Override
    public boolean exists(String key, double value) {
        return exists(new BasicDBObject(key, value));
    }

    @Override
    public boolean exists(String key, boolean value) {
        return exists(new BasicDBObject(key, value));
    }

	@Override
    public boolean exists(String key, Enum value) {
        return exists(new BasicDBObject(key, value.name()));
    }

    protected boolean exists(final BasicDBObject condition) {
        return collection().getCount(condition) > 0;
    }

    protected T get(final BasicDBObject condition) {
        return morphia.fromDBObject(this.entityClass, (BasicDBObject) this.collection().findOne(condition));
    }

    @Override
    public T get(String id) {
        return morphia.fromDBObject(entityClass, (BasicDBObject) collection().findOne(new ObjectId(id)));
    }

    @Override
    public T getByValue(String key, String value) {
        return get(new BasicDBObject(key, value));
    }

    @Override
    public T getByValue(String key, int value) {
        return get(new BasicDBObject(key, value));
    }

    @Override
    public T getByValue(String key, long value) {
        return get(new BasicDBObject(key, value));
    }

    @Override
    public T getByValue(String key, double value) {
        return get(new BasicDBObject(key, value));
    }

    @Override
    public T getByValue(String key, boolean value) {
        return get(new BasicDBObject(key, value));
    }

    @Override
    public T getByValue(String key, Enum value) {
        return get(new BasicDBObject(key, value.name()));
    }

    @Override
    public void dropCollection() {
        collection().drop();
    }

    @Override
    public List<T> findAll(int startIndex, int resultSize) {
        DBCursor cursor = collection().find();
        if ( startIndex > 0 ) {
            cursor.skip(startIndex);
        }
        cursor.limit(resultSize);
        return toList(cursor);
    }

    protected T map( BasicDBObject dbObject ) {
        return morphia.fromDBObject(entityClass, dbObject);
    }

    protected List<T> toList( DBCursor cursor ) {
        List<T> list = new ArrayList<T>();
        while ( cursor.hasNext() ) {
            list.add(morphia.fromDBObject(entityClass, (BasicDBObject) cursor.next()));
        }
        return list;
    }

}
