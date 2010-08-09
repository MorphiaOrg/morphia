/**
 * 
 */
package com.google.code.morphia.query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.code.morphia.mapping.Mapper;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

/**
 * 
 * @author Scott Hernandez
 */
public class UpdateOpsImpl<T> implements UpdateOperations<T> {
	Map<String, Map<String, Object>> ops = new HashMap<String, Map<String, Object>>();
	Mapper mapr;
	Class<T> clazz;
	
	public UpdateOpsImpl(Class<T> type, Mapper mapper) {
		this.mapr = mapper;
		this.clazz = type;
	}
	
	@SuppressWarnings("unchecked")
	public void setOps(DBObject ops) {
		this.ops = (Map<String, Map<String, Object>>) ops;
	}
	public DBObject getOps() {
		return new BasicDBObject(ops);
	}
	protected void add(String op, String f, Object val) {
		if (!ops.containsKey(op)) {
			ops.put(op, new HashMap<String, Object>());
		}
		ops.get(op).put(f,val);
		
	}

	public UpdateOperations<T> add(String fieldExpr, Object value) {
		Object dbObj = mapr.toMongoObject(value);
		add("$addToSet", fieldExpr, dbObj);
		return this;
	}


	public UpdateOperations<T> add(String fieldExpr, Object value, boolean addDups) {
		Object dbObj = mapr.toMongoObject(value);
		add((addDups) ? "$push" : "$addToSet", fieldExpr, dbObj);
		return this;
	}

	public UpdateOperations<T> addAll(String fieldExpr, List<?> values, boolean addDups) {
		List<?> convertedValues = (List<?>)mapr.toMongoObject(values);
		if(addDups)
			add("$pushAll", fieldExpr, convertedValues);
		else
			add("$addToSet", fieldExpr, new BasicDBObject("$each", convertedValues));
		return this;
	}

	public UpdateOperations<T> dec(String fieldExpr) {
		return inc(fieldExpr, -1);
	}


	public UpdateOperations<T> inc(String fieldExpr) {
		return inc(fieldExpr, 1);
	}


	public UpdateOperations<T> inc(String fieldExpr, Number value) {
		add("$inc", fieldExpr, value);
		return this;
	}


	protected UpdateOperations<T> remove(String fieldExpr, boolean firstNotLast) {
		add("$pop", fieldExpr, (firstNotLast) ? -1 : 1 );
		return this;
	}


	public UpdateOperations<T> removeAll(String fieldExpr, Object value) {
		Object dbObj = mapr.toMongoObject(value);
		add("$pull", fieldExpr, dbObj);
		return this;
	}


	public UpdateOperations<T> removeAll(String fieldExpr, List<?> values) {
		List<Object> vals = toDBObjList(values);
		add("$pullAll", fieldExpr, vals);
		return this;
	}


	public UpdateOperations<T> removeFirst(String fieldExpr) {
		return remove(fieldExpr, true);
	}


	public UpdateOperations<T> removeLast(String fieldExpr) {
		return remove(fieldExpr, false);
	}

	public UpdateOperations<T> set(String fieldExpr, Object value) {
		Object dbObj = mapr.toMongoObject(value);
		add("$set", fieldExpr, dbObj);
		return this;
	}


	public UpdateOperations<T> unset(String fieldExpr) {
		add("$unset", fieldExpr, 1);
		return this;
	}
	
	protected List<Object> toDBObjList(List<?> values){
		ArrayList<Object> vals = new ArrayList<Object>((int) (values.size()*1.3));
		for(Object obj : values)
			vals.add(mapr.toMongoObject(obj));
		
		return vals;
	}
}
