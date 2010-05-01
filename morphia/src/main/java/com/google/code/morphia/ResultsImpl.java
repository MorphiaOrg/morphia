package com.google.code.morphia;

import com.google.code.morphia.mapping.Mapper;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCursor;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 *
 * @author Olafur Gauti Gudmundsson
 */
public class ResultsImpl<T> implements Results<T> {

    protected final Class<T> entityClass;
    protected final DBCursor cursor;
    protected final Mapper mapper;
    protected final boolean idsOnly;

    ResultsImpl( Class<T> entityClass, DBCursor cursor, Mapper mapper, boolean idsOnly ) {
        this.entityClass = entityClass;
        this.cursor = cursor;
        this.mapper = mapper;
        this.idsOnly = idsOnly;
    }

    @Override
    public List<T> asList() {
        List<T> list = new ArrayList<T>();
        while ( hasNext() ) {
            list.add(next());
        }
        return list;
    }

    @Override
    public Iterator<T> iterator() {
        return this;
    }

    @Override
    public boolean hasNext() {
		if ( cursor == null ) {
            return false;
        } else {
            return cursor.hasNext();
        }
    }

    @SuppressWarnings("unchecked")
	@Override
    public T next() {
        if ( !hasNext() ) {
            throw new NoSuchElementException();
        }
        if ( idsOnly ) {
            return (T) cursor.next().get(Mapper.ID_KEY);
        } else {
            return (T) mapper.fromDBObject(entityClass, (BasicDBObject) cursor.next());
        }
    }

    @Override
    public void remove() {
        cursor.remove();
    }

}
