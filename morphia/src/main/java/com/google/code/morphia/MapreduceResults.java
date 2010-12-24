package com.google.code.morphia;

import com.google.code.morphia.annotations.AlsoLoad;
import com.google.code.morphia.annotations.Entity;
import com.google.code.morphia.annotations.PreLoad;
import com.google.code.morphia.annotations.Property;
import com.google.code.morphia.query.Query;
import com.google.code.morphia.query.QueryImpl;
import com.mongodb.DBObject;

@Entity @SuppressWarnings({"unused", "unchecked", "rawtypes"})
public class MapreduceResults<T> {
	DBObject rawResults = null;
	private Stats counts = new Stats();
	
	@Property("result")
	private String outColl;
	private long timeMillis;
	private boolean ok;
	private String err;
	private MapreduceType type;
	private QueryImpl baseQuery;
	
	public Stats getCounts() 		{ return counts; }
	public long getElapsedMillis() 	{ return timeMillis; }
	public boolean isOk() 			{ return (ok); }
	public String getError() 		{ return isOk() ? "" : err; }
	public MapreduceType getType()	{ return type;}
	public Query<T> createQuery()	{ return baseQuery.clone(); };
	
	String getOutputCollectionName(){ return outColl; }
	void setBits(MapreduceType t, QueryImpl baseQ) { 
		type = t;
		baseQuery = baseQ;
	}
	
	@PreLoad 
	void preLoad(DBObject raw) {
		rawResults = raw;
	}
	
	public static class Stats {
		private int input, emit, output;
		public int getInputCount() 	{ return input; }
		public int getEmitCount() 	{ return emit; }
		public int getOutputCount() { return output; }
	}
}
