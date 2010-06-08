package com.google.code.morphia.query;

import java.util.Iterator;
import java.util.NoSuchElementException;

import com.google.code.morphia.mapping.Mapper;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCursor;

/**
 * 
 * @author Scott Hernandez
 */
@SuppressWarnings("unchecked")
public class MorphiaIterator<T> implements Iterable<T>, Iterator<T>{
	DBCursor wrapped;
	Mapper m;
	Class<T> clazz;
	String kind;

	public MorphiaIterator(DBCursor it, Mapper m, Class<T> clazz, String kind) {
		this.wrapped = it; this.m = m; this.clazz = clazz;this.kind = kind;
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
		T entity = (T) m.fromDBObject(clazz, (BasicDBObject) wrapped.next());
		return (T) entity;
	}
	
	public void remove() {
		wrapped.remove();
	}
}