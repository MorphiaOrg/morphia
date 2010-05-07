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
public class UpdateOpsImpl implements UpdateOperations {
	Map<String, Map<String, Object>> ops = new HashMap<String, Map<String, Object>>();
	
//	List<DBObject> ops = new ArrayList<DBObject>();
	Mapper mapr;
	
	public UpdateOpsImpl(Mapper mapper) {
		this.mapr = mapper;
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
	@Override
	public UpdateOperations add(String fieldExpr, Object value) {
		Object dbObj = mapr.toMongoObject(value);
		add("$set", fieldExpr, dbObj);
		return this;
	}

	@Override
	public UpdateOperations add(String fieldExpr, Object value, boolean addDups) {
		Object dbObj = mapr.toMongoObject(value);
		add((addDups) ? "$push" : "$addToSet", fieldExpr, dbObj);
		return this;
	}
	
	@Override
	public UpdateOperations add(String fieldExpr, List<?> values, boolean addDups) {
		List<Object> vals = toDBObjList(values);
		add((addDups) ? "$pushAll" : "$addToSet", fieldExpr, vals);
		return this;
	}

	@Override
	public UpdateOperations dec(String fieldExpr) {
		return inc(fieldExpr, -1);
	}

	@Override
	public UpdateOperations inc(String fieldExpr) {
		return inc(fieldExpr, 1);
	}

	@Override
	public UpdateOperations inc(String fieldExpr, Number value) {
		add("$inc", fieldExpr, value);
		return this;
	}


	protected UpdateOperations remove(String fieldExpr, boolean firstNotLast) {
		add("$pop", fieldExpr, (firstNotLast) ? -1 : 1 );
		return this;
	}

	@Override
	public UpdateOperations removeAll(String fieldExpr, Object value) {
		Object dbObj = mapr.toMongoObject(value);
		add("$pull", fieldExpr, dbObj);
		return this;
	}

	@Override
	public UpdateOperations removeAll(String fieldExpr, List<?> values) {
		List<Object> vals = toDBObjList(values);
		add("$pullAll", fieldExpr, vals);
		return this;
	}

	@Override
	public UpdateOperations removeFirst(String fieldExpr) {
		return remove(fieldExpr, true);
	}

	@Override
	public UpdateOperations removeLast(String fieldExpr) {
		return remove(fieldExpr, false);
	}

	@Override
	public UpdateOperations set(String fieldExpr, Object value) {
		Object dbObj = mapr.toMongoObject(value);
		add("$set", fieldExpr, dbObj);
		return this;
	}

	@Override
	public UpdateOperations unset(String fieldExpr) {
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
