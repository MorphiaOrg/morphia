package com.google.code.morphia.query;

import java.util.Iterator;
import java.util.NoSuchElementException;

import com.google.code.morphia.mapping.Mapper;
import com.google.code.morphia.mapping.cache.EntityCache;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCursor;

/**
 * 
 * @author Scott Hernandez
 */
@SuppressWarnings("unchecked")
public class MorphiaIterator<T> implements Iterable<T>, Iterator<T>{
	private final DBCursor wrapped;
	private final Mapper m;
	private final Class<T> clazz;
	private final String kind;
	private final EntityCache cache;

	public MorphiaIterator(DBCursor it, Mapper m, Class<T> clazz, String kind, EntityCache cache) {
		this.wrapped = it;
		this.m = m;
		this.clazz = clazz;
		this.kind = kind;
		this.cache = cache;
	}
	
	public Iterator<T> iterator() {
		return this;
	}
	
	public boolean hasNext() {
		if(wrapped == null) return false;
		return wrapped.hasNext();
	}
	
	public T next() {
		if(!hasNext()) throw new NoSuchElementException();
		T entity = (T) m.fromDBObject(clazz, (BasicDBObject) wrapped.next(), cache);
		return (T) entity;
	}
	
	public void remove() {
		wrapped.remove();
	}
}