package com.google.code.morphia.query;

import com.google.code.morphia.Key;
import com.google.code.morphia.mapping.Mapper;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCursor;

/**
 * 
 * @author Scott Hernandez
 */
public class MorphiaKeyIterator<T> extends MorphiaIterator<T, Key<T>>{
	public MorphiaKeyIterator(DBCursor cursor, Mapper m, Class<T> clazz, String kind) {
		super(cursor, m, clazz, kind, null);
	}

	@Override
	protected Key<T> convertItem(BasicDBObject dbObj) {
		Key<T> key = new Key<T>(kind, dbObj.get(Mapper.ID_KEY));
		key.setKindClass(this.clazz);
		return key;
	}
	
}