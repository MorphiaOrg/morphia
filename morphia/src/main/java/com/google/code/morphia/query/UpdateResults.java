package com.google.code.morphia.query;

import com.mongodb.DBObject;

public class UpdateResults<T> {
	private boolean hadError;
	private String error;
	private boolean updatedExisting;
	private int updateCount;
	private int insertCount;
	
	public UpdateResults(DBObject dbObj) {
		updatedExisting = (dbObj.containsField("updatedExisting") && (Boolean)dbObj.get("updatedExisting"));
		error = (String)dbObj.get("err");
		hadError = error != null && !error.isEmpty();
		if (dbObj.containsField("n")) {
			if(updatedExisting) 
				updateCount = ((Number)dbObj.get("n")).intValue();
			else
				insertCount = ((Number)dbObj.get("n")).intValue();
		}
	}
	
	public String getError() {return error;}
	public boolean getHadError() {return hadError;}
	public boolean getUpdatedExisting() {return updatedExisting;}
	public int getUpdatedCount() {return updateCount;}
	public int getInsertedCount() {return insertCount;}
	
} 
