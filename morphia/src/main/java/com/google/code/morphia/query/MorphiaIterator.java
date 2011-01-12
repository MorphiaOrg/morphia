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
public class MorphiaIterator<T, V> implements Iterable<V>, Iterator<V>{
	private final DBCursor wrapped;
	private final Mapper m;
	private final Class<T> clazz;
//	private final String kind;
	private final EntityCache cache;
	private long  driverTime = 0;
	private long  mapperTime= 0;

	public MorphiaIterator(DBCursor it, Mapper m, Class<T> clazz, String kind, EntityCache cache) {
		this.wrapped = it;
		this.m = m;
		this.clazz = clazz;
//		this.kind = kind;
		this.cache = cache;
	}
	
	public Iterator<V> iterator() {
		return this;
	}
	
	public boolean hasNext() {
		if(wrapped == null) return false;
    	long start = System.currentTimeMillis();
		boolean ret = wrapped.hasNext();
    	driverTime += System.currentTimeMillis() - start;
		return ret;
	}
	
	public V next() {
		if(!hasNext()) throw new NoSuchElementException();
    	BasicDBObject dbObj = getNext();
    	return processItem(dbObj);
	}
	
	protected V processItem(BasicDBObject dbObj) {
    	long start = System.currentTimeMillis();
		V entity = (V) m.fromDBObject(clazz, dbObj, cache);
    	mapperTime += System.currentTimeMillis() - start;
		return (V) entity;
	}
	
	protected BasicDBObject getNext() {
		long start = System.currentTimeMillis();
		BasicDBObject dbObj = (BasicDBObject) wrapped.next();
    	driverTime += System.currentTimeMillis() - start;
    	return dbObj;
	}
	
	public void remove() {
		long start = System.currentTimeMillis();
		wrapped.remove();
    	driverTime += System.currentTimeMillis() - start;
	}
	
	/** Returns the time spent calling the driver in ms */
	public long getDriverTime() {
		return driverTime;
	}
	
	/** Returns the time spent calling the mapper in ms */
	public long getMapperTime() {
		return mapperTime;
	}
	
	public void close() {
		if (wrapped != null)
			wrapped.close();
	}
}