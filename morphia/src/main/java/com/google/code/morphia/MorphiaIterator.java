package com.google.code.morphia;

import java.util.Iterator;
import java.util.NoSuchElementException;

import com.mongodb.BasicDBObject;

/**
 * 
 * @author Scott Hernandez
 */
@SuppressWarnings("unchecked")
public class MorphiaIterator<T> implements Iterable<T>, Iterator<T>{
	Iterator wrapped;
	Morphia m;
	Class<T> clazz;

	public MorphiaIterator(Iterator it, Morphia m, Class<T> clazz) {
		this.wrapped = it; this.m = m; this.clazz = clazz;
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
		return (T) m.fromDBObject(clazz, (BasicDBObject) wrapped.next());
	}
	
	@Override
	public void remove() {
		wrapped.remove();
	}
}
