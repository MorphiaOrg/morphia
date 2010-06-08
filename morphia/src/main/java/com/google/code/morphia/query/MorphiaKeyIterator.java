package com.google.code.morphia.query;

import java.util.Iterator;
import java.util.NoSuchElementException;

import com.google.code.morphia.Key;
import com.google.code.morphia.mapping.Mapper;
import com.mongodb.BasicDBObject;

/**
 * 
 * @author Scott Hernandez
 */
@SuppressWarnings("unchecked")
public class MorphiaKeyIterator<T> implements Iterable<Key<T>>, Iterator<Key<T>>{
	Iterator wrapped;
	Mapper m;
	Class<T> clazz;
	String kind;
	
	public MorphiaKeyIterator(Iterator it, Mapper m, Class<T> clazz, String kind) {
		this.wrapped = it; this.m = m; this.clazz = clazz;this.kind = kind;
	}
	
	public Iterator<Key<T>> iterator() {
		return this;
	}
	
	public boolean hasNext() {
		if(wrapped == null) return false;
		return wrapped.hasNext();
	}
	
	public Key<T> next() {
		if(!hasNext()) throw new NoSuchElementException();
		BasicDBObject dbObj = (BasicDBObject) wrapped.next();
		Key<T> key = new Key<T>(kind, dbObj.get(Mapper.ID_KEY));
		key.setKindClass(this.clazz);
		return key;
	}
	
	public void remove() {
		wrapped.remove();
	}
}