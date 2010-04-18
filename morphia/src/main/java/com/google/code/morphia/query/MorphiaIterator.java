package com.google.code.morphia.query;

import java.util.Iterator;
import java.util.NoSuchElementException;

import com.google.code.morphia.mapping.Mapper;
import com.mongodb.BasicDBObject;

/**
 * 
 * @author Scott Hernandez
 */
@SuppressWarnings("unchecked")
public class MorphiaIterator<T> implements Iterable<T>, Iterator<T>{
	Iterator wrapped;
	Mapper m;
	Class<T> clazz;
	String kind;

	public MorphiaIterator(Iterator it, Mapper m, Class<T> clazz, String kind) {
		this.wrapped = it; this.m = m; this.clazz = clazz;this.kind = kind;
	}
	
	@Override
	public Iterator<T> iterator() {
		return this;
	}
	
	@Override
	public boolean hasNext() {
		if(wrapped == null) return false;
		return wrapped.hasNext();
	}
	
	@Override
	public T next() {
		if(!hasNext()) throw new NoSuchElementException();
		T entity = (T) m.fromDBObject(clazz, (BasicDBObject) wrapped.next());
		m.updateKeyInfo(entity, null, kind);
		return (T) entity;
	}
	
	@Override
	public void remove() {
		wrapped.remove();
	}
}