package com.google.code.morphia.query;

import com.mongodb.CommandResult;
import com.mongodb.WriteResult;

public class UpdateResults<T> {
	//TODO: remove most of these vars and get from WriteResult/GLE
	private boolean hadError;
	private String error;
	private boolean updatedExisting;
	private int updateCount;
	private int insertCount;
	private Object newId;
	private WriteResult wr;
	
	public UpdateResults(WriteResult wr) {
		this.wr = wr;
		CommandResult opRes = wr.getLastError();
		updatedExisting = (opRes.containsField("updatedExisting") && (Boolean)opRes.get("updatedExisting"));
		error = (String)opRes.getErrorMessage();
		hadError = error != null && !error.isEmpty();
		if (opRes.containsField("n")) {
			if(updatedExisting) 
				updateCount = ((Number)opRes.get("n")).intValue();
			else
				insertCount = ((Number)opRes.get("n")).intValue();
		}
		
		if (insertCount > 0 && opRes.containsField("upserted"))
			newId = opRes.get("upserted");
	}
	
	public String getError() {return error;}
	public boolean getHadError() {return hadError;}
	public boolean getUpdatedExisting() {return updatedExisting;}
	public int getUpdatedCount() {return updateCount;}
	public int getInsertedCount() {return insertCount;}
	public Object getNewId() {return newId;}
	public WriteResult getWriteResult() {return wr;}
} 
